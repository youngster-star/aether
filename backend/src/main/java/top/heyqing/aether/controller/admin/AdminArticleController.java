package top.heyqing.aether.controller.admin;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import top.heyqing.aether.aspect.OperationLog;
import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.common.Result;
import top.heyqing.aether.model.dto.ArticleHotRequest;
import top.heyqing.aether.model.dto.ArticleSaveRequest;
import top.heyqing.aether.model.vo.ArticleAdminVO;
import top.heyqing.aether.service.article.ArticleAdminService;

/**
 * 文章管理接口（BackEnd-Plan §5.2 /admin/articles，需 Access Token）
 */
@RestController
@RequestMapping("/v1/admin/articles")
public class AdminArticleController {

    private final ArticleAdminService articleAdminService;

    public AdminArticleController(ArticleAdminService articleAdminService) {
        this.articleAdminService = articleAdminService;
    }

    /**
     * 管理端分页列表（含草稿）
     */
    @GetMapping
    public Result<PageResult<ArticleAdminVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isPublished) {
        return Result.ok(articleAdminService.list(page, size, keyword, isPublished));
    }

    /**
     * 管理端详情（含草稿与编辑回显字段）
     */
    @GetMapping("/{id}")
    public Result<ArticleAdminVO> get(@PathVariable Long id) {
        return Result.ok(articleAdminService.get(id));
    }

    /**
     * 新建文章（默认草稿）
     */
    @PostMapping
    @OperationLog(module = "文章", action = "新增")
    public Result<Long> create(@Valid @RequestBody ArticleSaveRequest request) {
        return Result.ok(articleAdminService.create(request));
    }

    /**
     * 编辑文章
     */
    @PutMapping("/{id}")
    @OperationLog(module = "文章", action = "编辑")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleSaveRequest request) {
        articleAdminService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "文章", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        articleAdminService.delete(id);
        return Result.ok();
    }

    /**
     * 发布/下线
     */
    @PutMapping("/{id}/publish")
    @OperationLog(module = "文章", action = "发布")
    public Result<Void> publish(@PathVariable Long id, @RequestParam Integer isPublished) {
        articleAdminService.publish(id, isPublished);
        return Result.ok();
    }

    /**
     * 设置热度
     */
    @PutMapping("/{id}/hot")
    @OperationLog(module = "文章", action = "热度设置")
    public Result<Void> setHot(@PathVariable Long id, @Valid @RequestBody ArticleHotRequest request) {
        articleAdminService.setHot(id, request);
        return Result.ok();
    }

    /**
     * 绑定独立样式（styleId 传 null 解绑回默认）
     */
    @PutMapping("/{id}/style")
    @OperationLog(module = "文章", action = "样式绑定")
    public Result<Void> bindStyle(@PathVariable Long id, @RequestParam(required = false) Long styleId) {
        articleAdminService.bindStyle(id, styleId);
        return Result.ok();
    }
}
