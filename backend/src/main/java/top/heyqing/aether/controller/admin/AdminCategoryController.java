package top.heyqing.aether.controller.admin;

import java.util.List;

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
import top.heyqing.aether.common.Result;
import top.heyqing.aether.model.dto.CategorySaveRequest;
import top.heyqing.aether.model.dto.TagSaveRequest;
import top.heyqing.aether.model.vo.CategoryVO;
import top.heyqing.aether.model.vo.TagVO;
import top.heyqing.aether.service.article.CategoryService;

/**
 * 分类/标签字典管理接口（BackEnd-Plan §5.2 /admin/categories、/admin/tags）
 */
@RestController
@RequestMapping("/v1/admin")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // ===== 分类 =====

    @GetMapping("/categories")
    public Result<List<CategoryVO>> listCategories(@RequestParam(defaultValue = "article") String bizType) {
        return Result.ok(categoryService.listCategories(bizType));
    }

    @PostMapping("/categories")
    @OperationLog(module = "分类", action = "新增")
    public Result<Long> createCategory(@Valid @RequestBody CategorySaveRequest request) {
        return Result.ok(categoryService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @OperationLog(module = "分类", action = "编辑")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody CategorySaveRequest request) {
        categoryService.updateCategory(id, request);
        return Result.ok();
    }

    @DeleteMapping("/categories/{id}")
    @OperationLog(module = "分类", action = "删除")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.ok();
    }

    // ===== 标签 =====

    @GetMapping("/tags")
    public Result<List<TagVO>> listTags(@RequestParam(defaultValue = "article") String bizType) {
        return Result.ok(categoryService.listTags(bizType));
    }

    @PostMapping("/tags")
    @OperationLog(module = "标签", action = "新增")
    public Result<Long> createTag(@Valid @RequestBody TagSaveRequest request) {
        return Result.ok(categoryService.createTag(request));
    }

    @PutMapping("/tags/{id}")
    @OperationLog(module = "标签", action = "编辑")
    public Result<Void> updateTag(@PathVariable Long id, @Valid @RequestBody TagSaveRequest request) {
        categoryService.updateTag(id, request);
        return Result.ok();
    }

    @DeleteMapping("/tags/{id}")
    @OperationLog(module = "标签", action = "删除")
    public Result<Void> deleteTag(@PathVariable Long id) {
        categoryService.deleteTag(id);
        return Result.ok();
    }
}
