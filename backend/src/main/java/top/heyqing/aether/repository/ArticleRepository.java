package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import top.heyqing.aether.model.entity.Article;

/**
 * 文章仓库（BackEnd-Plan §5.2 文章接口）
 *
 * <p>列表/搜索走 Specification（参数绑定防注入，BackEnd-Plan §4.3）；
 * 阅读数自增用原子 UPDATE 避免读改写竞态。</p>
 */
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    /**
     * 热门文章（BackEnd-Plan §5.2 /articles/hot：hot_order 排序，不返回创建时间）
     */
    List<Article> findByIsPublishedAndIsHotOrderByHotOrderDescPublishTimeDesc(Integer isPublished, Integer isHot,
                                                                              Pageable pageable);

    /**
     * 相关文章兜底：排除自身的最新已发布文章
     */
    @Query("""
            SELECT a FROM Article a
            WHERE a.isPublished = 1 AND a.id <> :articleId
            ORDER BY a.publishTime DESC, a.id DESC
            """)
    List<Article> findLatestPublishedExcept(@Param("articleId") Long articleId, Pageable pageable);

    /**
     * 相关文章优先：同分类的最新已发布文章（相关文章接口第一步，不足再兜底）
     */
    @Query("""
            SELECT a FROM Article a
            WHERE a.isPublished = 1 AND a.id <> :articleId
              AND a.id IN (SELECT r.bizId FROM BizCategoryRel r
                           WHERE r.bizType = 'article' AND r.categoryId IN :categoryIds)
            ORDER BY a.publishTime DESC, a.id DESC
            """)
    List<Article> findPublishedByCategoriesExcept(@Param("articleId") Long articleId,
                                                  @Param("categoryIds") List<Long> categoryIds,
                                                  Pageable pageable);

    /**
     * 阅读数原子自增（详情接口：IP 24 小时去重命中时 +1）
     */
    @Modifying
    @Query("UPDATE Article a SET a.readingCount = a.readingCount + 1 WHERE a.id = :articleId")
    int incrementReadingCount(@Param("articleId") Long articleId);

    /**
     * 样式被引用校验（管理端删除样式防呆）
     */
    boolean existsByArticleStyleId(Long articleStyleId);
}
