"use client";

/**
 * 竖排侧边文字按钮（UI-Plan §2.8 C3/C4 / §8.2）：
 *
 * <p>文字竖排非正放（writing-mode: vertical-rl，中文直立/拉丁旋转 90°）；
 * hover 时变 --tint + 逐字接力跳动（每字 translateY -6px 回落，stagger 45ms，
 * CSS 见 globals .side-tab/@keyframes hop）+ 伴随细线伸长。
 * open 时文字切换为 labelOpen（"收起"），跳动效果保留。</p>
 */
export default function SideTab({
  label,
  labelOpen,
  open,
  onToggle,
}: {
  label: string;
  labelOpen: string;
  open: boolean;
  onToggle: () => void;
}) {
  const text = open ? labelOpen : label;
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-label={text}
      aria-expanded={open}
      className="side-tab fixed right-2 top-1/2 z-40 hidden -translate-y-1/2 rounded-pill px-3 py-8
                 font-display text-sm font-bold transition-colors hover:bg-tint/10 lg:block"
    >
      {/* 每字一个 span，--i 为接力跳动序号；空格用不换行空格占位 */}
      {Array.from(text).map((ch, index) => (
        <span
          key={`${text}-${index}`}
          className="ch"
          style={{"--i": index} as React.CSSProperties}
        >
          {ch === " " ? "\u00A0" : ch}
        </span>
      ))}
    </button>
  );
}
