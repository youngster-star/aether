package top.heyqing.aether.controller.album;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.common.Result;
import top.heyqing.aether.model.vo.AlbumDetailVO;
import top.heyqing.aether.model.vo.AlbumListVO;
import top.heyqing.aether.service.album.AlbumService;

/**
 * 图集公开接口（BackEnd-Plan §5.2 图集 album 公开）
 */
@RestController
@RequestMapping("/v1/albums")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    /**
     * 图集分页列表（keyword 搜索标题/介绍）
     */
    @GetMapping
    public Result<PageResult<AlbumListVO>> albums(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(albumService.pageList(page, size, keyword));
    }

    /**
     * 推荐图集（主页推荐区块，limit 3-4）
     */
    @GetMapping("/recommend")
    public Result<List<AlbumListVO>> recommend(@RequestParam(defaultValue = "4") int limit) {
        return Result.ok(albumService.recommend(limit));
    }

    /**
     * 图集详情（含图片列表：签名 URL、标题、介绍、宽高、大小）
     */
    @GetMapping("/{id}")
    public Result<AlbumDetailVO> detail(@PathVariable Long id) {
        return Result.ok(albumService.detail(id));
    }
}
