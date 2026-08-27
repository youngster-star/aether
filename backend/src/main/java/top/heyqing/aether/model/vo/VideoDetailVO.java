package top.heyqing.aether.model.vo;

import java.util.List;

/**
 * 视频详情 VO（BackEnd-Plan §5.2 GET /videos/{id}）
 *
 * @param id      视频 ID
 * @param title   标题
 * @param coverUrl 封面签名 URL（可空）
 * @param intro   介绍
 * @param duration 时长（秒）
 * @param playUrl 播放签名 URL（ArtPlayer 源地址）
 * @param chapters 关键时间节点（按 sort 排序）
 */
public record VideoDetailVO(Long id, String title, String coverUrl, String intro,
                            Integer duration, String playUrl, List<VideoChapterVO> chapters) {
}
