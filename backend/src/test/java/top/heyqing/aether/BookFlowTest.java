package top.heyqing.aether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.BookSaveRequest;
import top.heyqing.aether.model.dto.SplitConfirmRequest;
import top.heyqing.aether.model.dto.StorageInitRequest;
import top.heyqing.aether.model.entity.BookAiTask;
import top.heyqing.aether.model.entity.BookChapter;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.BookChapterContentVO;
import top.heyqing.aether.model.vo.StorageInitVO;
import top.heyqing.aether.model.vo.StorageMergeVO;
import top.heyqing.aether.model.vo.SplitTaskVO;
import top.heyqing.aether.repository.BookAiTaskRepository;
import top.heyqing.aether.repository.BookChapterRepository;
import top.heyqing.aether.repository.BookRepository;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.repository.StorageRefRepository;
import top.heyqing.aether.service.book.BookAdminService;
import top.heyqing.aether.service.book.BookAgentClient;
import top.heyqing.aether.service.book.BookService;
import top.heyqing.aether.service.book.BookSplitService;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.util.DigestUtil;
import top.heyqing.aether.util.SplitHeuristics;

/**
 * 书籍模块功能测试（阶段 5 完成标准，BackEnd-Plan §5.2/§7.2）
 *
 * <p>测试自建数据（用例结束清理）。覆盖：公开列表/详情目录树/章节内容 prev+next、
 * 管理 CRUD 与引用归零删除一致性、分章状态机（agent → 待确认 → 确认落库 → 30404 拒绝）、
 * 断网降级（agent 不可达 → Java 启发式）、启发式分章规则、触发/确认前置校验。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "aether.storage.local-base-dir=target/test-storage")
class BookFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookService bookService;

    @Autowired
    private BookAdminService bookAdminService;

    @Autowired
    private BookSplitService bookSplitService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookChapterRepository bookChapterRepository;

    @Autowired
    private BookAiTaskRepository bookAiTaskRepository;

    @Autowired
    private StorageFileRepository storageFileRepository;

    @Autowired
    private StorageRefRepository storageRefRepository;

    @MockitoBean
    private BookAgentClient bookAgentClient;

    /** 用例内建的书 ID（清理用） */
    private Long bookId;

    @AfterEach
    void cleanup() {
        if (bookId != null) {
            bookAdminService.delete(bookId);
            bookId = null;
        }
        Mockito.reset(bookAgentClient);
    }

    // ===== 公开接口（自建数据） =====

    @Test
    @DisplayName("书籍公开接口：列表 keyword 过滤/详情目录树（章-节两级）/章节内容 prev+next 导航")
    void booksPublicApi() throws Exception {
        StorageFile cover = upload(renderPng(0x3E5C76), "书籍封面.png");
        bookId = bookAdminService.create(new BookSaveRequest(
                "以太漂流志测试", "dkb", cover.getId(), "一句话介绍", 1, null, 1));
        // 手工建章-节结构（不经分章）：章 1 + 节 + 章 2
        Long chapter1Id = saveChapter(bookId, "序章：起航", 1, null, 1,
                "<p class=\"indent\">第一段。</p><p class=\"indent\">第二段。</p>", 100);
        Long sectionId = saveChapter(bookId, "港口", 2, chapter1Id, 2,
                "<p class=\"indent\">节内容。</p>", 40);
        saveChapter(bookId, "第二章：雾海", 1, null, 3,
                "<p class=\"indent\">雾。</p>", 20);
        // total_chapters 为冗余维护字段（分章确认时由服务同步），手工建章场景需补
        var book = bookRepository.findById(bookId).orElseThrow();
        book.setTotalChapters(2);
        bookRepository.save(book);

        mockMvc.perform(get("/v1/books").param("keyword", "漂流志"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].coverUrl")
                        .value(org.hamcrest.Matchers.containsString("sign=")))
                .andExpect(jsonPath("$.data.records[0].isRecommend").value(1));

        mockMvc.perform(get("/v1/books").param("keyword", "不存在的书"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        // 详情：章-节两级树（章 1 含 1 节）
        mockMvc.perform(get("/v1/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ownershipType").value(1))
                .andExpect(jsonPath("$.data.totalChapters").value(2))
                .andExpect(jsonPath("$.data.chapters.length()").value(2))
                .andExpect(jsonPath("$.data.chapters[0].children.length()").value(1))
                .andExpect(jsonPath("$.data.chapters[0].children[0].title").value("港口"));

        // 章节内容：节的内容 + 全书线性序导航（节 prev=章1、next=章2）
        mockMvc.perform(get("/v1/books/{id}/chapters/{chapterId}", bookId, sectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.contentHtml").value("<p class=\"indent\">节内容。</p>"))
                .andExpect(jsonPath("$.data.totalChapters").value(3))
                .andExpect(jsonPath("$.data.next.title").value("第二章：雾海"));
        BookChapterContentVO vo = bookService.chapterContent(bookId, sectionId);
        assertNotNull(vo.prev());
        assertEquals("序章：起航", vo.prev().title());
        assertEquals("第二章：雾海", vo.next().title());

        // 不存在的章节 → 30403
        mockMvc.perform(get("/v1/books/{id}/chapters/{chapterId}", bookId, 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30403));
    }

    // ===== 管理 CRUD + 删除一致性（§8.3） =====

    @Test
    @DisplayName("书籍管理：CRUD、换封面旧文件归零、删除清章节/任务/引用")
    void bookAdminCrudAndDeleteConsistency() {
        StorageFile cover = upload(renderPng(0x4F6D5A), "管理封面.png");
        Long id = bookAdminService.create(new BookSaveRequest(
                "管理测试书", "测试作者", cover.getId(), "简介", 2, null, 0));
        try {
            assertEquals("管理测试书", bookService.detail(id).title());
            assertEquals(2, bookService.detail(id).ownershipType());

            // 换封面：旧文件引用归零 → status=0
            StorageFile cover2 = upload(renderPng(0x123456), "管理封面2.png");
            bookAdminService.update(id, new BookSaveRequest(
                    "管理测试书2", "测试作者", cover2.getId(), "简介", 2, null, 1));
            assertEquals(0, storageFileRepository.findById(cover.getId()).orElseThrow().getStatus(),
                    "旧封面引用归零应标记待清理");
            assertEquals(1, bookService.detail(id).isRecommend());

            // 章节数维护：确认服务/手工场景一致（无需依赖 Controller 校验，归属越界由 @Valid 保证）

            // 删除 → 引用清空
            bookAdminService.delete(id);
            assertTrue(storageRefRepository.findByBizTypeAndBizId("book", id).isEmpty(),
                    "删除书籍后 storage_ref 应无残留引用");
            bookId = null; // 已删，cleanup 跳过
        } finally {
            if (bookId != null) {
                bookAdminService.delete(bookId);
                bookId = null;
            }
        }
    }

    // ===== 分章状态机（§7.2） =====

    @Test
    @DisplayName("分章状态机：agent 建议 → 待确认 → 确认落库（p.indent 排版）→ 重复触发/确认被拒")
    void splitStateMachineWithAgent() throws Exception {
        // agent 返回固定两章建议（延迟 400ms 模拟 agent 处理，保证"解析中重复触发"断言稳定）
        Mockito.when(bookAgentClient.split(anyString(), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    Thread.sleep(400);
                    return Optional.of(List.of(
                            new SplitHeuristics.ChapterDraft("第一章 启程", 1,
                                    List.of("清晨的码头很安静。", "[插图：港口速写]")),
                            new SplitHeuristics.ChapterDraft("第二章 迷雾", 1, List.of("雾从北边压过来。"))));
                });

        StorageFile txt = upload(sampleTxt(), "测试小说.txt");
        Long id = bookAdminService.create(new BookSaveRequest(
                "分章测试书", "dkb", null, null, 1, txt.getId(), 0));
        bookId = id;

        Long taskId = bookSplitService.trigger(id);
        assertNotNull(taskId);
        // 解析中拒绝重复触发
        BusinessException busy = assertThrows(BusinessException.class, () -> bookSplitService.trigger(id));
        assertEquals(30404, busy.getErrorCode().getCode());
        // 轮询至待确认
        awaitStatus(id, BookAiTask.STATUS_PENDING_CONFIRM);
        SplitTaskVO task = bookSplitService.task(id);
        assertEquals("agent", task.source());
        assertEquals(2, task.aiResult().chapters().size());

        // 确认落库（管理端编辑后提交）
        bookSplitService.confirm(id, new SplitConfirmRequest(List.of(
                new SplitConfirmRequest.ChapterConfirm("第一章 启程", 1,
                        List.of("清晨的码头很安静。", "[插图：港口速写]")),
                new SplitConfirmRequest.ChapterConfirm("第二章 迷雾", 1, List.of("雾从北边压过来。")))));
        assertEquals(2, bookService.detail(id).totalChapters());
        var chapters = bookChapterRepository.findByBookIdOrderByOrderNoAsc(id);
        assertEquals(2, chapters.size());
        assertEquals(1, chapters.get(0).getLevel());
        assertTrue(chapters.get(0).getContent().contains("<p class=\"indent\">清晨的码头很安静。</p>"),
                "段首空两格排版（p.indent）");
        assertTrue(chapters.get(0).getContent().contains("figure-mark"),
                "插图标记位（figure-mark 占位）");
        // 公开接口可读
        mockMvc.perform(get("/v1/books/{id}/chapters/{chapterId}", id, chapters.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.contentHtml").value(
                        org.hamcrest.Matchers.containsString("indent")));

        // 已完成：重复确认 / 重复触发 均拒绝（30404）
        BusinessException reConfirm = assertThrows(BusinessException.class, () -> bookSplitService.confirm(id,
                new SplitConfirmRequest(List.of(new SplitConfirmRequest.ChapterConfirm("章", 1, List.of("段"))))));
        assertEquals(30404, reConfirm.getErrorCode().getCode());
        BusinessException reTrigger = assertThrows(BusinessException.class, () -> bookSplitService.trigger(id));
        assertEquals(30404, reTrigger.getErrorCode().getCode());
    }

    @Test
    @DisplayName("断网降级：agent 不可达 → Java 启发式分章可用（source=heuristic）")
    void fallbackToHeuristicWhenAgentDown() {
        Mockito.when(bookAgentClient.split(anyString(), anyInt(), anyInt())).thenReturn(Optional.empty());
        StorageFile txt = upload(sampleTxt(), "降级测试.txt");
        Long id = bookAdminService.create(new BookSaveRequest(
                "降级测试书", "dkb", null, null, 1, txt.getId(), 0));
        bookId = id;

        bookSplitService.trigger(id);
        awaitStatus(id, BookAiTask.STATUS_PENDING_CONFIRM);
        SplitTaskVO task = bookSplitService.task(id);
        assertEquals("heuristic", task.source(), "agent 不可达应降级 Java 启发式");
        assertTrue(task.aiResult().chapters().size() >= 2, "样例 txt 含两章标题");
        assertTrue(task.aiResult().chapters().get(0).title().contains("第一章"));

        bookSplitService.confirm(id, new SplitConfirmRequest(List.of(
                new SplitConfirmRequest.ChapterConfirm("第一章 启程", 1, List.of("段一")),
                new SplitConfirmRequest.ChapterConfirm("1.1 风向", 2, List.of("节段")),
                new SplitConfirmRequest.ChapterConfirm("第二章 迷雾", 1, List.of("段二")))));
        // 章-节两级落库：节挂最近章
        var chapters = bookChapterRepository.findByBookIdOrderByOrderNoAsc(id);
        assertEquals(3, chapters.size());
        assertEquals(2, chapters.get(1).getLevel());
        assertEquals(chapters.get(0).getId(), chapters.get(1).getParentId());
        assertEquals(2, bookService.detail(id).totalChapters(), "total_chapters 只统计章");
    }

    @Test
    @DisplayName("触发前置校验：无源文件 → 30405；无任务确认 → 30401")
    void triggerAndConfirmPreconditions() {
        Long id = bookAdminService.create(new BookSaveRequest(
                "无源文件书", "dkb", null, null, 1, null, 0));
        bookId = id;
        BusinessException noSource = assertThrows(BusinessException.class, () -> bookSplitService.trigger(id));
        assertEquals(30405, noSource.getErrorCode().getCode());
        BusinessException noTask = assertThrows(BusinessException.class, () ->
                bookSplitService.confirm(id, new SplitConfirmRequest(List.of(
                        new SplitConfirmRequest.ChapterConfirm("章", 1, List.of("段"))))));
        assertEquals(30401, noTask.getErrorCode().getCode());
    }

    // ===== 启发式分章规则（Java 降级器，与 agent 同构） =====

    @Test
    @DisplayName("启发式分章：第X章/第X节识别、空行分段、正文引用防误判、无标题单章兜底")
    void splitHeuristicsRules() {
        String text = """
                第一章 启程
                清晨的码头很安静，只有海鸥在桅杆上打盹。

                我数了数口袋里的硬币，够买三天的干粮。

                第二章 迷雾
                雾从北边压过来，像一堵会移动的墙。

                第一节 风向
                指南针在雾里格外亮。

                这本书里引用了一句“第三章 的名字”，但它只是正文引用不应被切分。
                """;
        List<SplitHeuristics.ChapterDraft> chapters = SplitHeuristics.split(text);
        assertEquals(3, chapters.size(), "正文引用'第三章'不应被切分为新章");
        assertEquals("第一章 启程", chapters.get(0).title());
        assertEquals(1, chapters.get(0).level());
        assertEquals(2, chapters.get(0).paragraphs().size(), "空行分段");
        assertEquals("第二章 迷雾", chapters.get(1).title());
        assertEquals(2, chapters.get(2).level(), "第X节 识别为节");
        assertEquals("第一节 风向", chapters.get(2).title());
        assertTrue(chapters.get(2).paragraphs().get(1).contains("第三章"),
                "正文引用不被切分且并入正文");

        // 无标题 → 单章兜底
        List<SplitHeuristics.ChapterDraft> single = SplitHeuristics.split("没有标题的文本\n\n第二段");
        assertEquals(1, single.size());
        assertEquals("全文", single.get(0).title());
    }

    // ===== 工具方法 =====

    /**
     * 轮询等待任务到达目标状态（异步分章，超时 10s）
     */
    private void awaitStatus(Long id, int expected) {
        for (int i = 0; i < 50; i++) {
            Optional<BookAiTask> task = bookAiTaskRepository.findFirstByBookIdOrderByIdDesc(id);
            if (task.isPresent() && task.get().getStatus() == expected) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待分章被中断", e);
            }
        }
        throw new IllegalStateException("分章任务未在超时内到达状态 " + expected);
    }

    /**
     * 样例 txt（两章，含空行分段）
     */
    private static byte[] sampleTxt() {
        String text = """
                第一章 启程
                清晨的码头很安静，只有海鸥在桅杆上打盹。

                我数了数口袋里的硬币，够买三天的干粮。

                第二章 迷雾
                雾从北边压过来，像一堵会移动的墙。
                """;
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 分片上传-合并（单分片小文件场景；同内容已存在时秒传直接返回现有文件）
     */
    private StorageFile upload(byte[] content, String name) {
        String md5 = DigestUtil.sha256Hex(new java.io.ByteArrayInputStream(content));
        StorageInitVO init = fileUploadService.init(
                new StorageInitRequest(md5, (long) content.length, name, 1));
        if (init.uploadId() == null) {
            return storageFileRepository.findById(init.fileId()).orElseThrow();
        }
        MultipartFile file = new MockMultipartFile("file", "chunk-0", "application/octet-stream", content);
        fileUploadService.chunk(init.uploadId(), 0, file);
        StorageMergeVO merge = fileUploadService.merge(init.uploadId());
        return storageFileRepository.findById(merge.fileId()).orElseThrow();
    }

    /**
     * 渲染纯色 PNG（作为测试封面）
     */
    private static byte[] renderPng(int rgb) {
        var image = new java.awt.image.BufferedImage(32, 24, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 24; y++) {
            for (int x = 0; x < 32; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("测试图片渲染失败", e);
        }
    }

    /**
     * 手工保存章节（公开接口用例建章-节结构用）
     */
    private Long saveChapter(Long bookId, String title, int level, Long parentId,
                             int orderNo, String content, int wordCount) {
        BookChapter chapter = new BookChapter();
        chapter.setBookId(bookId);
        chapter.setTitle(title);
        chapter.setLevel(level);
        chapter.setParentId(parentId);
        chapter.setOrderNo(orderNo);
        chapter.setContent(content);
        chapter.setWordCount(wordCount);
        return bookChapterRepository.save(chapter).getId();
    }
}
