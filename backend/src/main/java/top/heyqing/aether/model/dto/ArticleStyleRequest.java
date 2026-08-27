package top.heyqing.aether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 文章样式保存入参（BackEnd-Plan §5.2 /admin/article-styles）
 *
 * <p>styleJson 结构见 BackEnd-Plan §6.3（CSS 变量集 + 排版参数）。</p>
 */
public record ArticleStyleRequest(
        @NotBlank(message = "样式名称不能为空")
        @Size(max = 50, message = "样式名称最长 50 字")
        String name,

        @NotBlank(message = "样式配置不能为空")
        String styleJson,

        /** 是否默认样式：1 是 0 否（全站仅一个默认） */
        Integer isDefault) {
}
