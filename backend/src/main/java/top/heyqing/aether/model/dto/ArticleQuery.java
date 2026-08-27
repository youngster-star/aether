package top.heyqing.aether.model.dto;

/**
 * 文章列表查询参数（公开，BackEnd-Plan §5.2 /articles）
 *
 * @param page       页码（从 1 开始）
 * @param size       每页大小
 * @param categoryId 分类过滤（可空）
 * @param tagId      标签过滤（可空）
 * @param keyword    关键词（标题/简介/内容，可空）
 * @param sort       排序：latest 最新 | hot 热门
 */
public record ArticleQuery(int page, int size, Long categoryId, Long tagId, String keyword, String sort) {

    /** 页码/每页防呆（Controller 已 clamp，此处再兜底） */
    public ArticleQuery {
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 50) {
            size = 10;
        }
    }
}
