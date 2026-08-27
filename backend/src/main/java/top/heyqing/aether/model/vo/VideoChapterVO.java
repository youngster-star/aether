package top.heyqing.aether.model.vo;

/**
 * 视频关键时间节点 VO（视频详情内嵌，BackEnd-Plan §5.2 GET /videos/{id}）
 *
 * @param id         节点 ID
 * @param title      节点标题
 * @param timeOffset 时间偏移（秒）
 */
public record VideoChapterVO(Long id, String title, Integer timeOffset) {
}
