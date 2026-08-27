package top.heyqing.aether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 图集保存请求（BackEnd-Plan §5.2 /admin/albums）
 *
 * @param title       图集标题
 * @param intro       图集介绍（可空）
 * @param coverFileId 封面文件 ID（可空=无封面）
 * @param isRecommend 是否推荐：1 是 0 否
 * @param sort        推荐排序号（值大优先）
 */
public record AlbumSaveRequest(
        @NotBlank(message = "图集标题不能为空") @Size(max = 200, message = "标题最长 200 字")
        String title,
        @Size(max = 500, message = "介绍最长 500 字")
        String intro,
        Long coverFileId,
        Integer isRecommend,
        Integer sort) {
}
