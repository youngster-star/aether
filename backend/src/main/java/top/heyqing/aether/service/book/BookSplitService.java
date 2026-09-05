package top.heyqing.aether.service.book;

import top.heyqing.aether.model.dto.SplitConfirmRequest;
import top.heyqing.aether.model.vo.SplitTaskVO;

/**
 * 书籍分章服务（BackEnd-Plan §7.2 状态机：解析中 → 待确认 → 已完成/失败）
 *
 * <p>流程：trigger 建 status=1 任务并异步分块调 python-agent（不可达降级 Java
 * 启发式）→ 汇总写 ai_result、status=2 → 管理端预览编辑 → confirm 排版落库、
 * status=3。失败写 status=4 + fail_reason，可重试。</p>
 */
public interface BookSplitService {

    /**
     * 触发分章（异步执行）
     *
     * <p>状态约束：status=1 拒绝（解析中）；status=3 拒绝（已完成，需重新上传源
     * 文件）；status=2/4 允许（覆盖重跑/重试）。</p>
     *
     * @param bookId 书籍 ID
     * @return 任务 ID
     */
    Long trigger(Long bookId);

    /**
     * 查询最近任务状态与分章建议
     *
     * @param bookId 书籍 ID
     * @return 任务 VO（aiResult 为结构化建议，管理端直接渲染）
     */
    SplitTaskVO task(Long bookId);

    /**
     * 确认分章并落库（可编辑后提交）
     *
     * <p>前置：最近任务 status=2（否则 30404）；生成排版 HTML
     * （段首空两格 p.indent + 插图标记位 figure-mark）写 book_chapter，
     * 更新 total_chapters，task → status=3。</p>
     *
     * @param bookId  书籍 ID
     * @param request 编辑后的分章结构
     */
    void confirm(Long bookId, SplitConfirmRequest request);
}
