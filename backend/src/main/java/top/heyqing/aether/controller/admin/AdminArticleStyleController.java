package top.heyqing.aether.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import top.heyqing.aether.aspect.OperationLog;
import top.heyqing.aether.common.Result;
import top.heyqing.aether.model.dto.ArticleStyleRequest;
import top.heyqing.aether.model.vo.ArticleStyleVO;
import top.heyqing.aether.service.article.ArticleStyleService;

/**
 * 文章样式管理接口（BackEnd-Plan §5.2 /admin/article-styles）
 */
@RestController
@RequestMapping("/v1/admin/article-styles")
public class AdminArticleStyleController {

    private final ArticleStyleService articleStyleService;

    public AdminArticleStyleController(ArticleStyleService articleStyleService) {
        this.articleStyleService = articleStyleService;
    }

    @GetMapping
    public Result<List<ArticleStyleVO>> list() {
        return Result.ok(articleStyleService.list());
    }

    @PostMapping
    @OperationLog(module = "样式", action = "新增")
    public Result<Long> create(@Valid @RequestBody ArticleStyleRequest request) {
        return Result.ok(articleStyleService.create(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "样式", action = "编辑")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleStyleRequest request) {
        articleStyleService.update(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "样式", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        articleStyleService.delete(id);
        return Result.ok();
    }
}
