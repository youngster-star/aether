package top.heyqing.aether.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情视图对象（公开，BackEnd-Plan §5.2 /articles/{id}）
 *
 * <p>含 sanitize 后 HTML 与独立样式（null=前端默认样式），阅读数已按
 * IP 24 小时去重逻辑计算（去重命中时 +1）。</p>
 */
public record ArticleDetailVO(
        Long id,
        String title,
        String summary,
        String coverUrl,
        String contentHtml,
        Integer wordCount,
        Integer readingCount,
        Integer isHot,
        LocalDateTime publishTime,
        LocalDateTime updateTime,
        ArticleStyleVO style,
        List<CategoryVO> categories,
        List<TagVO> tags) {
}
