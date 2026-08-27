package top.heyqing.aether.model.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 文章保存入参（管理端新建/编辑，BackEnd-Plan §5.2 /admin/articles）
 *
 * <p>contentHtml 由编辑器产出（md/latex 模式在前端编辑器渲染为 HTML 后提交），
 * 后端入库前统一 jsoup sanitize；contentMd 保留源文本供编辑回显。</p>
 */
public record ArticleSaveRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题最长 200 字")
        String title,

        /** 封面文件 ID（storage_file.id，可空） */
        Long coverFileId,

        @Size(max = 500, message = "简介最长 500 字")
        String summary,

        @NotBlank(message = "内容不能为空")
        String contentHtml,

        /** Markdown 源内容（编辑回显；非 md 编辑时可为空） */
        String contentMd,

        /** 分类 ID 列表 */
        List<Long> categoryIds,

        /** 标签 ID 列表 */
        List<Long> tagIds,

        /** 独立样式 ID（空=默认样式） */
        Long articleStyleId) {
}
