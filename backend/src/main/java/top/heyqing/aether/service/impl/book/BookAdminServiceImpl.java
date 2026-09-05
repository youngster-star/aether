package top.heyqing.aether.service.impl.book;

import java.util.ArrayList;
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
import top.heyqing.aether.model.dto.BookSaveRequest;
import top.heyqing.aether.model.entity.Book;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.BookAdminVO;
import top.heyqing.aether.repository.BookAiTaskRepository;
import top.heyqing.aether.repository.BookChapterRepository;
import top.heyqing.aether.repository.BookRepository;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.service.book.BookAdminService;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.service.storage.StorageRefService;
import top.heyqing.aether.storage.FileTypeValidator;

/**
 * 书籍管理端服务实现（BackEnd-Plan §5.2 /admin/books）
 *
 * <p>txt 源文件与封面引用登记在 bizType=book；删除书籍时章节/任务/引用一并解绑
 * （引用归零异步物理清理，§8.3）；封面/txt 防呆校验（不允许引用已删除文件）。</p>
 */
@Service
public class BookAdminServiceImpl implements BookAdminService {

    private final BookRepository bookRepository;
    private final BookChapterRepository bookChapterRepository;
    private final BookAiTaskRepository bookAiTaskRepository;
    private final StorageFileRepository storageFileRepository;
    private final FileUploadService fileUploadService;
    private final StorageRefService storageRefService;

    public BookAdminServiceImpl(BookRepository bookRepository,
                                BookChapterRepository bookChapterRepository,
                                BookAiTaskRepository bookAiTaskRepository,
                                StorageFileRepository storageFileRepository,
                                FileUploadService fileUploadService,
                                StorageRefService storageRefService) {
        this.bookRepository = bookRepository;
        this.bookChapterRepository = bookChapterRepository;
        this.bookAiTaskRepository = bookAiTaskRepository;
        this.storageFileRepository = storageFileRepository;
        this.fileUploadService = fileUploadService;
        this.storageRefService = storageRefService;
    }

    @Override
    public PageResult<BookAdminVO> list(int page, int size, String keyword) {
        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.strip() + "%";
                predicates.add(cb.or(cb.like(root.get("title"), pattern),
                        cb.like(root.get("author"), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Book> result = bookRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        List<BookAdminVO> records = result.getContent().stream().map(this::toAdminVO).toList();
        return PageResult.of(records, result.getTotalElements(), page, size);
    }

    @Override
    @Transactional
    public Long create(BookSaveRequest request) {
        Book book = new Book();
        applyRequest(book, request);
        bookRepository.save(book);
        // 封面与 txt 源文件引用登记（bizType=book）
        if (book.getCoverFileId() != null) {
            storageRefService.bind(book.getCoverFileId(), StorageRefService.BIZ_BOOK, book.getId());
        }
        if (book.getSourceFileId() != null) {
            storageRefService.bind(book.getSourceFileId(), StorageRefService.BIZ_BOOK, book.getId());
        }
        return book.getId();
    }

    @Override
    @Transactional
    public void update(Long id, BookSaveRequest request) {
        Book book = requireBook(id);
        Long oldCoverFileId = book.getCoverFileId();
        Long oldSourceFileId = book.getSourceFileId();
        applyRequest(book, request);
        bookRepository.save(book);
        // 文件更换：旧引用解绑（归零自动清理），新引用登记
        if (oldCoverFileId != null && !oldCoverFileId.equals(book.getCoverFileId())) {
            storageRefService.unbindFile(oldCoverFileId, StorageRefService.BIZ_BOOK, id);
        }
        if (book.getCoverFileId() != null && !book.getCoverFileId().equals(oldCoverFileId)) {
            storageRefService.bind(book.getCoverFileId(), StorageRefService.BIZ_BOOK, id);
        }
        if (oldSourceFileId != null && !oldSourceFileId.equals(book.getSourceFileId())) {
            storageRefService.unbindFile(oldSourceFileId, StorageRefService.BIZ_BOOK, id);
        }
        if (book.getSourceFileId() != null && !book.getSourceFileId().equals(oldSourceFileId)) {
            storageRefService.bind(book.getSourceFileId(), StorageRefService.BIZ_BOOK, id);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireBook(id);
        // 章节与分章任务随书删除；文件引用解绑（引用归零异步物理清理）
        bookChapterRepository.deleteByBookId(id);
        bookAiTaskRepository.findFirstByBookIdOrderByIdDesc(id)
                .ifPresent(bookAiTaskRepository::delete);
        bookRepository.deleteById(id);
        storageRefService.unbindBiz(StorageRefService.BIZ_BOOK, id);
    }

    /**
     * 应用保存请求（封面/txt 源文件防呆校验；txt 换新时重置分章状态由管理端重新触发）
     */
    private void applyRequest(Book book, BookSaveRequest request) {
        book.setTitle(request.title().strip());
        book.setAuthor(request.author() == null ? null : request.author().strip());
        book.setCoverFileId(requireValidFileId(request.coverFileId()));
        book.setIntro(request.intro() == null ? null : request.intro().strip());
        book.setOwnershipType(request.ownershipType());
        // txt 源文件必须是文本（txt/md）
        Long sourceFileId = request.sourceFileId();
        if (sourceFileId != null) {
            StorageFile file = requireValidFile(sourceFileId);
            String ext = FileTypeValidator.extractExt(file.getOriginalName());
            if (!"txt".equals(ext) && !"md".equals(ext)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "源文件仅支持 txt/md 文本");
            }
        }
        book.setSourceFileId(sourceFileId);
        book.setIsRecommend(request.isRecommend() == null ? 0 : request.isRecommend());
    }

    private Book requireBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
    }

    /**
     * 校验文件 ID 指向正常状态文件（防呆：不允许引用已删除/不存在的文件）
     *
     * @return fileId 为空返回 null（封面可空）
     */
    private Long requireValidFileId(Long fileId) {
        if (fileId == null) {
            return null;
        }
        storageFileRepository.findById(fileId)
                .filter(file -> file.getStatus() == 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件不存在"));
        return fileId;
    }

    private StorageFile requireValidFile(Long fileId) {
        return storageFileRepository.findById(fileId)
                .filter(file -> file.getStatus() == 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件不存在"));
    }

    private BookAdminVO toAdminVO(Book book) {
        return new BookAdminVO(book.getId(), book.getTitle(), book.getAuthor(),
                book.getCoverFileId() == null ? null : fileUploadService.signUrl(book.getCoverFileId()),
                book.getIntro(), book.getOwnershipType(), book.getSourceFileId(),
                book.getTotalChapters(), book.getIsRecommend(), book.getCreateTime());
    }
}
