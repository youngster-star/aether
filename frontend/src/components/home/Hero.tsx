"use client";

import {AnimatePresence, motion} from "framer-motion";
import {useTranslations} from "next-intl";
import {useEffect, useState} from "react";

/**
 * 主页 Hero（UI-Plan §6.1-2）：
 * 大字标 + morphing-text 口号轮换 + 悬浮信息面板（参考 home-02）
 *
 * <p>kinetic-text / morphing-text 的 magicui 语义以 framer-motion 自实现：
 * 口号每 3.5s 轮换（AnimatePresence）；信息面板在字标 hover 时浮现。</p>
 */
export default function Hero({locale}: {locale: string}) {
  const t = useTranslations("home");
  const slogans: string[] = t.raw("heroSlogans");
  const [index, setIndex] = useState(0);
  const [hovering, setHovering] = useState(false);

  // 口号轮换
  useEffect(() => {
    const timer = setInterval(() => setIndex((i) => (i + 1) % slogans.length), 3500);
    return () => clearInterval(timer);
  }, [slogans.length]);

  return (
    <section className="mx-auto mt-16 max-w-6xl px-4 text-center">
      {/* 大字标（kinetic-text 语义：hover 时逐字字重动画简化为整体缩放）
          取景框装饰：四角 L 形角标（.frame-corners 伪元素 + 补角 span）+ 虚线描边（§2.1 v2.0） */}
      <div
        className="frame-corners frame-dashed relative inline-block cursor-default px-10 py-8 sm:px-14"
        onMouseEnter={() => setHovering(true)}
        onMouseLeave={() => setHovering(false)}
      >
        {/* 右上/左下补角（左上/右下由 .frame-corners 伪元素绘制） */}
        <span aria-hidden className="frame-corner-tr" />
        <span aria-hidden className="frame-corner-bl" />
        <h1
          aria-label="AETHER"
          className="font-display text-7xl font-black tracking-[0.18em] sm:text-8xl lg:text-9xl"
          style={{transition: "letter-spacing 500ms cubic-bezier(0.22,1,0.36,1)"}}
        >
          AETHER
        </h1>
        {/* 光环装饰 */}
        <svg
          aria-hidden
          viewBox="0 0 200 200"
          className="pointer-events-none absolute left-1/2 top-1/2 -z-10 h-[120%] w-[120%]
                     -translate-x-1/2 -translate-y-1/2"
        >
          <circle cx="100" cy="100" r="88" fill="none" stroke="var(--accent)" strokeWidth="0.6" opacity=".5" />
          <circle cx="100" cy="100" r="70" fill="none" stroke="var(--accent-2)" strokeWidth="0.6" opacity=".5" />
        </svg>

        {/* 悬浮信息面板（参考 home-02 hover 信息展示） */}
        <AnimatePresence>
          {hovering && (
            <motion.div
              initial={{opacity: 0, y: 8, scale: 0.98}}
              animate={{opacity: 1, y: 0, scale: 1}}
              exit={{opacity: 0, y: 8, scale: 0.98}}
              transition={{duration: 0.3, ease: [0.22, 1, 0.36, 1]}}
              className="absolute left-1/2 top-full z-10 mt-6 w-72 -translate-x-1/2 rounded-lg
                         border border-border bg-card p-5 text-left shadow-aether"
            >
              <p className="text-sm leading-relaxed text-muted">{t("heroInfo")}</p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* 口号轮换（morphing-text 语义） */}
      <div className="mt-8 flex h-8 items-center justify-center overflow-hidden">
        <AnimatePresence mode="wait">
          <motion.p
            key={index}
            initial={{opacity: 0, y: 12}}
            animate={{opacity: 1, y: 0}}
            exit={{opacity: 0, y: -12}}
            transition={{duration: 0.4, ease: [0.22, 1, 0.36, 1]}}
            className="text-lg tracking-widest text-accent"
          >
            {slogans[index]}
          </motion.p>
        </AnimatePresence>
      </div>

      <p className="mt-4 text-xs tracking-[0.4em] text-muted">
        {locale === "zh" ? "个人网站 · 星界与纸张" : "Personal site · Stars and paper"}
      </p>
    </section>
  );
}
