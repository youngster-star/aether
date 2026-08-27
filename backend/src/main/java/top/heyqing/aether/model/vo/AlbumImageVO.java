package top.heyqing.aether.model.vo;

/**
 * 图集图片 VO（图集详情内嵌，BackEnd-Plan §5.2 GET /albums/{id}）
 *
 * <p>宽高/大小来自 storage_file 元数据（预览层悬浮信息展示用，UI-Plan §6.4）。</p>
 *
 * @param id     图片记录 ID
 * @param title  标题（可空）
 * @param intro  介绍（可空）
 * @param url    签名访问 URL
 * @param width  图片宽度（像素，可空）
 * @param height 图片高度（像素，可空）
 * @param size   文件大小（字节）
 */
public record AlbumImageVO(Long id, String title, String intro, String url,
                           Integer width, Integer height, long size) {
}
