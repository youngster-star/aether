package top.heyqing.aether.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 书籍新建/编辑请求（BackEnd-Plan §5.2 POST/PUT /admin/books）
 *
 * <p>sourceFileId 为 txt 源文件（storage_file，先走存储上传再建书籍）；
 * ownership_type 决定阅读器版权页文案。</p>
 */
public record BookSaveRequest(
        @NotBlank(message = "书名不能为空")
        @Size(max = 200, message = "书名不能超过 200 字")
        String title,

        @Size(max = 100, message = "作者不能超过 100 字")
        String author,

        Long coverFileId,

        @Size(max = 1000, message = "简介不能超过 1000 字")
        String intro,

        @NotNull(message = "版权归属不能为空")
        @Min(value = 1, message = "版权归属只能是 1 本人 / 2 他人出版")
        @Max(value = 2, message = "版权归属只能是 1 本人 / 2 他人出版")
        Integer ownershipType,

        Long sourceFileId,

        @Min(value = 0, message = "是否推荐只能是 0 否 / 1 是")
        @Max(value = 1, message = "是否推荐只能是 0 否 / 1 是")
        Integer isRecommend) {
}
