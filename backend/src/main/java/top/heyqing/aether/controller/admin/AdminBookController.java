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
import top.heyqing.aether.model.dto.BookSaveRequest;
import top.heyqing.aether.model.dto.SplitConfirmRequest;
import top.heyqing.aether.model.vo.BookAdminVO;
import top.heyqing.aether.model.vo.SplitTaskVO;
import top.heyqing.aether.service.book.BookAdminService;
import top.heyqing.aether.service.book.BookSplitService;

/**
 * 书籍管理接口（BackEnd-Plan §5.2 /admin/books，需 Access Token）
 */
@RestController
@RequestMapping("/v1/admin/books")
public class AdminBookController {

    private final BookAdminService bookAdminService;
    private final BookSplitService bookSplitService;

    public AdminBookController(BookAdminService bookAdminService, BookSplitService bookSplitService) {
        this.bookAdminService = bookAdminService;
        this.bookSplitService = bookSplitService;
    }

    /**
     * 管理端分页列表（keyword 匹配书名/作者）
     */
    @GetMapping
    public Result<PageResult<BookAdminVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(bookAdminService.list(page, size, keyword));
    }

    /**
     * 新建书籍（上传封面/txt 后引用文件 ID）
     */
    @PostMapping
    @OperationLog(module = "书籍", action = "新增")
    public Result<Long> create(@Valid @RequestBody BookSaveRequest request) {
        return Result.ok(bookAdminService.create(request));
    }

    /**
     * 编辑书籍（含换封面/换 txt 源文件，旧引用自动解绑）
     */
    @PutMapping("/{id}")
    @OperationLog(module = "书籍", action = "编辑")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BookSaveRequest request) {
        bookAdminService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除书籍（章节/分章任务/文件引用一并清理）
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "书籍", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        bookAdminService.delete(id);
        return Result.ok();
    }

    /**
     * 触发 AI 分章（异步：python-agent 优先，不可达降级 Java 启发式，§7.2）
     */
    @PostMapping("/{id}/split")
    @OperationLog(module = "书籍", action = "触发分章")
    public Result<Long> split(@PathVariable Long id) {
        return Result.ok(bookSplitService.trigger(id));
    }

    /**
     * 查询分章任务状态与建议（管理端轮询）
     */
    @GetMapping("/{id}/split-task")
    public Result<SplitTaskVO> splitTask(@PathVariable Long id) {
        return Result.ok(bookSplitService.task(id));
    }

    /**
     * 确认分章并落库（预览编辑器可增删改/合并拆分后提交）
     */
    @PostMapping("/{id}/chapters/confirm")
    @OperationLog(module = "书籍", action = "确认分章")
    public Result<Void> confirm(@PathVariable Long id, @Valid @RequestBody SplitConfirmRequest request) {
        bookSplitService.confirm(id, request);
        return Result.ok();
    }
}
