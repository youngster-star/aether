package top.heyqing.aether.model.vo;

import java.util.List;

/**
 * 书籍详情 VO（BackEnd-Plan §5.2 GET /books/{id}，含章节目录树）
 *
 * @param id             书籍 ID
 * @param title          书名
 * @param author         作者
 * @param coverUrl       封面签名 URL（可空）
 * @param intro          简介
 * @param ownershipType  版权归属：1 本人 2 他人出版（阅读器版权页文案依据）
 * @param totalChapters  总章节数
 * @param isRecommend    是否推荐
 * @param categories     分类名列表
 * @param tags           标签名列表
 * @param chapters       章节目录树（章-节两级）
 */
public record BookDetailVO(
        Long id,
        String title,
        String author,
        String coverUrl,
        String intro,
        Integer ownershipType,
        Integer totalChapters,
        Integer isRecommend,
        List<String> categories,
        List<String> tags,
        List<ChapterNodeVO> chapters) {
}
