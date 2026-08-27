"use client";

import {motion, useReducedMotion, useScroll, useTransform} from "framer-motion";
import {useRef} from "react";

/**
 * 文章详情吸顶标题栏（UI-Plan §8.1：滚动页面 5%-8% 时浮现）
 *
 * <p>framer-motion useScroll({target}) 取 scrollYProgress（0~1），
 * [0.05, 0.08] 映射 opacity/y 滑入；accent 横幅 + 标题文字（参考 article-02）；
 * prefers-reduced-motion 时直接常显。</p>
 */
export default function StickyTitleBar({title}: {title: string}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const reduceMotion = useReducedMotion();
  const {scrollYProgress} = useScroll({target: containerRef, offset: ["start start", "end end"]});
  const opacity = useTransform(scrollYProgress, [0.05, 0.08], [0, 1]);
  const y = useTransform(scrollYProgress, [0.05, 0.08], ["-100%", "0%"]);

  if (reduceMotion) {
    return null;
  }
  return (
    <>
      {/* 滚动测量锚点（包裹整个文章容器） */}
      <div ref={containerRef} className="pointer-events-none absolute inset-x-0 top-0 h-full" aria-hidden />
      <motion.header
        className="fixed inset-x-0 top-0 z-40 border-b border-background/20"
        style={{opacity, y, background: "var(--accent)", color: "var(--background)"}}
      >
        <div className="mx-auto flex h-12 max-w-6xl items-center gap-3 px-4">
          <span aria-hidden className="font-display text-sm opacity-70">
            AETHER
          </span>
          <span aria-hidden className="opacity-50">/</span>
          <p className="truncate font-display text-sm font-bold tracking-wide">{title}</p>
        </div>
      </motion.header>
    </>
  );
}
