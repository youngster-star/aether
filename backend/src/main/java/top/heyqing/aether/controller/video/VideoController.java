package top.heyqing.aether.controller.video;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.common.Result;
import top.heyqing.aether.model.vo.VideoDetailVO;
import top.heyqing.aether.model.vo.VideoListVO;
import top.heyqing.aether.service.video.VideoService;

/**
 * 视频公开接口（BackEnd-Plan §5.2 视频 video 公开）
 */
@RestController
@RequestMapping("/v1/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    /**
     * 视频分页列表（keyword 搜索标题/介绍）
     */
    @GetMapping
    public Result<PageResult<VideoListVO>> videos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(videoService.pageList(page, size, keyword));
    }

    /**
     * 视频详情（含时长、播放签名 URL、关键时间节点）
     */
    @GetMapping("/{id}")
    public Result<VideoDetailVO> detail(@PathVariable Long id) {
        return Result.ok(videoService.detail(id));
    }
}
