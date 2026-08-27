package top.heyqing.aether.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图集详情 VO（BackEnd-Plan §5.2 GET /albums/{id}）
 *
 * @param id         图集 ID
 * @param title      标题
 * @param coverUrl   封面签名 URL（可空）
 * @param intro      介绍
 * @param createTime 创建时间
 * @param images     图片列表（按 sort 排序）
 */
public record AlbumDetailVO(Long id, String title, String coverUrl, String intro,
                            LocalDateTime createTime, List<AlbumImageVO> images) {
}
