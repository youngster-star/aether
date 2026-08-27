"use client";

import {AnimatePresence, motion} from "framer-motion";
import {useTranslations} from "next-intl";
import {useState} from "react";

import {Link} from "@/i18n/navigation";
import BlurFade from "@/components/ui/BlurFade";
import LocaleSwitcher from "./LocaleSwitcher";
import ThemeToggle from "./ThemeToggle";
import Wordmark from "./Wordmark";

/**
 * 全局导航（UI-Plan §5）：左侧汉堡（全屏 Sheet 导航，菜单项级联入场，参考 home-03）、
 * 中间文字 LOGO、右侧主题/语言/管理端入口。
 */
export default function Navbar() {
  const t = useTranslations();
  const [menuOpen, setMenuOpen] = useState(false);

  const menuItems = [
    {key: "articles", href: "/articles"},
    {key: "albums", href: "/albums"},
    {key: "videos", href: "/videos"},
    {key: "music", href: "/music"},
    {key: "books", href: "/books"},
    {key: "announcements", href: "/announcements"},
  ];

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-background/95 backdrop-blur-sm">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4">
        {/* 左侧：汉堡按钮 */}
        <button
          type="button"
          aria-label={t("nav.menu")}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen(true)}
          className="flex h-10 w-10 flex-col items-center justify-center gap-1.5 rounded-pill border border-border
                     transition-colors hover:bg-accent"
        >
          <span className="block h-px w-5 bg-foreground" />
          <span className="block h-px w-5 bg-foreground" />
          <span className="block h-px w-3 self-start ml-2.5 bg-accent" />
        </button>

        {/* 中间：字标 */}
        <Link href="/" className="hover:opacity-80 transition-opacity">
          <Wordmark height={40} />
        </Link>

        {/* 右侧：主题/语言/管理端 */}
        <div className="flex items-center gap-2">
          <ThemeToggle />
          <LocaleSwitcher />
          <a
            href="/aether/cryptex"
            target="_blank"
            rel="noopener noreferrer"
            className="hidden sm:flex h-9 items-center rounded-pill border border-border px-3 text-xs
                       tracking-widest transition-colors hover:bg-accent hover:text-background"
          >
            {t("nav.admin")}
          </a>
        </div>
      </div>

      {/* 全屏导航 Sheet（菜单项 blur-fade 级联入场） */}
      <AnimatePresence>
        {menuOpen && (
          <motion.div
            className="fixed inset-0 z-[60] flex flex-col bg-background"
            initial={{opacity: 0, y: -24}}
            animate={{opacity: 1, y: 0}}
            exit={{opacity: 0, y: -24}}
            transition={{duration: 0.5, ease: [0.22, 1, 0.36, 1]}}
          >
            <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-4">
              <Wordmark height={40} />
              <button
                type="button"
                aria-label={t("common.close")}
                onClick={() => setMenuOpen(false)}
                className="flex h-10 w-10 items-center justify-center rounded-pill border border-border
                           text-xl transition-colors hover:bg-accent hover:text-background"
              >
                ×
              </button>
            </div>
            <nav className="mx-auto flex w-full max-w-6xl flex-1 flex-col justify-center gap-2 px-4">
              {menuItems.map((item, index) => (
                <BlurFade key={item.key} delay={index * 0.06}>
                  <Link
                    href={item.href}
                    onClick={() => setMenuOpen(false)}
                    className="group flex items-baseline gap-4 border-b border-border py-4"
                  >
                    <span className="font-display text-sm text-accent">
                      {String(index + 1).padStart(2, "0")}
                    </span>
                    <span className="font-display text-4xl font-black tracking-wide transition-colors
                                     group-hover:text-accent">
                      {t(`nav.${item.key}`)}
                    </span>
                  </Link>
                </BlurFade>
              ))}
            </nav>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}
