"use client";

import {motion, useReducedMotion} from "framer-motion";

/**
 * 模糊淡入入场组件（UI-Plan §7 blur-fade 自实现：列表/区块级联入场）
 *
 * <p>framer-motion 实现（magicui 的 blur-fade 语义）：初始 blur(8px)+y 偏移
 * 淡入到最终态；delay 逐级递增形成级联。prefers-reduced-motion 时直接呈现。</p>
 */
export default function BlurFade({
  children,
  delay = 0,
  yOffset = 16,
  className,
}: {
  children: React.ReactNode;
  delay?: number;
  yOffset?: number;
  className?: string;
}) {
  const reduceMotion = useReducedMotion();
  if (reduceMotion) {
    return <div className={className}>{children}</div>;
  }
  return (
    <motion.div
      className={className}
      initial={{opacity: 0, y: yOffset, filter: "blur(8px)"}}
      whileInView={{opacity: 1, y: 0, filter: "blur(0px)"}}
      viewport={{once: true, margin: "-40px"}}
      transition={{
        duration: 0.5,
        delay,
        ease: [0.22, 1, 0.36, 1],
      }}
    >
      {children}
    </motion.div>
  );
}
