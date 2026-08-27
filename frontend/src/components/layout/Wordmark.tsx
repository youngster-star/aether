/**
 * SVG 文字 LOGO 字标（UI-Plan §3.3 静态版，light/dark 由 CSS 变量自适应）
 *
 * <p>以太光环符（外环+内环+中心点）+ AETHER 字标 + 双线饰线。
 * 颜色全部走 var(--foreground)/var(--accent)/var(--accent-2)。</p>
 */
export default function Wordmark({height = 56}: {height?: number}) {
  return (
    <svg
      className="aether-wordmark"
      viewBox="0 0 360 100"
      style={{height}}
      role="img"
      aria-label="Aether"
    >
      <circle cx="180" cy="22" r="13" fill="none" stroke="var(--accent)" strokeWidth="1.5" />
      <circle cx="180" cy="22" r="7" fill="none" stroke="var(--accent-2)" strokeWidth="1.5" />
      <circle cx="180" cy="22" r="2" fill="var(--accent)" />
      <text
        x="180"
        y="66"
        textAnchor="middle"
        fontFamily="'Noto Serif SC','Source Serif 4',Georgia,serif"
        fontWeight="900"
        fontSize="44"
        letterSpacing="10"
        fill="var(--foreground)"
      >
        AETHER
      </text>
      <path d="M 88 82 H 172 M 188 82 H 272" stroke="var(--border)" strokeWidth="1" />
      <rect
        x="176"
        y="78"
        width="8"
        height="8"
        transform="rotate(45 180 82)"
        fill="var(--accent)"
      />
    </svg>
  );
}
