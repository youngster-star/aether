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
import top.heyqing.aether.model.dto.VideoChapterBatchRequest;
import top.heyqing.aether.model.dto.VideoSaveRequest;
import top.heyqing.aether.model.vo.VideoAdminVO;
import top.heyqing.aether.service.video.VideoAdminService;

/**
 * 视频管理接口（BackEnd-Plan §5.2 /admin/videos，需 Access Token）
 */
@RestController
@RequestMapping("/v1/admin/videos")
public class AdminVideoController {

    private final VideoAdminService videoAdminService;

    public AdminVideoController(VideoAdminService videoAdminService) {
        this.videoAdminService = videoAdminService;
    }

    /**
     * 管理端分页列表（含关键时间节点与签名播放地址）
     */
    @GetMapping
    public Result<PageResult<VideoAdminVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(videoAdminService.list(page, size, keyword));
    }

    /**
     * 新建视频
     */
    @PostMapping
    @OperationLog(module = "视频", action = "新增")
    public Result<Long> create(@Valid @RequestBody VideoSaveRequest request) {
        return Result.ok(videoAdminService.create(request));
    }

    /**
     * 编辑视频
     */
    @PutMapping("/{id}")
    @OperationLog(module = "视频", action = "编辑")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody VideoSaveRequest request) {
        videoAdminService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除视频（含关键时间节点，引用归零文件进入物理清理链路）
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "视频", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        videoAdminService.delete(id);
        return Result.ok();
    }

    /**
     * 整体保存关键时间节点（提交列表即全集，空列表=清空）
     */
    @PostMapping("/{id}/chapters")
    @OperationLog(module = "视频", action = "保存时间节点")
    public Result<Void> saveChapters(@PathVariable Long id, @Valid @RequestBody VideoChapterBatchRequest request) {
        videoAdminService.saveChapters(id, request);
        return Result.ok();
    }
}
