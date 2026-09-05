"use client";

import {motion, useReducedMotion, useScroll, useSpring, useTransform, type MotionValue} from "framer-motion";
import {useRef} from "react";

/**
 * AetherRing 以太光环（UI-Plan §3.4，v2.0 F2 滚动驱动 + 氛围细节）：
 *
 * <p>底部吸附式滚动驱动：区块进入视口后先"顿住"（进度前段完全不显示，
 * 约半圈滚轮行程），继续下滚后光环**全环同步**逐步展开——整体由大渐小
 * （scale 1.3→1）落定 + 渐显，回拉反向放大淡出（useTransform 天然可逆）；
 * 中心亮点与字标在尾段接力点亮。prefers-reduced-motion 时直接呈现最终状态。
 *
 * 氛围细节（随机点缀，固定种子伪随机保证 SSR/CSR 渲染一致）：
 * 闪烁四角星、穿行小纸飞机、偶发流星、虚线装饰环慢转、
 * 双轨道光点沿环公转、光环整体呼吸、中心光晕脉冲。</p>
 */

const RINGS = [
  {size: 94, width: 1.5},
  {size: 164, width: 1.2},
  {size: 232, width: 1.2},
  {size: 288, width: 1},
];

/** 固定种子伪随机（线性同余）：同一序列在服务端与客户端生成，避免水合不匹配 */
function seeded(seed: number) {
  let s = seed % 2147483647;
  if (s <= 0) {
    s += 2147483646;
  }
  return () => (s = (s * 16807) % 2147483647) / 2147483647;
}

/** 闪烁星星参数（随机位置/大小/节奏/颜色） */
const STARS = (() => {
  const rand = seeded(20260901);
  return Array.from({length: 16}, () => ({
    left: 3 + rand() * 94, // %（容器内）
    top: 3 + rand() * 94,
    size: 3 + rand() * 7, // px
    delay: rand() * 4, // s
    duration: 2.2 + rand() * 3.6, // s
    gold: rand() > 0.45, // 书签金星 / 反色星
  }));
})();

/** 纸飞机参数（随机高度/方向/速度/大小） */
const PLANES = (() => {
  const rand = seeded(19700101);
  return Array.from({length: 3}, (_, i) => ({
    top: 10 + rand() * 64, // % 起始高度
    delay: 2 + rand() * 14, // s
    duration: 17 + rand() * 13, // s 单程
    scale: 0.65 + rand() * 0.6,
    toRight: i !== 1, // 中间那架反向
    drift: (rand() - 0.5) * 56, // y 向随机漂移幅度 px
  }));
})();

export default function AetherRing() {
  const ref = useRef<HTMLDivElement>(null);
  const reduceMotion = useReducedMotion();
  // 底部吸附：区块顶达视口 95% 处起计进度，至视口 60% 处完成
  const {scrollYProgress} = useScroll({target: ref, offset: ["start 0.95", "start 0.6"]});
  // 弹簧平滑（时间维度吸附感的关键）：快速滚到底时进度并不瞬时到位，
  // 弹簧以自身节奏滞后追赶（约 1s），前段死区在追赶期呈现"顿一下"再缓缓展开；
  // 慢速滚动时弹簧近似即时跟随（滑多少显示多少），回拉反向收拢
  const springProgress = useSpring(scrollYProgress, {stiffness: 55, damping: 18, mass: 1});
  // 三段式映射：弹簧进度前 35% 完全不显示（吸附死区），
  // 之后全环同步展开——由大渐小（1.35→1）落定 + 渐显；字标尾段接力（hooks 无条件调用）
  const bodyScale = useTransform(springProgress, [0.35, 0.95], [1.35, 1]);
  const bodyOpacity = useTransform(springProgress, [0.35, 0.95], [0, 1]);
  const centerScale = useTransform(springProgress, [0.35, 0.95], [0.4, 1]);
  const centerOpacity = useTransform(springProgress, [0.35, 0.95], [0, 1]);
  const labelOpacity = useTransform(springProgress, [0.92, 1], [0, 1]);
  const labelY = useTransform(springProgress, [0.92, 1], [10, 0]);

  return (
    <div
      ref={ref}
      className="relative mx-auto flex h-[300px] w-[300px] max-w-full items-center justify-center"
    >
      {/* 氛围层：星星/纸飞机/流星（纯装饰，不响应鼠标；由 Footer 色块 overflow-hidden 裁剪） */}
      {!reduceMotion && (
        <div aria-hidden className="pointer-events-none absolute inset-0">
          {/* 闪烁四角星 */}
          {STARS.map((star, index) => (
            <motion.svg
              key={index}
              viewBox="0 0 24 24"
              className="absolute"
              style={{
                left: `${star.left}%`,
                top: `${star.top}%`,
                width: star.size,
                height: star.size,
                color: star.gold ? "var(--highlight)" : "var(--tint-on)",
              }}
              animate={{opacity: [0.12, 0.95, 0.12], scale: [0.75, 1.2, 0.75]}}
              transition={{duration: star.duration, delay: star.delay, repeat: Infinity, ease: "easeInOut"}}
            >
              <path
                d="M12 0 L14.5 9.5 L24 12 L14.5 14.5 L12 24 L9.5 14.5 L0 12 L9.5 9.5 Z"
                fill="currentColor"
              />
            </motion.svg>
          ))}
          {/* 穿行的小纸飞机（随机方向/高度，长循环） */}
          {PLANES.map((plane, index) => (
            <motion.div
              key={index}
              className="absolute"
              style={{
                top: `${plane.top}%`,
                color: "var(--tint-on)",
                transform: plane.toRight ? undefined : "scaleX(-1)",
              }}
              initial={{x: plane.toRight ? -60 : 360, opacity: 0}}
              animate={{
                x: plane.toRight ? [-60, 360] : [360, -60],
                y: [0, plane.drift, 0],
                opacity: [0, 0.85, 0.85, 0],
                rotate: plane.toRight ? [0, 6, -3, 0] : [0, -6, 3, 0],
              }}
              transition={{
                duration: plane.duration,
                delay: plane.delay,
                repeat: Infinity,
                repeatDelay: 6 + index * 5,
                ease: "easeInOut",
                times: [0, 0.5, 0.9, 1],
              }}
            >
              <PaperPlane size={16 * plane.scale} />
            </motion.div>
          ))}
          {/* 偶发流星（右上 → 左下快速划过，长间隔循环） */}
          <motion.div
            className="absolute"
            style={{
              top: "12%",
              right: "6%",
              width: 72,
              height: 1,
              background: "linear-gradient(to left, var(--highlight), transparent)",
            }}
            initial={{x: 0, y: 0, opacity: 0}}
            animate={{x: [-190, 0], y: [95, 0], opacity: [0, 0, 0.9, 0, 0]}}
            transition={{duration: 9, repeat: Infinity, delay: 4, ease: "easeIn", times: [0, 0.82, 0.9, 0.97, 1]}}
          />
        </div>
      )}

      {/* 虚线装饰环（缓慢自转，蚀刻罗盘感；reduce 时静态） */}
      {reduceMotion ? (
        <div
          aria-hidden
          className="absolute rounded-full border border-dashed"
          style={{width: 258, height: 258, borderColor: "var(--tint-on)", opacity: 0.22}}
        />
      ) : (
        <motion.div
          aria-hidden
          className="absolute rounded-full border border-dashed"
          style={{width: 258, height: 258, borderColor: "var(--tint-on)", opacity: 0.22}}
          animate={{rotate: 360}}
          transition={{duration: 70, repeat: Infinity, ease: "linear"}}
        />
      )}

      {/* 双轨道光点：沿外环/中环公转（外层 rotate 线性驱动，内层 translate 定半径） */}
      {!reduceMotion && (
        <>
          <motion.div
            aria-hidden
            className="absolute inset-0"
            animate={{rotate: 360}}
            transition={{duration: 26, repeat: Infinity, ease: "linear"}}
          >
            <span
              className="absolute left-1/2 top-1/2 block h-1 w-1 rounded-full"
              style={{
                background: "var(--highlight)",
                boxShadow: "0 0 8px 2px var(--highlight)",
                transform: "translate(-50%, -144px)", // 外环半径 288/2
              }}
            />
          </motion.div>
          <motion.div
            aria-hidden
            className="absolute inset-0"
            animate={{rotate: -360}}
            transition={{duration: 38, repeat: Infinity, ease: "linear"}}
          >
            <span
              className="absolute left-1/2 top-1/2 block h-1 w-1 rounded-full"
              style={{
                background: "var(--tint-on)",
                opacity: 0.8,
                transform: "translate(-50%, -116px)", // 虚线装饰环半径附近
              }}
            />
          </motion.div>
        </>
      )}

      {/* 光环主体（全环同步：滚动时整体由大渐小落定 + 渐显；CSS 呼吸层温和起伏） */}
      <div
        aria-hidden
        className={
          reduceMotion
            ? "absolute inset-0 flex items-center justify-center"
            : "ring-breathe absolute inset-0 flex items-center justify-center"
        }
      >
        {RINGS.map((ring) => (
          <Ring key={ring.size} {...ring} scale={bodyScale} opacity={bodyOpacity} reduce={!!reduceMotion} />
        ))}
        {/* 中心亮点（与环同步展开；内层 span 做光晕脉冲） */}
        <motion.div
          className="absolute left-1/2 top-1/2"
          style={
            reduceMotion
              ? {transform: "translate(-50%, -50%)"}
              : {
                  transform: "translate(-50%, -50%)",
                  scale: centerScale,
                  opacity: centerOpacity,
                }
          }
        >
          <span
            className="halo-pulse block h-2 w-2 rounded-full"
            style={{background: "currentColor", boxShadow: "0 0 18px 4px currentColor"}}
          />
        </motion.div>
      </div>

      {/* 字标（尾段接力淡入上浮，不参与呼吸保持稳定） */}
      <motion.p
        className="absolute bottom-0 font-display text-sm font-black tracking-[0.5em]"
        style={reduceMotion ? {opacity: 1} : {opacity: labelOpacity, y: labelY}}
      >
        AETHER
      </motion.p>
    </div>
  );
}

/**
 * 单个圆环：使用父级统一进度映射（全环同步由大渐小落定 + 渐显，回拉反向）
 */
function Ring({
  size,
  width,
  scale,
  opacity,
  reduce,
}: {
  size: number;
  width: number;
  scale: MotionValue<number>;
  opacity: MotionValue<number>;
  reduce: boolean;
}) {
  if (reduce) {
    // 降级：直接呈现最终状态
    return (
      <div
        className="absolute rounded-full"
        style={{width: size, height: size, border: `${width}px solid currentColor`, opacity: 0.9}}
      />
    );
  }
  return (
    <motion.div
      className="absolute rounded-full"
      style={{width: size, height: size, border: `${width}px solid currentColor`, scale, opacity}}
    />
  );
}

/**
 * 小纸飞机（蚀刻细线风格，跟随容器 currentColor）
 */
function PaperPlane({size}: {size: number}) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M22 2 L11 13" />
      <path d="M22 2 L15 22 L11 13 L2 9 Z" />
    </svg>
  );
}
