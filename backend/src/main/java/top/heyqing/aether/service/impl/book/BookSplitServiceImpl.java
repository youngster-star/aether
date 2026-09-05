package top.heyqing.aether.service.impl.book;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.SplitConfirmRequest;
import top.heyqing.aether.model.entity.Book;
import top.heyqing.aether.model.entity.BookAiTask;
import top.heyqing.aether.model.entity.BookChapter;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.SplitTaskVO;
import top.heyqing.aether.repository.BookAiTaskRepository;
import top.heyqing.aether.repository.BookChapterRepository;
import top.heyqing.aether.repository.BookRepository;
import top.heyqing.aether.service.book.BookAgentClient;
import top.heyqing.aether.service.book.BookSplitService;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.util.SplitHeuristics;

/**
 * 书籍分章服务实现（BackEnd-Plan §7.2 流程编排）
 *
 * <p>分块策略：每块 ≤ 20000 字符 + 200 字符重叠窗口（防标题/段落跨块切断）；
 * 跨块合并：相邻块首末章标题相同则拼接段落，并剔除重叠窗口产生的重复段；
 * 降级策略：任一块 agent 失败 → 该块走 Java 启发式（SplitHeuristics），
 * source 标记 heuristic。异步执行经 bookSplitExecutor 线程池。</p>
 */
@Service
public class BookSplitServiceImpl implements BookSplitService {

    private static final Logger log = LoggerFactory.getLogger(BookSplitServiceImpl.class);

    /** 单块最大字符数（§7.2） */
    private static final int CHUNK_SIZE = 20000;

    /** 相邻块重叠窗口（防边界切断标题/段落） */
    private static final int CHUNK_OVERLAP = 200;

    /** fail_reason 最大长度（表字段 500） */
    private static final int FAIL_REASON_MAX = 500;

    private final BookRepository bookRepository;
    private final BookChapterRepository bookChapterRepository;
    private final BookAiTaskRepository bookAiTaskRepository;
    private final BookAgentClient bookAgentClient;
    private final FileUploadService fileUploadService;
    private final JsonMapper jsonMapper;

    /** 分章专用线程池（避免阻塞 HTTP 线程） */
    private final java.util.concurrent.Executor bookSplitExecutor;

    public BookSplitServiceImpl(BookRepository bookRepository,
                                BookChapterRepository bookChapterRepository,
                                BookAiTaskRepository bookAiTaskRepository,
                                BookAgentClient bookAgentClient,
                                FileUploadService fileUploadService,
                                JsonMapper jsonMapper,
                                @Qualifier("bookSplitExecutor") java.util.concurrent.Executor bookSplitExecutor) {
        this.bookRepository = bookRepository;
        this.bookChapterRepository = bookChapterRepository;
        this.bookAiTaskRepository = bookAiTaskRepository;
        this.bookAgentClient = bookAgentClient;
        this.fileUploadService = fileUploadService;
        this.jsonMapper = jsonMapper;
        this.bookSplitExecutor = bookSplitExecutor;
    }

    @Override
    public Long trigger(Long bookId) {
        Book book = findBook(bookId);
        if (book.getSourceFileId() == null) {
            throw new BusinessException(ErrorCode.BOOK_SOURCE_MISSING);
        }
        BookAiTask latest = bookAiTaskRepository.findFirstByBookIdOrderByIdDesc(bookId).orElse(null);
        if (latest != null && (latest.getStatus() == BookAiTask.STATUS_PARSING
                || latest.getStatus() == BookAiTask.STATUS_CONFIRMED)) {
            // 解析中拒绝重复触发；已完成不允许再分（重新分章需重新上传源文件后走编辑）
            throw new BusinessException(ErrorCode.SPLIT_TASK_STATE_INVALID);
        }
        BookAiTask task = new BookAiTask();
        task.setBookId(bookId);
        task.setStatus(BookAiTask.STATUS_PARSING);
        task = bookAiTaskRepository.save(task);
        Long taskId = task.getId();
        // 异步执行（管理端轮询 split-task 拿进度）
        bookSplitExecutor.execute(() -> executeSplit(taskId, bookId));
        return taskId;
    }

    @Override
    public SplitTaskVO task(Long bookId) {
        BookAiTask task = bookAiTaskRepository.findFirstByBookIdOrderByIdDesc(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPLIT_TASK_NOT_FOUND));
        String source = null;
        SplitTaskVO.SplitChaptersVO aiResult = null;
        if (task.getAiResult() != null) {
            try {
                JsonNode root = jsonMapper.readTree(task.getAiResult());
                source = root.path("source").asString(null);
                aiResult = parseAiResult(root.path("chapters"), source);
            } catch (Exception e) {
                log.warn("ai_result 解析失败: taskId={}", task.getId(), e);
            }
        }
        return new SplitTaskVO(task.getId(), task.getBookId(), task.getStatus(), source,
                aiResult, task.getFailReason(), task.getConfirmTime());
    }

    @Override
    @Transactional
    public void confirm(Long bookId, SplitConfirmRequest request) {
        Book book = findBook(bookId);
        BookAiTask task = bookAiTaskRepository.findFirstByBookIdOrderByIdDesc(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPLIT_TASK_NOT_FOUND));
        if (task.getStatus() != BookAiTask.STATUS_PENDING_CONFIRM) {
            throw new BusinessException(ErrorCode.SPLIT_TASK_STATE_INVALID);
        }
        // 清空旧章节后重建（重新分章确认场景）
        bookChapterRepository.deleteByBookId(bookId);
        int orderNo = 1;
        int totalChapters = 0;
        Long lastChapterId = null;
        for (SplitConfirmRequest.ChapterConfirm chapter : request.chapters()) {
            // 节必须挂章：无前置章的节归为章（容错）
            int level = chapter.level();
            Long parentId = null;
            if (level == 2 && lastChapterId == null) {
                level = 1;
            } else if (level == 2) {
                parentId = lastChapterId;
            }
            int wordCount = chapter.paragraphs().stream()
                    .mapToInt(String::length).sum();
            BookChapter entity = new BookChapter();
            entity.setBookId(bookId);
            entity.setTitle(chapter.title().strip());
            entity.setLevel(level);
            entity.setParentId(parentId);
            entity.setOrderNo(orderNo++);
            entity.setContent(renderContentHtml(chapter.paragraphs()));
            entity.setWordCount(wordCount);
            entity = bookChapterRepository.save(entity);
            if (level == 1) {
                totalChapters++;
                lastChapterId = entity.getId();
            }
        }
        book.setTotalChapters(totalChapters);
        bookRepository.save(book);
        task.setStatus(BookAiTask.STATUS_CONFIRMED);
        task.setConfirmTime(LocalDateTime.now());
        bookAiTaskRepository.save(task);
        log.info("分章确认落库: bookId={}, chapters={}, totalChapters={}",
                bookId, request.chapters().size(), totalChapters);
    }

    /**
     * 异步执行分章（任何异常都收敛为 status=4 + fail_reason）
     */
    void executeSplit(Long taskId, Long bookId) {
        BookAiTask task = bookAiTaskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != BookAiTask.STATUS_PARSING) {
            return;
        }
        try {
            Book book = findBook(bookId);
            String fullText = readSourceText(book.getSourceFileId());
            SplitOutcome outcome = splitAndMerge(fullText);
            ObjectNode root = jsonMapper.createObjectNode();
            ArrayNode chaptersNode = root.putArray("chapters");
            for (SplitHeuristics.ChapterDraft chapter : outcome.chapters()) {
                ObjectNode chapterNode = chaptersNode.addObject();
                chapterNode.put("title", chapter.title());
                chapterNode.put("level", chapter.level());
                ArrayNode paragraphs = chapterNode.putArray("paragraphs");
                chapter.paragraphs().forEach(paragraphs::add);
            }
            root.put("source", outcome.usedAgent() ? "agent" : "heuristic");
            task.setAiResult(jsonMapper.writeValueAsString(root));
            task.setStatus(BookAiTask.STATUS_PENDING_CONFIRM);
            bookAiTaskRepository.save(task);
            log.info("分章完成: bookId={}, taskId={}, chapters={}, source={}",
                    bookId, taskId, outcome.chapters().size(), outcome.usedAgent() ? "agent" : "heuristic");
        } catch (Exception e) {
            log.error("分章失败: bookId={}, taskId={}", bookId, taskId, e);
            BookAiTask failed = bookAiTaskRepository.findById(taskId).orElse(null);
            if (failed != null) {
                failed.setStatus(BookAiTask.STATUS_FAILED);
                failed.setFailReason(truncate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                bookAiTaskRepository.save(failed);
            }
        }
    }

    /** 本轮分章结果（章节 + 是否用过 agent） */
    private record SplitOutcome(List<SplitHeuristics.ChapterDraft> chapters, boolean usedAgent) {
    }

    /**
     * 分块 → 逐块调用（agent 优先，失败降级启发式）→ 跨块合并
     */
    private SplitOutcome splitAndMerge(String fullText) {
        boolean agentUsed = true;
        List<String> blocks = chunk(fullText);
        List<List<SplitHeuristics.ChapterDraft>> blockChapters = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            Optional<List<SplitHeuristics.ChapterDraft>> result = bookAgentClient.split(blocks.get(i), i, blocks.size());
            if (result.isEmpty()) {
                agentUsed = false;
                result = Optional.of(SplitHeuristics.split(blocks.get(i)));
            }
            blockChapters.add(result.get());
        }
        return new SplitOutcome(mergeChunks(blockChapters), agentUsed);
    }

    /**
     * 字符分块（每块 ≤ CHUNK_SIZE，相邻块重叠 CHUNK_OVERLAP 字符）
     */
    private List<String> chunk(String text) {
        List<String> blocks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            blocks.add(text.substring(start, end));
            if (end >= text.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return blocks;
    }

    /**
     * 跨块合并：相邻块首末章标题相同 → 段落拼接（剔除重叠窗口产生的重复段）
     */
    private List<SplitHeuristics.ChapterDraft> mergeChunks(
            List<List<SplitHeuristics.ChapterDraft>> blockChapters) {
        List<SplitHeuristics.ChapterDraft> merged = new ArrayList<>();
        for (List<SplitHeuristics.ChapterDraft> chapters : blockChapters) {
            if (chapters.isEmpty()) {
                continue;
            }
            if (!merged.isEmpty()) {
                SplitHeuristics.ChapterDraft last = merged.get(merged.size() - 1);
                SplitHeuristics.ChapterDraft first = chapters.get(0);
                if (last.title().equals(first.title())) {
                    // 拼接段落：剔除与前章段落重复的段（重叠窗口重复）
                    Set<String> seen = new HashSet<>(last.paragraphs());
                    List<String> combined = new ArrayList<>(last.paragraphs());
                    for (String paragraph : first.paragraphs()) {
                        if (seen.add(paragraph)) {
                            combined.add(paragraph);
                        }
                    }
                    merged.set(merged.size() - 1,
                            new SplitHeuristics.ChapterDraft(last.title(), last.level(), combined));
                    merged.addAll(chapters.subList(1, chapters.size()));
                    continue;
                }
            }
            merged.addAll(chapters);
        }
        return merged;
    }

    /**
     * 读取 txt 源文件全文（UTF-8，strip BOM）
     */
    private String readSourceText(Long fileId) {
        StorageFile file = fileUploadService.findPublishedFile(fileId);
        try (InputStream in = fileUploadService.openFile(file)) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BOOK_SOURCE_MISSING);
        }
    }

    /**
     * 段落 → 排版 HTML（段首空两格 p.indent；插图标记行 → figure-mark 占位）
     */
    private String renderContentHtml(List<String> paragraphs) {
        StringBuilder html = new StringBuilder();
        for (String paragraph : paragraphs) {
            String value = paragraph.strip();
            if (value.isEmpty()) {
                continue;
            }
            if (value.matches("^\\[(插图|img|图)[:：].*]$")) {
                // 插图标记位（后续人工配图，先保留占位）
                html.append("<div class=\"figure-mark\">").append(HtmlUtils.htmlEscape(value)).append("</div>");
            } else {
                html.append("<p class=\"indent\">").append(HtmlUtils.htmlEscape(value)).append("</p>");
            }
        }
        return html.toString();
    }

    private SplitTaskVO.SplitChaptersVO parseAiResult(JsonNode chapters, String source) {
        List<SplitTaskVO.ChapterDraftVO> drafts = new ArrayList<>();
        if (chapters.isArray()) {
            for (JsonNode chapter : chapters) {
                List<String> paragraphs = new ArrayList<>();
                JsonNode paragraphsNode = chapter.path("paragraphs");
                if (paragraphsNode.isArray()) {
                    paragraphsNode.forEach(paragraph -> paragraphs.add(paragraph.asString()));
                }
                drafts.add(new SplitTaskVO.ChapterDraftVO(
                        chapter.path("title").asString(""),
                        chapter.path("level").asInt(),
                        paragraphs));
            }
        }
        return new SplitTaskVO.SplitChaptersVO(drafts, source);
    }

    private Book findBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
    }

    private String truncate(String value) {
        return value.length() <= FAIL_REASON_MAX ? value : value.substring(0, FAIL_REASON_MAX);
    }
}
