"use client";

import {AnimatePresence, motion} from "framer-motion";
import {useTranslations} from "next-intl";
import {useEffect, useState} from "react";

import type {ArticleListVO} from "@/lib/api/types";
import {Link} from "@/i18n/navigation";
import SideTab from "@/components/layout/SideTab";

/**
 * RELATED ARTICLES（UI-Plan §8.2 v2.0，按参考站实测行为重做）：
 *
 * <p>默认态：桌面端（≥lg）右缘竖排跳动按钮（SideTab，"查看更多文章"）。
 * 打开态：主内容区覆盖淡分区色层（T3 整区换色）——左侧滑出白色列表面板
 * （相关文章卡片列表，面板头为引导语"或许你还想读："）；关闭按钮固定在
 * 顶部最右侧（与右缘竖排按钮同侧呼应）；ESC/点击遮罩/X 关闭。
 * 移动端（<lg）不渲染按钮，由正文底部 RelatedInline 降级。</p>
 */
export default function RelatedPanel({articles}: {articles: ArticleListVO[]}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);

  // ESC 关闭（§8.2）
  useEffect(() => {
    if (!open) {
      return;
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  if (articles.length === 0) {
    return null;
  }
  return (
    <>
      {/* 右缘竖排跳动按钮：打开时文字轮换为"收起"（C4） */}
      <SideTab
        label={t("articles.related")}
        labelOpen={t("articles.relatedClose")}
        open={open}
        onToggle={() => setOpen((value) => !value)}
      />

      {/* 打开态：实色羊皮纸底覆盖层（完全遮住原文章，非半透明虚化）
          + 左侧白色列表面板（仅此一处推荐，去重复）；点空白处关闭 */}
      <AnimatePresence>
        {open && (
          <motion.div
            className="fixed inset-0 z-[70]"
            style={{
              background: "color-mix(in srgb, var(--tint) 6%, var(--background))",
            }}
            initial={{opacity: 0}}
            animate={{opacity: 1}}
            exit={{opacity: 0}}
            transition={{duration: 0.3, ease: [0.22, 1, 0.36, 1]}}
            onClick={() => setOpen(false)}
          >
            {/* 关闭按钮：顶部最右侧（与竖排按钮同侧，站长指定） */}
            <button
              type="button"
              aria-label={t("common.close")}
              onClick={() => setOpen(false)}
              className="absolute right-5 top-5 z-[80] flex h-10 w-10 items-center justify-center
                         rounded-pill border border-border bg-card text-xl shadow-aether
                         transition-colors hover:bg-accent hover:text-background"
            >
              ×
            </button>

            {/* 左侧白色列表面板（约 1/3 视口宽，参考 article-04；stopPropagation 防误触遮罩关闭） */}
            <motion.aside
              className="flex h-full w-full max-w-md flex-col border-r border-border bg-card shadow-aether"
              initial={{x: "-100%"}}
              animate={{x: 0}}
              exit={{x: "-100%"}}
              transition={{duration: 0.5, ease: [0.22, 1, 0.36, 1]}}
              onClick={(event) => event.stopPropagation()}
            >
              <div className="border-b border-border px-5 py-4">
                <h2 className="font-display text-lg font-black">{t("articles.relatedLead")}</h2>
              </div>
              <nav className="flex-1 overflow-y-auto p-4">
                {articles.map((article, index) => (
                  <motion.div
                    key={article.id}
                    initial={{opacity: 0, y: 12}}
                    animate={{opacity: 1, y: 0}}
                    transition={{delay: 0.15 + index * 0.06, duration: 0.35, ease: [0.22, 1, 0.36, 1]}}
                  >
                    <RelatedCard article={article} onNavigate={() => setOpen(false)} />
                  </motion.div>
                ))}
              </nav>
            </motion.aside>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}

/**
 * 相关文章卡片：hover 整卡反色（§2.7 T2）
 */
function RelatedCard({article, onNavigate}: {article: ArticleListVO; onNavigate: () => void}) {
  const t = useTranslations();
  return (
    <Link href={`/articles/${article.id}`} onClick={onNavigate} className="block">
      <article className="hover-card group block rounded-lg border border-border p-4">
        <p className="font-display text-sm font-bold leading-snug">{article.title}</p>
        <p className="hover-card-dim mt-1 text-xs opacity-70">
          {article.publishTime?.slice(0, 10)} · {t("common.viewCount", {count: article.readingCount})}
        </p>
      </article>
    </Link>
  );
}
