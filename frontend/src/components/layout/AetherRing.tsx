"use client";

import {motion, useReducedMotion} from "framer-motion";

/**
 * AetherRing 以太光环（UI-Plan §3.4，v2.0 F2）：
 * 同心圆环由闭到开、由暗到亮"苏醒"（呼应参考站眼睛由闭到睁）。
 * 滚动进入视口触发播放一次（viewport once + amount 0.4）；
 * prefers-reduced-motion 时直接呈现最终状态。
 */
const RINGS = [
  {size: 120, color: "var(--accent)", width: 2},
  {size: 210, color: "var(--accent-2)", width: 1.5},
  {size: 300, color: "var(--accent)", width: 1.5},
  {size: 390, color: "var(--accent-2)", width: 1},
];

export default function AetherRing() {
  const reduceMotion = useReducedMotion();
  const shared = reduceMotion
    ? {} // 降级：无动画，直接最终状态
    : {
        initial: {scale: 0.55, opacity: 0},
        whileInView: {scale: 1, opacity: 0.9},
        viewport: {once: true, amount: 0.4},
      };
  return (
    <div className="relative mx-auto flex h-[400px] w-[400px] max-w-full items-center justify-center">
      {RINGS.map((ring, index) => (
        <motion.div
          key={ring.size}
          aria-hidden
          className="absolute rounded-full"
          style={{
            width: ring.size,
            height: ring.size,
            border: `${ring.width}px solid ${ring.color}`,
          }}
          transition={{duration: 1.4, ease: [0.22, 1, 0.36, 1], delay: index * 0.12}}
          {...shared}
        />
      ))}
      {/* 中心亮点（delay 480ms） */}
      <motion.div
        aria-hidden
        className="absolute h-2 w-2 rounded-full"
        style={{background: "var(--accent)", boxShadow: "0 0 18px 4px var(--accent)"}}
        transition={{duration: 0.6, ease: [0.22, 1, 0.36, 1], delay: 0.48}}
        {...shared}
      />
      {/* 字标（1s 后淡入） */}
      <motion.p
        className="absolute bottom-0 font-display text-sm font-black tracking-[0.5em]"
        transition={{duration: 1, ease: [0.22, 1, 0.36, 1], delay: 1}}
        {...shared}
      >
        AETHER
      </motion.p>
    </div>
  );
}
