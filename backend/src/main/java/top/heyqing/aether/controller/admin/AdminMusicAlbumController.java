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
import top.heyqing.aether.model.dto.MusicAlbumSaveRequest;
import top.heyqing.aether.model.vo.MusicAlbumAdminVO;
import top.heyqing.aether.service.music.MusicAdminService;

/**
 * 音乐合集管理接口（BackEnd-Plan §5.2 /admin/music/albums，需 Access Token）
 */
@RestController
@RequestMapping("/v1/admin/music/albums")
public class AdminMusicAlbumController {

    private final MusicAdminService musicAdminService;

    public AdminMusicAlbumController(MusicAdminService musicAdminService) {
        this.musicAdminService = musicAdminService;
    }

    /**
     * 管理端合集分页列表
     */
    @GetMapping
    public Result<PageResult<MusicAlbumAdminVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(musicAdminService.albumList(page, size, keyword));
    }

    /**
     * 新建合集
     */
    @PostMapping
    @OperationLog(module = "音乐", action = "新增合集")
    public Result<Long> create(@Valid @RequestBody MusicAlbumSaveRequest request) {
        return Result.ok(musicAdminService.albumCreate(request));
    }

    /**
     * 编辑合集
     */
    @PutMapping("/{id}")
    @OperationLog(module = "音乐", action = "编辑合集")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MusicAlbumSaveRequest request) {
        musicAdminService.albumUpdate(id, request);
        return Result.ok();
    }

    /**
     * 删除合集（含曲目时拒绝）
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "音乐", action = "删除合集")
    public Result<Void> delete(@PathVariable Long id) {
        musicAdminService.albumDelete(id);
        return Result.ok();
    }
}
