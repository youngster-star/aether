package top.heyqing.aether.model.vo;

import java.time.LocalDateTime;

/**
 * 书籍管理端列表/详情项 VO（GET /admin/books）
 *
 * @param id             书籍 ID
 * @param title          书名
 * @param author         作者
 * @param coverUrl       封面签名 URL（可空）
 * @param intro          简介
 * @param ownershipType  版权归属：1 本人 2 他人出版
 * @param sourceFileId   源文件 ID（txt，可空）
 * @param totalChapters  总章节数
 * @param isRecommend    是否推荐
 * @param createTime     创建时间
 */
public record BookAdminVO(
        Long id,
        String title,
        String author,
        String coverUrl,
        String intro,
        Integer ownershipType,
        Long sourceFileId,
        Integer totalChapters,
        Integer isRecommend,
        LocalDateTime createTime) {
}
