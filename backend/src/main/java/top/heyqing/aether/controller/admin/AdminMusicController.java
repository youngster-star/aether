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
import top.heyqing.aether.model.dto.MusicSaveRequest;
import top.heyqing.aether.model.vo.MusicAdminVO;
import top.heyqing.aether.service.music.MusicAdminService;

/**
 * 音乐单曲管理接口（BackEnd-Plan §5.2 /admin/music，需 Access Token）
 */
@RestController
@RequestMapping("/v1/admin/music")
public class AdminMusicController {

    private final MusicAdminService musicAdminService;

    public AdminMusicController(MusicAdminService musicAdminService) {
        this.musicAdminService = musicAdminService;
    }

    /**
     * 管理端单曲分页列表（keyword 匹配歌名/歌手；albumId 限定合集）
     */
    @GetMapping
    public Result<PageResult<MusicAdminVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long albumId) {
        return Result.ok(musicAdminService.list(page, size, keyword, albumId));
    }

    /**
     * 新建单曲
     */
    @PostMapping
    @OperationLog(module = "音乐", action = "新增")
    public Result<Long> create(@Valid @RequestBody MusicSaveRequest request) {
        return Result.ok(musicAdminService.create(request));
    }

    /**
     * 编辑单曲（含歌词/特效配置手工调参，落库前过 Schema 校验）
     */
    @PutMapping("/{id}")
    @OperationLog(module = "音乐", action = "编辑")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MusicSaveRequest request) {
        musicAdminService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除单曲
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "音乐", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        musicAdminService.delete(id);
        return Result.ok();
    }

    /**
     * 生成播放特效（EffectConfig，§7.3；调色板提取 + Schema 校验后落库）
     */
    @PostMapping("/{id}/effect/generate")
    @OperationLog(module = "音乐", action = "生成特效")
    public Result<String> generateEffect(@PathVariable Long id) {
        return Result.ok(musicAdminService.generateEffect(id));
    }
}
