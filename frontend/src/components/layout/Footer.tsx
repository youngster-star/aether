import {useTranslations} from "next-intl";

import AetherRing from "./AetherRing";

/**
 * 页脚（UI-Plan §5 / §6.1-8）：版权行 + 底部滚动展开的 AetherRing LOGO（§3.4，F2 睁眼动画）
 */
export default function Footer() {
  const t = useTranslations();
  return (
    <footer className="mt-24 border-t border-border">
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-4 px-4 py-12 text-center">
        {/* AetherRing：滚入视口由闭到开"睁眼"展开（F2） */}
        <AetherRing />
        <p className="text-xs tracking-widest text-muted">{t("footer.slogan")}</p>
        <p className="text-xs text-muted">
          {t("footer.copyright", {year: new Date().getFullYear()})}
        </p>
      </div>
    </footer>
  );
}
