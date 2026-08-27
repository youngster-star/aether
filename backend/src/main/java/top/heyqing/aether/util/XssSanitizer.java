package top.heyqing.aether.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * 富文本 XSS sanitize 工具（BackEnd-Plan §4.3）
 *
 * <p>文章/公告/书籍内容入库前经 jsoup 白名单过滤：保留排版标签与 class（代码高亮、
 * 排版依赖），剥除 script/style 与全部事件属性；前端渲染再经 DOMPurify 双保险。</p>
 */
public final class XssSanitizer {

    private XssSanitizer() {
    }

    /**
     * 内容白名单（relaxed 基础上扩展：保留 class 便于排版与代码高亮）
     */
    private static final Safelist CONTENT_SAFELIST = Safelist.relaxed()
            // 全标签保留 class（语言标注/排版钩子；class 本身不含脚本语义）
            .addAttributes(":all", "class")
            // 链接外链安全：target/rel
            .addAttributes("a", "target", "rel")
            // 图片懒加载与引用说明
            .addAttributes("img", "loading", "referrerpolicy")
            // 代码块/引用相关补充标签（relaxed 已含 pre/code，figure 系列补上）
            .addTags("figure", "figcaption", "mark", "s", "kbd", "hr")
            .preserveRelativeLinks(true);

    /**
     * 白名单过滤 HTML
     *
     * @param html 原始 HTML（可为 null，返回 null）
     * @return sanitize 后 HTML（关闭 prettyPrint，避免 prettify 在行间注入换行）
     */
    public static String clean(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, "", CONTENT_SAFELIST,
                new Document.OutputSettings().prettyPrint(false));
    }
}
