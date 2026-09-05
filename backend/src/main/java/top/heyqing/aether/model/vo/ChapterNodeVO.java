package top.heyqing.aether.model.vo;

import java.util.List;

/**
 * 章节目录树节点 VO（书籍详情章节目录，UI-Plan §6.7 章-节两级树）
 *
 * @param id        章节 ID
 * @param title     章节标题
 * @param level     层级：1 章 2 节
 * @param orderNo   全书排序号
 * @param wordCount 字数
 * @param children  子节列表（仅章有）
 */
public record ChapterNodeVO(
        Long id,
        String title,
        Integer level,
        Integer orderNo,
        Integer wordCount,
        List<ChapterNodeVO> children) {
}
