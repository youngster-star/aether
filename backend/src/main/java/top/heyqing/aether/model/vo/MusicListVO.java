package top.heyqing.aether.model.vo;

/**
 * 音乐单曲列表 VO（合集详情曲目/搜索结果/推荐通用，BackEnd-Plan §5.2）
 *
 * @param id       单曲 ID
 * @param title    歌曲名称
 * @param artist   歌手（可空）
 * @param coverUrl 封面签名 URL（可空=无封面）
 * @param fileUrl  音频签名 URL（§4.4 防直链；前端播放队列构建依赖）
 * @param duration 时长（秒）
 * @param albumId  所属合集 ID（可空=独立单曲）
 */
public record MusicListVO(
        Long id,
        String title,
        String artist,
        String coverUrl,
        String fileUrl,
        Integer duration,
        Long albumId) {
}
