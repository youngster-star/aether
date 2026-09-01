package top.heyqing.aether.model.vo;

import java.util.List;

/**
 * 音乐合集详情 VO（BackEnd-Plan §5.2 GET /music/albums/{id}，含曲目列表）
 *
 * @param id            合集 ID
 * @param title         合集标题
 * @param coverUrl      封面签名 URL（可空）
 * @param intro         合集介绍
 * @param type          类型：1 自定义合集 2 固定合集
 * @param certification 认证信息（固定合集必填展示）
 * @param tracks        曲目列表（id 升序）
 */
public record MusicAlbumDetailVO(
        Long id,
        String title,
        String coverUrl,
        String intro,
        Integer type,
        String certification,
        List<MusicListVO> tracks) {
}
