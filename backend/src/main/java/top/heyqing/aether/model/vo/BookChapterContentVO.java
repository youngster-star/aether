package top.heyqing.aether.model.vo;

/**
 * 章节内容 VO（BackEnd-Plan §5.2 GET /books/{id}/chapters/{chapterId}）
 *
 * <p>contentHtml 为分段排版 HTML（&lt;p class="indent"&gt; 段首空两格 +
 * 段间半行距 class 钩子，§7.2）；prev/next 为全书线性序导航（阅读器 ←/→ 翻页）。</p>
 *
 * @param bookId        书籍 ID
 * @param chapterId     章节 ID
 * @param title         章节标题
 * @param level         层级：1 章 2 节
 * @param orderNo       全书排序号
 * @param totalChapters 全书章节线性总数（进度计算分母）
 * @param contentHtml   排版后的 HTML 内容
 * @param prev          上一章导航（可空）
 * @param next          下一章导航（可空）
 */
public record BookChapterContentVO(
        Long bookId,
        Long chapterId,
        String title,
        Integer level,
        Integer orderNo,
        Integer totalChapters,
        String contentHtml,
        ChapterRefVO prev,
        ChapterRefVO next) {

    /**
     * 前后章导航引用
     */
    public record ChapterRefVO(Long chapterId, String title) {
    }
}
