"use client";

import {useLocale} from "next-intl";
import {useRouter} from "@/i18n/navigation";

/**
 * 语言切换按钮（UI-Plan §11：URL 不变，Cookie 持久化 + 刷新重渲染）
 */
export default function LocaleSwitcher() {
  const locale = useLocale();
  const router = useRouter();

  const toggle = () => {
    const next = locale === "zh" ? "en" : "zh";
    // next-intl 无前缀方案：写 Cookie 后刷新，URL 保持不变
    document.cookie = `NEXT_LOCALE=${next}; path=/; max-age=31536000`;
    router.refresh();
  };

  return (
    <button
      type="button"
      onClick={toggle}
      className="flex h-9 items-center justify-center rounded-pill border border-border px-3
                 text-xs tracking-widest text-foreground transition-colors
                 hover:bg-accent hover:text-background"
      style={{transition: "background-color 300ms cubic-bezier(0.22,1,0.36,1), color 300ms cubic-bezier(0.22,1,0.36,1)"}}
    >
      {locale === "zh" ? "EN" : "中"}
    </button>
  );
}
