package top.heyqing.aether.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端文章视图对象（含草稿与编辑回显字段，BackEnd-Plan §5.2 /admin/articles）
 */
public record ArticleAdminVO(
        Long id,
        String title,
        Long coverFileId,
        String coverUrl,
        String summary,
        String contentHtml,
        String contentMd,
        Integer wordCount,
        Integer readingCount,
        Integer isHot,
        Integer hotOrder,
        Long articleStyleId,
        Integer isPublished,
        LocalDateTime publishTime,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<Long> categoryIds,
        List<Long> tagIds) {
}
