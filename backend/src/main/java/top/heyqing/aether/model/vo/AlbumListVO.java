package top.heyqing.aether.model.vo;

/**
 * 图集列表 VO（BackEnd-Plan §5.2 GET /albums）
 *
 * @param id         图集 ID
 * @param title      标题
 * @param coverUrl   封面签名 URL（可空）
 * @param intro      介绍
 * @param imageCount 图片数量
 */
public record AlbumListVO(Long id, String title, String coverUrl, String intro, long imageCount) {
}
