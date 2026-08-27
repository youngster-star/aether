package top.heyqing.aether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 标签保存入参（全站字典管理，BackEnd-Plan §5.2 /admin/tags）
 */
public record TagSaveRequest(
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 50, message = "标签名称最长 50 字")
        String name,

        @NotBlank(message = "唯一标识不能为空")
        @Size(max = 80, message = "唯一标识最长 80 字")
        String slug,

        /** 业务域：article/book/music/album/video/announcement */
        @NotBlank(message = "业务域不能为空")
        @Size(max = 20, message = "业务域最长 20 字")
        String bizType) {
}
