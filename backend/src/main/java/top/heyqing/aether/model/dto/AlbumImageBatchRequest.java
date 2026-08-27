package top.heyqing.aether.model.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 图集图片批量添加请求（BackEnd-Plan §5.2 POST /admin/albums/{id}/images）
 *
 * @param items 图片条目（含排序/标题/介绍）
 */
public record AlbumImageBatchRequest(
        @NotEmpty(message = "图片列表不能为空")
        List<@Valid Item> items) {

    /**
     * 单张图片条目
     *
     * @param fileId 图片文件 ID（storage_file）
     * @param title  图片标题（可选）
     * @param intro  图片介绍（可选）
     * @param sort   排序号
     */
    public record Item(
            @NotNull(message = "图片文件 ID 不能为空")
            Long fileId,
            @Size(max = 200, message = "标题最长 200 字")
            String title,
            @Size(max = 500, message = "介绍最长 500 字")
            String intro,
            Integer sort) {
    }
}
