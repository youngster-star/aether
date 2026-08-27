import {useTranslations} from "next-intl";

/**
 * 页脚（UI-Plan §5）：版权行 + 站内 slogan
 *
 * <p>AetherRing 滚动展开 LOGO（§3.4）在阶段 2 以静态光环符呈现，滚动触发动画后置。</p>
 */
export default function Footer() {
  const t = useTranslations();
  return (
    <footer className="mt-24 border-t border-border">
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-4 px-4 py-12 text-center">
        {/* 静态光环符（AetherRing 占位，§3.4 滚动展开动画后置） */}
        <svg viewBox="0 0 48 48" className="h-12 w-12" role="img" aria-label="Aether ring">
          <circle cx="24" cy="24" r="18" fill="none" stroke="var(--accent)" strokeWidth="1" opacity=".9" />
          <circle cx="24" cy="24" r="11" fill="none" stroke="var(--accent-2)" strokeWidth="1" opacity=".9" />
          <circle cx="24" cy="24" r="4" fill="none" stroke="var(--accent)" strokeWidth="1" opacity=".9" />
          <circle cx="24" cy="24" r="1.5" fill="var(--accent)" />
        </svg>
        <p className="text-xs tracking-widest text-muted">{t("footer.slogan")}</p>
        <p className="text-xs text-muted">
          {t("footer.copyright", {year: new Date().getFullYear()})}
        </p>
      </div>
    </footer>
  );
}
