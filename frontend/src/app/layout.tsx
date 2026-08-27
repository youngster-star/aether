import type {Metadata} from "next";
import {NextIntlClientProvider} from "next-intl";
import {getLocale, getMessages} from "next-intl/server";
import {ThemeProvider} from "next-themes";

import "./globals.css";

export const metadata: Metadata = {
  title: "Aether | 以太",
  description: "个人博客 + 多媒体内容管理 + 日常记录（UI-Plan §11：metadata 规范化，全站禁爬）",
};

/**
 * 根布局（UI-Plan §4.1）：主题（next-themes）+ i18n（next-intl，无语言路由前缀）
 *
 * <p>字体走 globals.css 系统衬线栈（UI-Plan §12.3 遗留问题 2：next/font 优化后置）。</p>
 */
export default async function RootLayout({children}: LayoutProps<"/">) {
  const locale = await getLocale();
  const messages = await getMessages();
  return (
    <html
      lang={locale === "zh" ? "zh-CN" : "en"}
      suppressHydrationWarning
      className="h-full antialiased"
    >
      <body className="min-h-full flex flex-col">
        <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
          <NextIntlClientProvider messages={messages}>{children}</NextIntlClientProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
