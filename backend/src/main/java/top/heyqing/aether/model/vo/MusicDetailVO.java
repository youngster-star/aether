package top.heyqing.aether.model.vo;

/**
 * 音乐单曲详情 VO（BackEnd-Plan §5.2 GET /music/{id}）
 *
 * <p>fileUrl 为签名播放地址（§4.4 防直链）；effectConfig 为 EffectConfig
 * Schema v1 原始 JSON 串（前端 MusicVisualizer 直接解析，§7.3）。</p>
 *
 * @param id           单曲 ID
 * @param title        歌曲名称
 * @param artist       歌手（可空）
 * @param albumId      所属合集 ID（可空=独立单曲）
 * @param albumTitle   合集标题（独立单曲为 null）
 * @param coverUrl     封面签名 URL（可空）
 * @param fileUrl      音频签名 URL
 * @param lyricText    歌词文本（LRC/纯文本，可空）
 * @param lyricOffset  歌词全局偏移（毫秒）
 * @param duration     时长（秒）
 * @param effectConfig 特效配置原始 JSON（可空=未生成，前端回退默认渲染）
 * @param effectSource 特效来源：1 生成 2 手工调整
 */
public record MusicDetailVO(
        Long id,
        String title,
        String artist,
        Long albumId,
        String albumTitle,
        String coverUrl,
        String fileUrl,
        String lyricText,
        Integer lyricOffset,
        Integer duration,
        String effectConfig,
        Integer effectSource) {
}
