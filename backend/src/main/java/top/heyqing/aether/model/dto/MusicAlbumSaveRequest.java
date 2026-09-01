package top.heyqing.aether.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 音乐合集保存请求（BackEnd-Plan §5.2 /admin/music/albums）
 *
 * @param title         合集标题
 * @param coverFileId   封面文件 ID（可空）
 * @param intro         合集介绍（可空）
 * @param type          类型：1 自定义合集 2 固定合集
 * @param certification 认证信息（type=2 必填，如发行方/认证编号）
 * @param isRecommend   是否推荐：1 是 0 否
 */
public record MusicAlbumSaveRequest(
        @NotBlank(message = "合集标题不能为空") @Size(max = 200, message = "标题最长 200 字")
        String title,
        Long coverFileId,
        @Size(max = 500, message = "介绍最长 500 字")
        String intro,
        @NotNull(message = "合集类型不能为空")
        @Min(value = 1, message = "合集类型非法") @Max(value = 2, message = "合集类型非法")
        Integer type,
        @Size(max = 200, message = "认证信息最长 200 字")
        String certification,
        Integer isRecommend) {
}
