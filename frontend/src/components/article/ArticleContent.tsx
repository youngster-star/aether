"use client";

import {useEffect, useState} from "react";

import type {ArticleStyleConfig} from "@/lib/api/types";

/**
 * 文章正文渲染（UI-Plan §6.3 / §8.3）：
 * content_html 渲染 + 独立样式 style_json 转换为 CSS Variables 注入文章容器。
 *
 * <p>XSS 双保险（§9.3）：SSR 渲染后端 jsoup 已清洗的 HTML；客户端挂载后
 * DOMPurify 动态加载再清洗一次（DOMPurify 依赖浏览器环境，SSR 不可用）。</p>
 */
export default function ArticleContent({
  html,
  style,
}: {
  html: string;
  style: ArticleStyleConfig | null;
}) {
  // 客户端 DOMPurify 就绪后替换为二次清洗结果（SSR 期间用后端已 sanitize 的原文）
  const [cleanHtml, setCleanHtml] = useState<string | null>(null);
  useEffect(() => {
    let cancelled = false;
    import("dompurify").then((module) => {
      if (!cancelled) {
        setCleanHtml(
          module.default.sanitize(html, {ADD_ATTR: ["class", "target", "rel", "loading"]}),
        );
      }
    });
    return () => {
      cancelled = true;
    };
  }, [html]);

  // style_json → CSS 变量映射（UI-Plan §8.3；无样式走全局默认排版）
  const cssVars = style
    ? ({
        "--art-font": style.fontFamily,
        "--art-font-size": style.fontSize !== undefined ? `${style.fontSize}px` : undefined,
        "--art-line-height": style.lineHeight,
        "--art-para-spacing": style.paragraphSpacing !== undefined ? `${style.paragraphSpacing}px` : undefined,
        "--art-indent": style.firstLineIndent,
        "--art-width": style.contentWidth !== undefined ? `${style.contentWidth}px` : undefined,
        "--art-theme": style.themeColor,
      } as React.CSSProperties)
    : undefined;

  return (
    <article
      className="article-body mx-auto px-4"
      style={{
        ...cssVars,
        maxWidth: "var(--art-width, 720px)",
        fontFamily: "var(--art-font, var(--font-body))",
        fontSize: "var(--art-font-size, 17px)",
        lineHeight: "var(--art-line-height, 1.9)",
        color: "var(--foreground)",
      }}
      // 富文本经 sanitize 后渲染（信任白名单内容）
      dangerouslySetInnerHTML={{__html: cleanHtml ?? html}}
    />
  );
}
