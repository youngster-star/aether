package top.heyqing.aether.service.video;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.model.vo.VideoDetailVO;
import top.heyqing.aether.model.vo.VideoListVO;

/**
 * 视频公开服务（BackEnd-Plan §5.2 视频 video 公开）
 */
public interface VideoService {

    /**
     * 视频分页列表（keyword 搜索标题/介绍）
     */
    PageResult<VideoListVO> pageList(int page, int size, String keyword);

    /**
     * 视频详情（含播放签名 URL 与关键时间节点）
     */
    VideoDetailVO detail(Long id);
}
