"use client";

import {useTheme} from "next-themes";

/**
 * 主题切换按钮（UI-Plan §5 Navbar 右上角；AnimatedThemeToggler 简版）
 *
 * <p>图标显隐纯 CSS（.dark 由 ThemeProvider attribute="class" 控制，
 * next-themes 注入脚本保证 SSR/hydration 一致，无 JS 状态）。</p>
 */
export default function ThemeToggle() {
  const {theme, setTheme} = useTheme();
  return (
    <button
      type="button"
      aria-label="切换日间/夜间模式"
      onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
      className="relative flex h-9 w-9 items-center justify-center rounded-pill border border-border
                 text-foreground transition-colors hover:bg-accent hover:text-background"
      style={{transition: "background-color 300ms cubic-bezier(0.22,1,0.36,1), color 300ms cubic-bezier(0.22,1,0.36,1)"}}
    >
      {/* 太阳/月亮符号：日间显太阳，dark 显月亮（衬线气质：字符代替图标库） */}
      <span aria-hidden className="text-sm leading-none dark:hidden">
        ☀
      </span>
      <span aria-hidden className="hidden text-sm leading-none dark:inline">
        ☾
      </span>
    </button>
  );
}
