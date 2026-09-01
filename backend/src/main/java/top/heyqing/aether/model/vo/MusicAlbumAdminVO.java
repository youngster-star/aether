package top.heyqing.aether.model.vo;

import java.time.LocalDateTime;

/**
 * 音乐合集管理 VO（BackEnd-Plan §5.2 /admin/music/albums）
 *
 * @param id            合集 ID
 * @param title         合集标题
 * @param coverFileId   封面文件 ID
 * @param coverUrl      封面签名 URL（可空）
 * @param intro         合集介绍
 * @param type          类型：1 自定义合集 2 固定合集
 * @param certification 认证信息
 * @param isRecommend   是否推荐
 * @param trackCount    曲目数
 * @param createTime    创建时间
 * @param updateTime    更新时间
 */
public record MusicAlbumAdminVO(
        Long id,
        String title,
        Long coverFileId,
        String coverUrl,
        String intro,
        Integer type,
        String certification,
        Integer isRecommend,
        Long trackCount,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
