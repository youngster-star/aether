package top.heyqing.aether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 音乐单曲保存请求（BackEnd-Plan §5.2 /admin/music）
 *
 * <p>时长默认取 storage_file 探测值（上传合并后自动探测，§8.2），
 * 探测失败时以 duration 手工补录；effectConfig/effectSource 用于管理端
 * 手工调参落库（Schema 校验通过才保存，§7.3）。</p>
 *
 * @param title        歌曲名称
 * @param artist       歌手（可空）
 * @param albumId      所属合集 ID（可空=独立单曲）
 * @param coverFileId  封面文件 ID（可空）
 * @param fileId       音频文件 ID（storage_file）
 * @param lyricText    歌词文本（LRC/纯文本，可空）
 * @param lyricOffset  歌词全局偏移（毫秒，正负可调）
 * @param duration     时长（秒；探测失败手工补录用）
 * @param effectConfig 特效配置原始 JSON（可空=不改/不设置）
 * @param effectSource 特效来源（与 effectConfig 成对提交：2 手工调整）
 * @param isRecommend  是否推荐：1 是 0 否
 */
public record MusicSaveRequest(
        @NotBlank(message = "歌曲名称不能为空") @Size(max = 200, message = "名称最长 200 字")
        String title,
        @Size(max = 100, message = "歌手最长 100 字")
        String artist,
        Long albumId,
        Long coverFileId,
        @NotNull(message = "音频文件 ID 不能为空")
        Long fileId,
        String lyricText,
        Integer lyricOffset,
        @PositiveOrZero(message = "时长不能为负数")
        Integer duration,
        String effectConfig,
        Integer effectSource,
        Integer isRecommend) {
}
