package top.heyqing.aether.controller.article;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.common.Result;
import top.heyqing.aether.model.dto.ArticleQuery;
import top.heyqing.aether.model.vo.ArticleDetailVO;
import top.heyqing.aether.model.vo.ArticleListVO;
import top.heyqing.aether.model.vo.CategoryVO;
import top.heyqing.aether.model.vo.TagVO;
import top.heyqing.aether.service.article.ArticleService;
import top.heyqing.aether.util.IpUtil;

/**
 * 文章公开接口（BackEnd-Plan §5.2 文章 article 公开）
 *
 * <p>分类/标签接口为全站通用字典接口（bizType 过滤，默认 article）。</p>
 */
@RestController
@RequestMapping("/v1")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /**
     * 文章分页列表：分类/标签过滤 + 关键词搜索（标题/简介/内容）+ 排序
     */
    @GetMapping("/articles")
    public Result<PageResult<ArticleListVO>> articles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort) {
        return Result.ok(articleService.pageList(
                new ArticleQuery(page, size, categoryId, tagId, keyword, sort)));
    }

    /**
     * 热门文章（hot_order 排序，limit 6-10）
     */
    @GetMapping("/articles/hot")
    public Result<List<ArticleListVO>> hot(@RequestParam(defaultValue = "6") int limit) {
        return Result.ok(articleService.hot(limit));
    }

    /**
     * 文章详情（阅读数按访问者 IP 24 小时去重自增）
     *
     * <p>前端 SSR/RSC 渲染预取时携带 X-Aether-No-Count: 1（服务端请求的 IP
     * 是渲染服务器而非访客，跳过计数）；浏览器端 hydrate 补偿请求不带头，
     * 以真实访客 IP 计数。</p>
     */
    @GetMapping("/articles/{id}")
    public Result<ArticleDetailVO> detail(@PathVariable Long id, HttpServletRequest request) {
        String clientIp = "1".equals(request.getHeader("X-Aether-No-Count"))
                ? null
                : IpUtil.clientIp(request);
        return Result.ok(articleService.detail(id, clientIp));
    }

    /**
     * 相关文章（同分类优先 + 最新兜底，6 条）
     */
    @GetMapping("/articles/{id}/related")
    public Result<List<ArticleListVO>> related(@PathVariable Long id) {
        return Result.ok(articleService.related(id));
    }

    /**
     * 分类列表（全站字典）
     */
    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories(@RequestParam(defaultValue = "article") String bizType) {
        return Result.ok(articleService.listCategories(bizType));
    }

    /**
     * 标签列表（全站字典）
     */
    @GetMapping("/tags")
    public Result<List<TagVO>> tags(@RequestParam(defaultValue = "article") String bizType) {
        return Result.ok(articleService.listTags(bizType));
    }
}
