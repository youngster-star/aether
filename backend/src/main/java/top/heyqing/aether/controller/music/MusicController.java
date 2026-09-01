package top.heyqing.aether.controller.music;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.common.Result;
import top.heyqing.aether.model.vo.MusicAlbumDetailVO;
import top.heyqing.aether.model.vo.MusicAlbumListVO;
import top.heyqing.aether.model.vo.MusicDetailVO;
import top.heyqing.aether.model.vo.MusicListVO;
import top.heyqing.aether.service.music.MusicService;

/**
 * 音乐公开接口（BackEnd-Plan §5.2 音乐 music 公开）
 */
@RestController
@RequestMapping("/v1/music")
public class MusicController {

    private final MusicService musicService;

    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    /**
     * 合集分页列表（type=1 自定义合集 / 2 固定合集，可空=全部）
     */
    @GetMapping("/albums")
    public Result<PageResult<MusicAlbumListVO>> albums(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return Result.ok(musicService.pageAlbums(type, page, size));
    }

    /**
     * 合集详情（含曲目列表）
     */
    @GetMapping("/albums/{id}")
    public Result<MusicAlbumDetailVO> albumDetail(@PathVariable Long id) {
        return Result.ok(musicService.albumDetail(id));
    }

    /**
     * 单曲搜索（keyword 匹配歌名/歌手；albumId 限定合集；仅音乐可搜索，§5.2）
     */
    @GetMapping
    public Result<PageResult<MusicListVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long albumId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return Result.ok(musicService.search(keyword, albumId, page, size));
    }

    /**
     * 推荐合集（主页推荐位，limit 3-4）
     */
    @GetMapping("/recommend")
    public Result<List<MusicAlbumListVO>> recommend(@RequestParam(defaultValue = "4") int limit) {
        return Result.ok(musicService.recommend(limit));
    }

    /**
     * 单曲详情（含歌词、时长、EffectConfig 原始 JSON）
     */
    @GetMapping("/{id}")
    public Result<MusicDetailVO> detail(@PathVariable Long id) {
        return Result.ok(musicService.detail(id));
    }
}
