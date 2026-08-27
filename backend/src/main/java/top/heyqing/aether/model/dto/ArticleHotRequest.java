package top.heyqing.aether.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 文章热度设置入参（BackEnd-Plan §5.2 PUT /admin/articles/{id}/hot）
 */
public record ArticleHotRequest(
        @NotNull(message = "isHot 不能为空")
        Integer isHot,

        @NotNull(message = "hotOrder 不能为空")
        Integer hotOrder) {
}
