package top.heyqing.aether.service.article;

import java.util.List;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.model.dto.ArticleQuery;
import top.heyqing.aether.model.vo.ArticleDetailVO;
import top.heyqing.aether.model.vo.ArticleListVO;
import top.heyqing.aether.model.vo.CategoryVO;
import top.heyqing.aether.model.vo.TagVO;

/**
 * 文章公开查询服务（BackEnd-Plan §5.2 文章 article 公开接口）
 */
public interface ArticleService {

    /**
     * 已发布文章分页列表（分类/标签过滤 + 关键词搜索 + 排序）
     */
    PageResult<ArticleListVO> pageList(ArticleQuery query);

    /**
     * 热门文章（hot_order 排序，limit 6-10，BackEnd-Plan §5.2 /articles/hot）
     */
    List<ArticleListVO> hot(int limit);

    /**
     * 文章详情（含样式与分类标签；阅读数按 IP 24 小时去重自增）
     *
     * @param id       文章 ID
     * @param clientIp 访问者 IP（阅读去重依据）
     */
    ArticleDetailVO detail(Long id, String clientIp);

    /**
     * 相关文章（同分类优先 + 最新兜底，6 条）
     */
    List<ArticleListVO> related(Long id);

    /**
     * 分类列表（全站字典，bizType 过滤）
     */
    List<CategoryVO> listCategories(String bizType);

    /**
     * 标签列表（全站字典，bizType 过滤）
     */
    List<TagVO> listTags(String bizType);
}
