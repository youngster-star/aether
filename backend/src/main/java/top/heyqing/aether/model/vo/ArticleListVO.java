package top.heyqing.aether.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表视图对象（公开，BackEnd-Plan §5.2 /articles）
 *
 * <p>coverUrl 为签名访问 URL（防直链，BackEnd-Plan §4.4）；hot 接口不返回
 * createTime，本 VO 不含创建时间字段。</p>
 */
public record ArticleListVO(
        Long id,
        String title,
        String summary,
        String coverUrl,
        Integer readingCount,
        Integer isHot,
        LocalDateTime publishTime,
        List<CategoryVO> categories,
        List<TagVO> tags) {
}
