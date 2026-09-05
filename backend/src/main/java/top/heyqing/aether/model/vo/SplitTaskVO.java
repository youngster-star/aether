package top.heyqing.aether.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分章任务状态 VO（BackEnd-Plan §5.2 GET /admin/books/{id}/split-task）
 *
 * <p>aiResult 为分章建议结构化对象（管理端预览编辑器直接渲染/编辑后提交确认）。</p>
 *
 * @param taskId      任务 ID
 * @param bookId      书籍 ID
 * @param status      状态：1 解析中 2 待确认 3 已完成 4 失败
 * @param source      分章来源：agent（python-agent）/ heuristic（Java 降级）
 * @param aiResult    分章建议（chapters[{title,level,paragraphs[]}，status=2 时）
 * @param failReason  失败原因（status=4 时）
 * @param confirmTime 确认时间（status=3 时）
 */
public record SplitTaskVO(
        Long taskId,
        Long bookId,
        Integer status,
        String source,
        SplitChaptersVO aiResult,
        String failReason,
        LocalDateTime confirmTime) {

    /**
     * 分章建议结构（ai_result JSON 的 Java 视图）
     */
    public record SplitChaptersVO(List<ChapterDraftVO> chapters, String source) {
    }

    /**
     * 单章建议（管理端可编辑：标题/层级/段落）
     */
    public record ChapterDraftVO(String title, Integer level, List<String> paragraphs) {
    }
}
