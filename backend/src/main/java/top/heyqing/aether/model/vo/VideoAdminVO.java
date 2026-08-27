package top.heyqing.aether.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频管理 VO（BackEnd-Plan §5.2 /admin/videos，比公开 VO 多文件 ID 与节点）
 *
 * @param id          视频 ID
 * @param title       标题
 * @param intro       介绍
 * @param coverFileId 封面文件 ID（可空）
 * @param coverUrl    封面签名 URL（可空）
 * @param fileId      视频文件 ID
 * @param playUrl     播放签名 URL
 * @param duration    时长（秒）
 * @param isRecommend 是否推荐
 * @param createTime  创建时间
 * @param updateTime  更新时间
 * @param chapters    关键时间节点
 */
public record VideoAdminVO(Long id, String title, String intro, Long coverFileId, String coverUrl,
                           Long fileId, String playUrl, Integer duration, Integer isRecommend,
                           LocalDateTime createTime, LocalDateTime updateTime,
                           List<VideoChapterVO> chapters) {
}
