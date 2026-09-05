package top.heyqing.aether.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 书籍启发式分章降级器（BackEnd-Plan §7.2，与 python-agent 启发式同构）
 *
 * <p>python-agent 不可达/超时时的 Java 本地兜底（阶段 5 完成标准：断网降级可用）。
 * 规则：章节标题正则（第X章/节/回/卷/部/篇、序章/楔子/引子/尾声/后记/番外、
 * Chapter N，行长约束防正文误判）；段落按空行分组（组内行以空格连接合并为段）。</p>
 */
public final class SplitHeuristics {

    private SplitHeuristics() {
    }

    /** level：章 */
    public static final int LEVEL_CHAPTER = 1;

    /** level：节 */
    public static final int LEVEL_SECTION = 2;

    /**
     * 章标题（第X章/回/卷/部/篇 或 序章/楔子 等，或英文 Chapter N）→ level 1
     */
    private static final Pattern CHAPTER_TITLE = Pattern.compile(
            "^(第[0-9一二三四五六七八九十百千零〇两]+[章节回卷部篇]).*"
                    + "|^(序章|序言|序|自序|前言|楔子|引子|尾声|后记|跋|番外).{0,40}"
                    + "|^(Chapter|CHAPTER)\\s+\\d+.*");

    /**
     * 节标题（第X节，配合章节两级结构）→ level 2
     */
    private static final Pattern SECTION_TITLE = Pattern.compile(
            "^(第[0-9一二三四五六七八九十百千零〇两]+节).{0,40}");

    /** 标题行最大长度（超过视为正文引用"第X章"字样，不切分） */
    private static final int MAX_TITLE_LENGTH = 50;

    /**
     * 单章建议（与 python-agent 输出结构对齐）
     */
    public record ChapterDraft(String title, int level, List<String> paragraphs) {
    }

    /**
     * 对全文执行启发式分章
     *
     * @param text 全文（已按 UTF-8 解码并去除 BOM）
     * @return 章节列表（至少一章）
     */
    public static List<ChapterDraft> split(String text) {
        List<ChapterDraft> chapters = new ArrayList<>();
        // 当前章标题/层级/段落缓冲
        String currentTitle = null;
        int currentLevel = LEVEL_CHAPTER;
        List<String> currentParagraphs = new ArrayList<>();
        // 当前段落行缓冲（空行即落段）
        List<String> lineBuffer = new ArrayList<>();

        for (String rawLine : text.split("\r\n|\n|\r", -1)) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                // 空行：落段
                flushParagraph(lineBuffer, currentParagraphs);
                continue;
            }
            Integer level = matchTitleLevel(line);
            if (level != null) {
                // 标题行：先落段、再收章、开新章
                flushParagraph(lineBuffer, currentParagraphs);
                if (currentTitle != null) {
                    chapters.add(new ChapterDraft(currentTitle, currentLevel, currentParagraphs));
                    currentParagraphs = new ArrayList<>();
                }
                currentTitle = line;
                currentLevel = level;
                continue;
            }
            lineBuffer.add(line);
        }
        // 收尾：落段 + 收最后一章
        flushParagraph(lineBuffer, currentParagraphs);
        if (currentTitle != null) {
            chapters.add(new ChapterDraft(currentTitle, currentLevel, currentParagraphs));
        } else if (chapters.isEmpty()) {
            // 无任何标题：全文单章兜底（确认时可编辑拆分）
            chapters.add(new ChapterDraft("全文", LEVEL_CHAPTER, currentParagraphs));
        }
        return chapters;
    }

    /**
     * 标题行判定（返回层级；非标题返回 null）
     */
    private static Integer matchTitleLevel(String line) {
        if (line.length() > MAX_TITLE_LENGTH) {
            return null;
        }
        Matcher section = SECTION_TITLE.matcher(line);
        if (section.matches()) {
            return LEVEL_SECTION;
        }
        Matcher chapter = CHAPTER_TITLE.matcher(line);
        if (chapter.matches()) {
            return LEVEL_CHAPTER;
        }
        return null;
    }

    /**
     * 行缓冲落段：组内行以空格连接为一段（空缓冲忽略）
     */
    private static void flushParagraph(List<String> lineBuffer, List<String> paragraphs) {
        if (!lineBuffer.isEmpty()) {
            paragraphs.add(String.join(" ", lineBuffer));
            lineBuffer.clear();
        }
    }
}
