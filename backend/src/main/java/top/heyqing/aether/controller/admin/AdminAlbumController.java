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
import top.heyqing.aether.model.dto.AlbumImageBatchRequest;
import top.heyqing.aether.model.dto.AlbumSaveRequest;
import top.heyqing.aether.model.vo.AlbumAdminVO;
import top.heyqing.aether.service.album.AlbumAdminService;

/**
 * 图集管理接口（BackEnd-Plan §5.2 /admin/albums，需 Access Token）
 */
@RestController
@RequestMapping("/v1/admin/albums")
public class AdminAlbumController {

    private final AlbumAdminService albumAdminService;

    public AdminAlbumController(AlbumAdminService albumAdminService) {
        this.albumAdminService = albumAdminService;
    }

    /**
     * 管理端分页列表
     */
    @GetMapping
    public Result<PageResult<AlbumAdminVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(albumAdminService.list(page, size, keyword));
    }

    /**
     * 新建图集
     */
    @PostMapping
    @OperationLog(module = "图集", action = "新增")
    public Result<Long> create(@Valid @RequestBody AlbumSaveRequest request) {
        return Result.ok(albumAdminService.create(request));
    }

    /**
     * 编辑图集
     */
    @PutMapping("/{id}")
    @OperationLog(module = "图集", action = "编辑")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AlbumSaveRequest request) {
        albumAdminService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除图集（含全部图片，引用归零文件进入物理清理链路）
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "图集", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        albumAdminService.delete(id);
        return Result.ok();
    }

    /**
     * 批量添加图片（含排序/标题/介绍）
     */
    @PostMapping("/{id}/images")
    @OperationLog(module = "图集", action = "添加图片")
    public Result<Void> addImages(@PathVariable Long id, @Valid @RequestBody AlbumImageBatchRequest request) {
        albumAdminService.addImages(id, request);
        return Result.ok();
    }

    /**
     * 删除单张图片
     */
    @DeleteMapping("/{id}/images/{imageId}")
    @OperationLog(module = "图集", action = "删除图片")
    public Result<Void> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        albumAdminService.deleteImage(id, imageId);
        return Result.ok();
    }
}
