"use client";

import {AnimatePresence, motion} from "framer-motion";
import {useTranslations} from "next-intl";
import {useState} from "react";

import type {ArticleListVO} from "@/lib/api/types";
import {Link} from "@/i18n/navigation";

/**
 * RELATED ARTICLES（UI-Plan §8.2）：
 * 桌面端（≥lg）屏幕右侧竖排透明文字按钮（vertical-rl + 文字轮换），
 * 点击滑出右侧抽屉（相关文章列表）；移动端降级为正文底部推荐区块。
 */
export default function RelatedPanel({articles}: {articles: ArticleListVO[]}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);

  if (articles.length === 0) {
    return null;
  }
  return (
    <>
      {/* 桌面端：右侧竖排按钮（<lg 隐藏） */}
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label={t("articles.related")}
        className="vertical-text fixed right-3 top-1/2 z-40 hidden -translate-y-1/2 rounded-pill
                   border border-border bg-card/80 px-1.5 py-6 font-display text-sm font-bold
                   tracking-widest text-accent opacity-60 backdrop-blur-sm transition-all
                   hover:bg-accent hover:text-background hover:opacity-100 lg:block"
        style={{transition: "all 300ms cubic-bezier(0.22,1,0.36,1)"}}
      >
        {/* 竖排文字（morphing-text 轮换词条属可选增强，此处固定词条） */}
        {t("articles.related")}
      </button>

      {/* 右侧抽屉 */}
      <AnimatePresence>
        {open && (
          <>
            <motion.div
              className="fixed inset-0 z-[70] bg-background/60 backdrop-blur-[2px]"
              initial={{opacity: 0}}
              animate={{opacity: 1}}
              exit={{opacity: 0}}
              onClick={() => setOpen(false)}
            />
            <motion.aside
              className="fixed right-0 top-0 z-[71] flex h-full w-[360px] flex-col border-l border-border bg-card"
              initial={{x: "100%"}}
              animate={{x: 0}}
              exit={{x: "100%"}}
              transition={{duration: 0.5, ease: [0.22, 1, 0.36, 1]}}
            >
              <div className="flex items-center justify-between border-b border-border px-5 py-4">
                <h2 className="font-display text-lg font-black">{t("articles.relatedTitle")}</h2>
                <button
                  type="button"
                  aria-label={t("articles.related")}
                  onClick={() => setOpen(false)}
                  className="flex h-9 w-9 items-center justify-center rounded-pill border border-border
                             hover:bg-accent hover:text-background"
                >
                  ×
                </button>
              </div>
              <nav className="flex-1 overflow-y-auto p-4">
                {articles.map((article, index) => (
                  <motion.div
                    key={article.id}
                    initial={{opacity: 0, y: 12}}
                    animate={{opacity: 1, y: 0}}
                    transition={{delay: index * 0.06, duration: 0.35, ease: [0.22, 1, 0.36, 1]}}
                  >
                    <Link
                      href={`/articles/${article.id}`}
                      onClick={() => setOpen(false)}
                      className="hover-card group block rounded-lg border border-border p-4"
                    >
                      <p className="font-display text-sm font-bold leading-snug">{article.title}</p>
                      <p className="hover-card-dim mt-1 text-xs text-muted">
                        {article.publishTime?.slice(0, 10)} · {t("common.viewCount", {count: article.readingCount})}
                      </p>
                    </Link>
                  </motion.div>
                ))}
              </nav>
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      {/* 移动端：正文底部推荐区块（<lg 显示，由调用方置于正文尾部） */}
    </>
  );
}
