package top.heyqing.aether.model.vo;

/**
 * 音乐合集列表 VO（BackEnd-Plan §5.2 GET /music/albums）
 *
 * @param id            合集 ID
 * @param title         合集标题
 * @param coverUrl      封面签名 URL（可空=无封面）
 * @param intro         合集介绍
 * @param type          类型：1 自定义合集 2 固定合集
 * @param certification 认证信息（固定合集展示，UI-Plan §6.6）
 * @param trackCount    曲目数（group by 批量统计）
 */
public record MusicAlbumListVO(
        Long id,
        String title,
        String coverUrl,
        String intro,
        Integer type,
        String certification,
        Long trackCount) {
}
