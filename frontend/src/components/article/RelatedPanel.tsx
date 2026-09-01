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
 * （相关文章卡片列表）+ 右区顶部引导语 + 相关文章 2 列网格；
 * 右缘按钮文字切换为"收起"；ESC/点击空白关闭。
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

      {/* 打开态：主区淡分区色覆盖层（T3）+ 左侧白色列表面板 + 右区推荐网格 */}
      <AnimatePresence>
        {open && (
          <motion.div
            className="fixed inset-0 z-[70]"
            style={{
              background: "color-mix(in srgb, var(--tint) 12%, var(--background))",
            }}
            initial={{opacity: 0}}
            animate={{opacity: 1}}
            exit={{opacity: 0}}
            transition={{duration: 0.3, ease: [0.22, 1, 0.36, 1]}}
            onClick={() => setOpen(false)}
          >
            {/* 关闭按钮：打开后固定在顶部最左侧（站长要求，参考 article-04 布局） */}
            <button
              type="button"
              aria-label={t("common.close")}
              onClick={() => setOpen(false)}
              className="absolute left-5 top-5 z-[80] flex h-10 w-10 items-center justify-center
                         rounded-pill border border-border bg-card text-xl shadow-aether
                         transition-colors hover:bg-accent hover:text-background"
            >
              ×
            </button>
            <div className="flex h-full">
              {/* 左侧白色列表面板（约 1/3 视口宽，参考 article-04） */}
              <motion.aside
                className="flex h-full w-full max-w-md flex-col border-r border-border bg-card shadow-aether"
                initial={{x: "-100%"}}
                animate={{x: 0}}
                exit={{x: "-100%"}}
                transition={{duration: 0.5, ease: [0.22, 1, 0.36, 1]}}
                onClick={(event) => event.stopPropagation()}
              >
                <div className="flex items-center border-b border-border px-5 py-4">
                  <h2 className="font-display text-lg font-black">{t("articles.relatedTitle")}</h2>
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

              {/* 右区：顶部引导语 + 相关文章 2 列网格（点击空白处关闭） */}
              <div className="flex-1 overflow-y-auto px-10 py-14">
                <motion.div
                  initial={{opacity: 0, y: 16}}
                  animate={{opacity: 1, y: 0}}
                  transition={{delay: 0.2, duration: 0.4, ease: [0.22, 1, 0.36, 1]}}
                  className="mx-auto max-w-3xl"
                  onClick={(event) => event.stopPropagation()}
                >
                  <p className="text-center font-display text-2xl font-black tracking-wide">
                    {t("articles.relatedLead")}
                  </p>
                  <div className="mt-8 grid gap-5 md:grid-cols-2">
                    {articles.map((article, index) => (
                      <motion.div
                        key={article.id}
                        initial={{opacity: 0, y: 16}}
                        animate={{opacity: 1, y: 0}}
                        transition={{delay: 0.25 + index * 0.06, duration: 0.4, ease: [0.22, 1, 0.36, 1]}}
                      >
                        <RelatedCard article={article} onNavigate={() => setOpen(false)} />
                      </motion.div>
                    ))}
                  </div>
                </motion.div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}

/**
 * 相关文章卡片（面板与右区网格共用）：hover 整卡反色（§2.7 T2）
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
