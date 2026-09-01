package top.heyqing.aether.model.vo;

import java.time.LocalDateTime;

/**
 * 音乐单曲管理 VO（BackEnd-Plan §5.2 /admin/music，管理端编辑回显）
 *
 * @param id           单曲 ID
 * @param title        歌曲名称
 * @param artist       歌手
 * @param albumId      所属合集 ID
 * @param coverFileId  封面文件 ID
 * @param coverUrl     封面签名 URL
 * @param fileId       音频文件 ID
 * @param fileUrl      音频签名 URL（试听）
 * @param lyricText    歌词文本（编辑器回显）
 * @param lyricOffset  歌词全局偏移（毫秒）
 * @param duration     时长（秒）
 * @param effectConfig 特效配置原始 JSON（可空）
 * @param effectSource 特效来源：1 生成 2 手工调整
 * @param isRecommend  是否推荐
 * @param createTime   创建时间
 * @param updateTime   更新时间
 */
public record MusicAdminVO(
        Long id,
        String title,
        String artist,
        Long albumId,
        Long coverFileId,
        String coverUrl,
        Long fileId,
        String fileUrl,
        String lyricText,
        Integer lyricOffset,
        Integer duration,
        String effectConfig,
        Integer effectSource,
        Integer isRecommend,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
