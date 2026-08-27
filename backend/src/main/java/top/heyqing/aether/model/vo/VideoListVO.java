package top.heyqing.aether.model.vo;

/**
 * 视频列表 VO（BackEnd-Plan §5.2 GET /videos）
 *
 * @param id      视频 ID
 * @param title   标题
 * @param coverUrl 封面签名 URL（可空）
 * @param intro   介绍
 * @param duration 时长（秒）
 */
public record VideoListVO(Long id, String title, String coverUrl, String intro, Integer duration) {
}
