package top.heyqing.aether.service.impl.article;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
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
import top.heyqing.aether.model.dto.ArticleHotRequest;
import top.heyqing.aether.model.dto.ArticleSaveRequest;
import top.heyqing.aether.model.entity.Article;
import top.heyqing.aether.model.entity.BizCategoryRel;
import top.heyqing.aether.model.entity.BizTagRel;
import top.heyqing.aether.model.vo.ArticleAdminVO;
import top.heyqing.aether.repository.ArticleRepository;
import top.heyqing.aether.repository.ArticleStyleRepository;
import top.heyqing.aether.repository.BizCategoryRelRepository;
import top.heyqing.aether.repository.BizTagRelRepository;
import top.heyqing.aether.repository.CategoryRepository;
import top.heyqing.aether.repository.TagRepository;
import top.heyqing.aether.service.article.ArticleAdminService;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.util.XssSanitizer;

/**
 * 文章管理实现（BackEnd-Plan §5.2 /admin/articles）
 *
 * <p>内容入库前统一 XSS sanitize（§4.3）；字数按 sanitize 后纯文本统计；
 * 分类标签关联在事务内整体重建；发布/热度/样式绑定独立小事务。</p>
 */
@Service
public class ArticleAdminServiceImpl implements ArticleAdminService {

    private final ArticleRepository articleRepository;
    private final ArticleStyleRepository articleStyleRepository;
    private final BizCategoryRelRepository bizCategoryRelRepository;
    private final BizTagRelRepository bizTagRelRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final FileUploadService fileUploadService;

    public ArticleAdminServiceImpl(ArticleRepository articleRepository,
                                   ArticleStyleRepository articleStyleRepository,
                                   BizCategoryRelRepository bizCategoryRelRepository,
                                   BizTagRelRepository bizTagRelRepository,
                                   CategoryRepository categoryRepository,
                                   TagRepository tagRepository,
                                   FileUploadService fileUploadService) {
        this.articleRepository = articleRepository;
        this.articleStyleRepository = articleStyleRepository;
        this.bizCategoryRelRepository = bizCategoryRelRepository;
        this.bizTagRelRepository = bizTagRelRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public PageResult<ArticleAdminVO> list(int page, int size, String keyword, Integer isPublished) {
        Specification<Article> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isPublished != null) {
                predicates.add(cb.equal(root.get("isPublished"), isPublished));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(cb.like(root.get("title"), pattern),
                        cb.like(root.get("summary"), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Article> result = articleRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.of(result.getContent().stream().map(this::toAdminVO).toList(),
                result.getTotalElements(), page, size);
    }

    @Override
    public ArticleAdminVO get(Long id) {
        return toAdminVO(requireArticle(id));
    }

    @Override
    @Transactional
    public Long create(ArticleSaveRequest request) {
        Article article = new Article();
        applyRequest(article, request);
        article.setIsPublished(0);
        article.setReadingCount(0);
        article.setIsHot(0);
        article.setHotOrder(0);
        articleRepository.save(article);
        saveRels(article.getId(), request.categoryIds(), request.tagIds());
        return article.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ArticleSaveRequest request) {
        Article article = requireArticle(id);
        applyRequest(article, request);
        articleRepository.save(article);
        saveRels(id, request.categoryIds(), request.tagIds());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Article article = requireArticle(id);
        // 关联清理 + 物理删除（封面文件物理清理待 storage_ref 机制接入，见接口注释）
        bizCategoryRelRepository.deleteByBizTypeAndBizId("article", id);
        bizTagRelRepository.deleteByBizTypeAndBizId("article", id);
        articleRepository.delete(article);
    }

    @Override
    @Transactional
    public void publish(Long id, Integer isPublished) {
        Article article = requireArticle(id);
        article.setIsPublished(isPublished);
        // 首次发布记录发布时间（重复发布不刷新，保持原发布时间）
        if (isPublished == 1 && article.getPublishTime() == null) {
            article.setPublishTime(LocalDateTime.now());
        }
        articleRepository.save(article);
    }

    @Override
    @Transactional
    public void setHot(Long id, ArticleHotRequest request) {
        Article article = requireArticle(id);
        article.setIsHot(request.isHot());
        article.setHotOrder(request.hotOrder());
        articleRepository.save(article);
    }

    @Override
    @Transactional
    public void bindStyle(Long id, Long styleId) {
        Article article = requireArticle(id);
        if (styleId != null && !articleStyleRepository.existsById(styleId)) {
            throw new BusinessException(ErrorCode.STYLE_NOT_FOUND);
        }
        article.setArticleStyleId(styleId);
        articleRepository.save(article);
    }

    /**
     * 把入参应用到实体（sanitize + 字数统计；关联与发布状态由调用方处理）
     */
    private void applyRequest(Article article, ArticleSaveRequest request) {
        article.setTitle(request.title().trim());
        article.setSummary(request.summary());
        article.setCoverFileId(request.coverFileId());
        article.setContentHtml(XssSanitizer.clean(request.contentHtml()));
        article.setContentMd(request.contentMd() == null ? "" : request.contentMd());
        // 字数：sanitize 后纯文本长度（中文按字符计）
        article.setWordCount(Jsoup.parse(article.getContentHtml()).text().length());
        article.setArticleStyleId(request.articleStyleId());
    }

    /**
     * 重建分类/标签关联（防呆：校验传入的字典 ID 真实存在）
     */
    private void saveRels(Long articleId, List<Long> categoryIds, List<Long> tagIds) {
        bizCategoryRelRepository.deleteByBizTypeAndBizId("article", articleId);
        bizTagRelRepository.deleteByBizTypeAndBizId("article", articleId);

        List<Long> distinctCategories = distinctNonNull(categoryIds);
        if (!distinctCategories.isEmpty()
                && categoryRepository.findAllById(distinctCategories).size() != distinctCategories.size()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        distinctCategories.forEach(categoryId -> {
            BizCategoryRel rel = new BizCategoryRel();
            rel.setBizType("article");
            rel.setBizId(articleId);
            rel.setCategoryId(categoryId);
            bizCategoryRelRepository.save(rel);
        });

        List<Long> distinctTags = distinctNonNull(tagIds);
        if (!distinctTags.isEmpty()
                && tagRepository.findAllById(distinctTags).size() != distinctTags.size()) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        distinctTags.forEach(tagId -> {
            BizTagRel rel = new BizTagRel();
            rel.setBizType("article");
            rel.setBizId(articleId);
            rel.setTagId(tagId);
            bizTagRelRepository.save(rel);
        });
    }

    private Article requireArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
    }

    private ArticleAdminVO toAdminVO(Article a) {
        List<Long> categoryIds = bizCategoryRelRepository.findByBizTypeAndBizId("article", a.getId())
                .stream().map(BizCategoryRel::getCategoryId).toList();
        List<Long> tagIds = bizTagRelRepository.findByBizTypeAndBizId("article", a.getId())
                .stream().map(BizTagRel::getTagId).toList();
        return new ArticleAdminVO(a.getId(), a.getTitle(), a.getCoverFileId(),
                a.getCoverFileId() == null ? null : fileUploadService.signUrl(a.getCoverFileId()),
                a.getSummary(), a.getContentHtml(), a.getContentMd(), a.getWordCount(),
                a.getReadingCount(), a.getIsHot(), a.getHotOrder(), a.getArticleStyleId(),
                a.getIsPublished(), a.getPublishTime(), a.getCreateTime(), a.getUpdateTime(),
                categoryIds, tagIds);
    }

    private static List<Long> distinctNonNull(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(id -> id != null).distinct().toList();
    }
}
