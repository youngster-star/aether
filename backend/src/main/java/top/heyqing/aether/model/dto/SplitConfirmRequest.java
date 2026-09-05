package top.heyqing.aether.model.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 分章确认请求（BackEnd-Plan §5.2 POST /admin/books/{id}/chapters/confirm）
 *
 * <p>管理端预览编辑器可增删改/合并拆分后提交；后端按 level 挂树（节挂最近章）
 * 生成排版 HTML 落库 book_chapter（§7.2），并更新 book.total_chapters、task.status=3。</p>
 */
public record SplitConfirmRequest(
        @Valid
        @NotEmpty(message = "章节列表不能为空")
        List<ChapterConfirm> chapters) {

    /**
     * 单章确认（编辑后的分章结构）
     */
    public record ChapterConfirm(
            @NotBlank(message = "章节标题不能为空")
            @Size(max = 200, message = "章节标题不能超过 200 字")
            String title,

            @NotNull(message = "层级不能为空")
            @Min(value = 1, message = "层级只能是 1 章 / 2 节")
            @Max(value = 2, message = "层级只能是 1 章 / 2 节")
            Integer level,

            @NotEmpty(message = "章节段落不能为空")
            List<@NotBlank(message = "段落不能为空白") @Size(max = 20000, message = "单段不能超过 2 万字符") String> paragraphs) {
    }
}
