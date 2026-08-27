package top.heyqing.aether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 视频保存请求（BackEnd-Plan §5.2 /admin/videos）
 *
 * @param title       视频标题
 * @param intro       视频介绍（可空）
 * @param coverFileId 封面文件 ID（可空）
 * @param fileId      视频文件 ID（storage_file）
 * @param duration    时长（秒；上传后自动探测，探测失败时手工补录）
 * @param isRecommend 是否推荐：1 是 0 否
 */
public record VideoSaveRequest(
        @NotBlank(message = "视频标题不能为空") @Size(max = 200, message = "标题最长 200 字")
        String title,
        @Size(max = 500, message = "介绍最长 500 字")
        String intro,
        Long coverFileId,
        @NotNull(message = "视频文件 ID 不能为空")
        Long fileId,
        @PositiveOrZero(message = "时长不能为负数")
        Integer duration,
        Integer isRecommend) {
}
