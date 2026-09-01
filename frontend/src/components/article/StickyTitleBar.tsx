"use client";

import {motion, useReducedMotion, useScroll, useTransform} from "framer-motion";

/**
 * 文章详情吸顶标题栏（UI-Plan §6.3/§8.1）
 *
 * <p>目的：用户向下滑动阅读、文章标题滚出视口后，顶部出现 banner 提示当前文章。
 * v2.0 改为固定滚动阈值（滚过标题区约 240-320px 区间渐入，滚回顶部自动滑出），
 * 不再依赖文章总长度的百分比（长文 5%-8% 会迟迟不出现，短文又立即出现）。
 * T3 整区换色横幅（--tint 底 + 反色文字）+ 底部 2px 阅读进度条（C2）；
 * prefers-reduced-motion 时直接常显。</p>
 */
export default function StickyTitleBar({title}: {title: string}) {
  const reduceMotion = useReducedMotion();
  // 页面级滚动：240→320px 区间渐入（滚过文章标题区），滚回顶部渐出
  const {scrollY} = useScroll();
  const opacity = useTransform(scrollY, [240, 320], [0, 1]);
  const y = useTransform(scrollY, [240, 320], ["-100%", "0%"]);
  // C2：阅读进度条（0 → 页面几乎滚到底，按滚动像素映射）
  const progress = useTransform(scrollY, [320, 100000], [0, 1], {clamp: true});

  if (reduceMotion) {
    return null;
  }
  return (
    /* C1：T3 整区换色横幅（--tint 底 + 反色文字） */
    <motion.header
      className="fixed inset-x-0 top-0 z-40"
      style={{opacity, y, background: "var(--tint)", color: "var(--tint-on)"}}
    >
      <div className="mx-auto flex h-12 max-w-6xl items-center gap-3 px-4">
        {/* LOGO 光环符（参考 article-02 左上眼睛 LOGO 的等价物） */}
        <svg viewBox="0 0 48 48" className="h-6 w-6 shrink-0" aria-hidden>
          <circle cx="24" cy="24" r="18" fill="none" stroke="currentColor" strokeWidth="2" opacity=".9" />
          <circle cx="24" cy="24" r="7" fill="currentColor" opacity=".9" />
        </svg>
        <p className="truncate font-display text-sm font-bold tracking-wide">{title}</p>
      </div>
      {/* C2：底部阅读进度条 */}
      <motion.div
        aria-hidden
        className="absolute inset-x-0 bottom-0 h-0.5 origin-left bg-current opacity-60"
        style={{scaleX: progress}}
      />
    </motion.header>
  );
}
