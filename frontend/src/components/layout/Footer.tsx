import {useTranslations} from "next-intl";

import AetherRing from "./AetherRing";

/**
 * 页脚（UI-Plan §5 / §6.1-8）：
 * 底部 LOGO 区域独立色块（T3 整区换色，与正文区块颜色区分，参考站页脚大色块），
 * AetherRing 滚动驱动"睁眼"展开（§3.4，滑动多少加载多少、回拉反向）。
 */
export default function Footer() {
  const t = useTranslations();
  return (
    <footer className="mt-24" data-module="article">
      {/* LOGO 动画区：分区色整块底（赭石/烛光赭）+ 反色内容 + 顶部门饰虚线；
          overflow-hidden 裁剪星星/纸飞机等氛围特效的出界部分 */}
      <div className="overflow-hidden bg-tint text-tint-on">
        <div className="border-t-2 border-dashed border-tint-on/30" />
        <div className="mx-auto flex max-w-6xl flex-col items-center gap-3 px-4 py-8 text-center">
          <AetherRing />
          <p className="text-xs tracking-widest opacity-85">{t("footer.slogan")}</p>
          <p className="text-xs opacity-70">{t("footer.copyright", {year: new Date().getFullYear()})}</p>
        </div>
      </div>
    </footer>
  );
}
