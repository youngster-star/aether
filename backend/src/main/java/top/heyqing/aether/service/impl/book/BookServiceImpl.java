package top.heyqing.aether.service.impl.book;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.entity.BizCategoryRel;
import top.heyqing.aether.model.entity.BizTagRel;
import top.heyqing.aether.model.entity.Book;
import top.heyqing.aether.model.entity.BookChapter;
import top.heyqing.aether.model.entity.Category;
import top.heyqing.aether.model.entity.Tag;
import top.heyqing.aether.model.vo.BookChapterContentVO;
import top.heyqing.aether.model.vo.BookDetailVO;
import top.heyqing.aether.model.vo.BookListVO;
import top.heyqing.aether.model.vo.ChapterNodeVO;
import top.heyqing.aether.repository.BizCategoryRelRepository;
import top.heyqing.aether.repository.BizTagRelRepository;
import top.heyqing.aether.repository.BookChapterRepository;
import top.heyqing.aether.repository.BookRepository;
import top.heyqing.aether.repository.CategoryRepository;
import top.heyqing.aether.repository.TagRepository;
import top.heyqing.aether.service.book.BookService;
import top.heyqing.aether.service.storage.FileUploadService;

/**
 * 书籍公开服务实现（BackEnd-Plan §5.2）
 *
 * <p>列表标签批量关联查询（biz_tag_rel bizIdIn 一次取齐，防 N+1，阶段 4 审查结论）；
 * 分类过滤经 biz_category_rel（书籍绑定 book 分类域）。</p>
 */
@Service
public class BookServiceImpl implements BookService {

    private static final String BIZ_BOOK = "book";

    private final BookRepository bookRepository;
    private final BookChapterRepository bookChapterRepository;
    private final BizCategoryRelRepository bizCategoryRelRepository;
    private final BizTagRelRepository bizTagRelRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final FileUploadService fileUploadService;

    public BookServiceImpl(BookRepository bookRepository,
                           BookChapterRepository bookChapterRepository,
                           BizCategoryRelRepository bizCategoryRelRepository,
                           BizTagRelRepository bizTagRelRepository,
                           CategoryRepository categoryRepository,
                           TagRepository tagRepository,
                           FileUploadService fileUploadService) {
        this.bookRepository = bookRepository;
        this.bookChapterRepository = bookChapterRepository;
        this.bizCategoryRelRepository = bizCategoryRelRepository;
        this.bizTagRelRepository = bizTagRelRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BookListVO> page(Long categoryId, Long tagId, String keyword, int page, int size) {
        List<Long> bookIdsByCategory = categoryId == null ? null
                : bizCategoryRelRepository.findByBizTypeAndBizId(BIZ_BOOK, categoryId).stream()
                        .map(BizCategoryRel::getBizId).toList();
        List<Long> bookIdsByTag = tagId == null ? null
                : bizTagRelRepository.findByBizTypeAndBizId(BIZ_BOOK, tagId).stream()
                        .map(BizTagRel::getBizId).toList();
        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.strip() + "%";
                predicates.add(cb.or(cb.like(root.get("title"), like), cb.like(root.get("author"), like)));
            }
            if (bookIdsByCategory != null) {
                predicates.add(root.get("id").in(bookIdsByCategory.isEmpty() ? List.of(-1L) : bookIdsByCategory));
            }
            if (bookIdsByTag != null) {
                predicates.add(root.get("id").in(bookIdsByTag.isEmpty() ? List.of(-1L) : bookIdsByTag));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Book> result = bookRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        List<Long> bookIds = result.getContent().stream().map(Book::getId).toList();
        Map<Long, List<String>> tagsByBook = loadTags(bookIds);
        return PageResult.of(result.getContent().stream()
                .map(book -> toListVO(book, tagsByBook.getOrDefault(book.getId(), List.of())))
                .toList(), result.getTotalElements(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookListVO> recommend(int limit) {
        List<Book> books = bookRepository.findByIsRecommend(1,
                PageRequest.of(0, Math.min(Math.max(limit, 1), 20)));
        List<Long> bookIds = books.stream().map(Book::getId).toList();
        Map<Long, List<String>> tagsByBook = loadTags(bookIds);
        return books.stream()
                .map(book -> toListVO(book, tagsByBook.getOrDefault(book.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookDetailVO detail(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        List<BookChapter> chapters = bookChapterRepository.findByBookIdOrderByOrderNoAsc(id);
        // 分类标签（单本直接查）
        List<String> categories = categoryRepository
                .findAllById(bizCategoryRelRepository.findByBizTypeAndBizId(BIZ_BOOK, id).stream()
                        .map(BizCategoryRel::getCategoryId).toList())
                .stream().sorted(Comparator.comparing(Category::getId)).map(Category::getName).toList();
        List<String> tags = tagRepository
                .findAllById(bizTagRelRepository.findByBizTypeAndBizId(BIZ_BOOK, id).stream()
                        .map(BizTagRel::getTagId).toList())
                .stream().sorted(Comparator.comparing(Tag::getId)).map(Tag::getName).toList();
        return new BookDetailVO(book.getId(), book.getTitle(), book.getAuthor(),
                book.getCoverFileId() == null ? null : fileUploadService.signUrl(book.getCoverFileId()),
                book.getIntro(), book.getOwnershipType(), book.getTotalChapters(), book.getIsRecommend(),
                categories, tags, buildChapterTree(chapters));
    }

    @Override
    @Transactional(readOnly = true)
    public BookChapterContentVO chapterContent(Long bookId, Long chapterId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        List<BookChapter> chapters = bookChapterRepository.findByBookIdOrderByOrderNoAsc(bookId);
        BookChapter current = chapters.stream()
                .filter(chapter -> chapter.getId().equals(chapterId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_CHAPTER_NOT_FOUND));
        int index = chapters.indexOf(current);
        BookChapterContentVO.ChapterRefVO prev = index > 0
                ? toRef(chapters.get(index - 1)) : null;
        BookChapterContentVO.ChapterRefVO next = index < chapters.size() - 1
                ? toRef(chapters.get(index + 1)) : null;
        return new BookChapterContentVO(book.getId(), current.getId(), current.getTitle(),
                current.getLevel(), current.getOrderNo(), chapters.size(), current.getContent(), prev, next);
    }

    /**
     * 章-节两级目录树（节按 parent_id 挂章；孤儿节归为章展示）
     */
    private List<ChapterNodeVO> buildChapterTree(List<BookChapter> chapters) {
        Map<Long, List<BookChapter>> byParent = chapters.stream()
                .filter(chapter -> chapter.getParentId() != null)
                .collect(Collectors.groupingBy(BookChapter::getParentId));
        List<ChapterNodeVO> tree = new ArrayList<>();
        for (BookChapter chapter : chapters) {
            if (chapter.getParentId() != null) {
                continue;
            }
            tree.add(toNode(chapter, byParent.getOrDefault(chapter.getId(), List.of())));
        }
        return tree;
    }

    private ChapterNodeVO toNode(BookChapter chapter, List<BookChapter> children) {
        return new ChapterNodeVO(chapter.getId(), chapter.getTitle(), chapter.getLevel(),
                chapter.getOrderNo(), chapter.getWordCount(),
                children.stream().map(child -> toNode(child, List.of())).toList());
    }

    private BookChapterContentVO.ChapterRefVO toRef(BookChapter chapter) {
        return new BookChapterContentVO.ChapterRefVO(chapter.getId(), chapter.getTitle());
    }

    private BookListVO toListVO(Book book, List<String> tags) {
        return new BookListVO(book.getId(), book.getTitle(), book.getAuthor(),
                book.getCoverFileId() == null ? null : fileUploadService.signUrl(book.getCoverFileId()),
                book.getIntro(), book.getTotalChapters(), book.getIsRecommend(), tags);
    }

    /**
     * 批量载入标签名（biz_tag_rel + tag 两次批量查询，防 N+1）
     */
    private Map<Long, List<String>> loadTags(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        List<Tag> tags = tagRepository.findAllById(bizTagRelRepository
                .findByBizTypeAndBizIdIn(BIZ_BOOK, bookIds).stream()
                .map(BizTagRel::getTagId).distinct().toList());
        Map<Long, String> tagNames = tags.stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        return bizTagRelRepository.findByBizTypeAndBizIdIn(BIZ_BOOK, bookIds).stream()
                .collect(Collectors.groupingBy(BizTagRel::getBizId,
                        Collectors.mapping(rel -> tagNames.getOrDefault(rel.getTagId(), ""),
                                Collectors.toList())));
    }
}
