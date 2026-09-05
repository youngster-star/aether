package top.heyqing.aether.model.vo;

import java.util.List;

/**
 * 书籍列表项 VO（BackEnd-Plan §5.2 GET /books / GET /books/recommend）
 *
 * @param id          书籍 ID
 * @param title       书名
 * @param author      作者
 * @param coverUrl    封面签名 URL（可空）
 * @param intro       一句话简介
 * @param totalChapters 总章节数
 * @param isRecommend 是否推荐
 * @param tags        标签名列表（批量关联查询，防 N+1）
 */
public record BookListVO(
        Long id,
        String title,
        String author,
        String coverUrl,
        String intro,
        Integer totalChapters,
        Integer isRecommend,
        List<String> tags) {
}
