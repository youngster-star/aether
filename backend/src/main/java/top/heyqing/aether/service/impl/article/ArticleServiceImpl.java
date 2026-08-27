package top.heyqing.aether.service.impl.article;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.common.cache.CacheStore;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.ArticleQuery;
import top.heyqing.aether.model.entity.Article;
import top.heyqing.aether.model.entity.ArticleStyle;
import top.heyqing.aether.model.entity.BizCategoryRel;
import top.heyqing.aether.model.entity.BizTagRel;
import top.heyqing.aether.model.vo.ArticleDetailVO;
import top.heyqing.aether.model.vo.ArticleListVO;
import top.heyqing.aether.model.vo.ArticleStyleVO;
import top.heyqing.aether.model.vo.CategoryVO;
import top.heyqing.aether.model.vo.TagVO;
import top.heyqing.aether.repository.ArticleRepository;
import top.heyqing.aether.repository.ArticleStyleRepository;
import top.heyqing.aether.repository.BizCategoryRelRepository;
import top.heyqing.aether.repository.BizTagRelRepository;
import top.heyqing.aether.repository.CategoryRepository;
import top.heyqing.aether.repository.TagRepository;
import top.heyqing.aether.service.article.ArticleService;
import top.heyqing.aether.service.storage.FileUploadService;

/**
 * 文章公开查询实现（BackEnd-Plan §5.2）
 *
 * <p>要点：搜索走 Specification EXISTS 子查询 + LIKE 参数绑定（防注入，§4.3）；
 * 列表装配批量查关联（防 N+1）；封面输出签名 URL（防直链，§4.4）；
 * 阅读数 IP 24 小时去重（§4.6）。</p>
 */
@Service
public class ArticleServiceImpl implements ArticleService {

    /** 阅读去重 key 前缀（+articleId:ip），24h TTL */
    private static final String READ_KEY = "article:read:";

    private static final Duration READ_TTL = Duration.ofHours(24);

    /** 相关文章条数（BackEnd-Plan §5.2） */
    private static final int RELATED_LIMIT = 6;

    private final ArticleRepository articleRepository;
    private final ArticleStyleRepository articleStyleRepository;
    private final BizCategoryRelRepository bizCategoryRelRepository;
    private final BizTagRelRepository bizTagRelRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final FileUploadService fileUploadService;
    private final CacheStore cacheStore;

    public ArticleServiceImpl(ArticleRepository articleRepository,
                              ArticleStyleRepository articleStyleRepository,
                              BizCategoryRelRepository bizCategoryRelRepository,
                              BizTagRelRepository bizTagRelRepository,
                              CategoryRepository categoryRepository,
                              TagRepository tagRepository,
                              FileUploadService fileUploadService,
                              CacheStore cacheStore) {
        this.articleRepository = articleRepository;
        this.articleStyleRepository = articleStyleRepository;
        this.bizCategoryRelRepository = bizCategoryRelRepository;
        this.bizTagRelRepository = bizTagRelRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.fileUploadService = fileUploadService;
        this.cacheStore = cacheStore;
    }

    @Override
    public PageResult<ArticleListVO> pageList(ArticleQuery query) {
        Sort sort = "hot".equals(query.sort())
                ? Sort.by(Sort.Direction.DESC, "isHot")
                        .and(Sort.by(Sort.Direction.DESC, "hotOrder"))
                        .and(Sort.by(Sort.Direction.DESC, "publishTime"))
                        .and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.DESC, "publishTime")
                        .and(Sort.by(Sort.Direction.DESC, "id"));
        Page<Article> result = articleRepository.findAll(
                buildSpecification(query), PageRequest.of(query.page() - 1, query.size(), sort));
        return PageResult.of(buildListVOs(result.getContent()), result.getTotalElements(),
                query.page(), query.size());
    }

    @Override
    public List<ArticleListVO> hot(int limit) {
        int safeLimit = Math.min(Math.max(limit, 6), 10);
        List<Article> articles = articleRepository.findByIsPublishedAndIsHotOrderByHotOrderDescPublishTimeDesc(
                1, 1, PageRequest.of(0, safeLimit));
        return buildListVOs(articles);
    }

    @Override
    @Transactional
    public ArticleDetailVO detail(Long id, String clientIp) {
        Article article = articleRepository.findById(id)
                .filter(a -> a.getIsPublished() == 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        // 阅读数：同 IP 24h 内只计 1 次（setIfAbsent 原子；命中即自增）
        int readingCount = article.getReadingCount();
        if (clientIp != null && cacheStore.setIfAbsent(READ_KEY + id + ":" + clientIp, "1", READ_TTL)) {
            articleRepository.incrementReadingCount(id);
            readingCount++;
        }
        return new ArticleDetailVO(
                article.getId(), article.getTitle(), article.getSummary(),
                coverUrl(article.getCoverFileId()), article.getContentHtml(),
                article.getWordCount(), readingCount, article.getIsHot(),
                article.getPublishTime(), article.getUpdateTime(),
                resolveStyle(article.getArticleStyleId()),
                listCategoriesOf(article.getId()), listTagsOf(article.getId()));
    }

    @Override
    public List<ArticleListVO> related(Long id) {
        // 校验文章存在且已发布（草稿/不存在不返回相关文章）
        articleRepository.findById(id)
                .filter(a -> a.getIsPublished() == 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        List<Long> categoryIds = bizCategoryRelRepository.findByBizTypeAndBizId("article", id).stream()
                .map(BizCategoryRel::getCategoryId).toList();

        // 同分类优先，不足 RELATED_LIMIT 用最新文章兜底补齐
        Map<Long, Article> merged = new LinkedHashMap<>();
        if (!categoryIds.isEmpty()) {
            articleRepository.findPublishedByCategoriesExcept(id, categoryIds, PageRequest.of(0, RELATED_LIMIT))
                    .forEach(a -> merged.put(a.getId(), a));
        }
        if (merged.size() < RELATED_LIMIT) {
            articleRepository.findLatestPublishedExcept(id, PageRequest.of(0, RELATED_LIMIT))
                    .forEach(a -> merged.putIfAbsent(a.getId(), a));
        }
        return buildListVOs(new ArrayList<>(merged.values()));
    }

    @Override
    public List<CategoryVO> listCategories(String bizType) {
        return categoryRepository.findByBizTypeOrderBySortAsc(bizType).stream()
                .map(c -> new CategoryVO(c.getId(), c.getName(), c.getSlug(), c.getSort()))
                .toList();
    }

    @Override
    public List<TagVO> listTags(String bizType) {
        return tagRepository.findByBizType(bizType).stream()
                .map(t -> new TagVO(t.getId(), t.getName(), t.getSlug()))
                .toList();
    }

    /**
     * 列表查询 Specification：已发布 + 可选分类/标签过滤 + 关键词 OR LIKE
     *
     * <p>分类/标签过滤用 EXISTS 子查询关联 biz_category_rel/biz_tag_rel；
     * 全部参数绑定，禁止拼接 SQL（BackEnd-Plan §4.3）。</p>
     */
    private Specification<Article> buildSpecification(ArticleQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isPublished"), 1));
            if (query.categoryId() != null) {
                predicates.add(cb.exists(relSubquery(query.categoryId(), cb, root,
                        "categoryId", BizCategoryRel.class)));
            }
            if (query.tagId() != null) {
                predicates.add(cb.exists(relSubquery(query.tagId(), cb, root,
                        "tagId", BizTagRel.class)));
            }
            String keyword = query.keyword();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), pattern),
                        cb.like(root.get("summary"), pattern),
                        cb.like(root.get("contentHtml"), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * EXISTS 子查询：该文章存在指定 categoryId/tagId 的关联记录
     *
     * <p>按关联表实体类型动态构建（分类/标签共用一套逻辑），全部参数绑定。</p>
     */
    private <T> Subquery<Long> relSubquery(Long relId, jakarta.persistence.criteria.CriteriaBuilder cb,
                                           Root<Article> root, String relColumn, Class<T> relType) {
        Subquery<Long> subquery = cb.createQuery().subquery(Long.class);
        Root<T> relRoot = subquery.from(relType);
        subquery.select(relRoot.get("id"));
        subquery.where(cb.equal(relRoot.get("bizType"), "article"),
                cb.equal(relRoot.get("bizId"), root.get("id")),
                cb.equal(relRoot.get(relColumn), relId));
        return subquery;
    }

    /**
     * 批量装配列表 VO：一次批量查分类/标签关联（防 N+1），封面走签名 URL
     */
    private List<ArticleListVO> buildListVOs(List<Article> articles) {
        if (articles.isEmpty()) {
            return List.of();
        }
        List<Long> ids = articles.stream().map(Article::getId).toList();

        // 批量加载分类/标签关联 → 字典 → 每篇组装（两次关联查询 + 两次字典查询，与文章数无关）
        Map<Long, List<CategoryVO>> categories = loadCategories(ids);
        Map<Long, List<TagVO>> tags = loadTags(ids);

        return articles.stream().map(a -> new ArticleListVO(
                a.getId(), a.getTitle(), a.getSummary(), coverUrl(a.getCoverFileId()),
                a.getReadingCount(), a.getIsHot(), a.getPublishTime(),
                categories.getOrDefault(a.getId(), List.of()),
                tags.getOrDefault(a.getId(), List.of()))).toList();
    }

    private Map<Long, List<CategoryVO>> loadCategories(List<Long> articleIds) {
        List<BizCategoryRel> rels = bizCategoryRelRepository.findByBizTypeAndBizIdIn("article", articleIds);
        Map<Long, CategoryVO> dict = categoryRepository.findAllById(
                        rels.stream().map(BizCategoryRel::getCategoryId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(
                        c -> c.getId(), c -> new CategoryVO(c.getId(), c.getName(), c.getSlug(), c.getSort())));
        Map<Long, List<CategoryVO>> result = new HashMap<>();
        for (BizCategoryRel rel : rels) {
            CategoryVO vo = dict.get(rel.getCategoryId());
            if (vo != null) {
                result.computeIfAbsent(rel.getBizId(), k -> new ArrayList<>()).add(vo);
            }
        }
        return result;
    }

    private Map<Long, List<TagVO>> loadTags(List<Long> articleIds) {
        List<BizTagRel> rels = bizTagRelRepository.findByBizTypeAndBizIdIn("article", articleIds);
        Map<Long, TagVO> dict = tagRepository.findAllById(
                        rels.stream().map(BizTagRel::getTagId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(
                        t -> t.getId(), t -> new TagVO(t.getId(), t.getName(), t.getSlug())));
        Map<Long, List<TagVO>> result = new HashMap<>();
        for (BizTagRel rel : rels) {
            TagVO vo = dict.get(rel.getTagId());
            if (vo != null) {
                result.computeIfAbsent(rel.getBizId(), k -> new ArrayList<>()).add(vo);
            }
        }
        return result;
    }

    private List<CategoryVO> listCategoriesOf(Long articleId) {
        List<Long> categoryIds = bizCategoryRelRepository.findByBizTypeAndBizId("article", articleId).stream()
                .map(BizCategoryRel::getCategoryId).toList();
        return categoryIds.isEmpty() ? List.of()
                : categoryRepository.findAllById(categoryIds).stream()
                        .map(c -> new CategoryVO(c.getId(), c.getName(), c.getSlug(), c.getSort())).toList();
    }

    private List<TagVO> listTagsOf(Long articleId) {
        List<Long> tagIds = bizTagRelRepository.findByBizTypeAndBizId("article", articleId).stream()
                .map(BizTagRel::getTagId).toList();
        return tagIds.isEmpty() ? List.of()
                : tagRepository.findAllById(tagIds).stream()
                        .map(t -> new TagVO(t.getId(), t.getName(), t.getSlug())).toList();
    }

    /**
     * 样式解析：文章绑定样式优先；未绑定时回退默认样式（is_default=1），无默认返回 null（前端用内置默认）
     */
    private ArticleStyleVO resolveStyle(Long articleStyleId) {
        ArticleStyle style = articleStyleId != null
                ? articleStyleRepository.findById(articleStyleId).orElse(null)
                : articleStyleRepository.findByIsDefault(1).orElse(null);
        return style == null ? null : new ArticleStyleVO(style.getId(), style.getName(),
                style.getStyleJson(), style.getIsDefault());
    }

    private String coverUrl(Long coverFileId) {
        return coverFileId == null ? null : fileUploadService.signUrl(coverFileId);
    }
}
