package top.heyqing.aether.model.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 视频关键时间节点批量保存请求（BackEnd-Plan §5.2 POST /admin/videos/{id}/chapters）
 *
 * <p>整体替换语义：提交的列表即为视频节点全集（管理端编辑器全量保存），
 * 空列表 = 清空全部节点。</p>
 *
 * @param items 节点条目（按 sort 排序，可空）
 */
public record VideoChapterBatchRequest(
        @NotNull(message = "节点列表不能为空")
        List<@Valid Item> items) {

    /**
     * 单节点条目
     *
     * @param title      节点标题
     * @param timeOffset 时间偏移（秒）
     * @param sort       排序号
     */
    public record Item(
            @NotBlank(message = "节点标题不能为空") @Size(max = 200, message = "标题最长 200 字")
            String title,
            @NotNull(message = "时间偏移不能为空") @PositiveOrZero(message = "时间偏移不能为负数")
            Integer timeOffset,
            Integer sort) {
    }
}
