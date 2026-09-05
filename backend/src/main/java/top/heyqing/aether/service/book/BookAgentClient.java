package top.heyqing.aether.service.book;

import java.util.List;
import java.util.Optional;

import top.heyqing.aether.util.SplitHeuristics;

/**
 * python-agent 分章客户端（BackEnd-Plan §7.2 POST /split-book）
 *
 * <p>agent 不可达/超时/输出不合法时返回 empty（不抛异常），
 * 由 {@link BookSplitService} 降级 Java 启发式（SplitHeuristics）。</p>
 */
public interface BookAgentClient {

    /**
     * 调用分章接口（单块）
     *
     * @param text       全文分块（含与前块的 200 字符重叠窗口）
     * @param chunkIndex 块序号（从 0）
     * @param chunkTotal 总块数
     * @return 章节建议（empty=agent 不可用，走降级）
     */
    Optional<List<SplitHeuristics.ChapterDraft>> split(String text, int chunkIndex, int chunkTotal);
}
