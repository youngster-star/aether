"use client";

import {AnimatePresence, motion} from "framer-motion";
import {useTranslations} from "next-intl";
import {useState} from "react";

import {Link} from "@/i18n/navigation";
import LocaleSwitcher from "./LocaleSwitcher";
import ThemeToggle from "./ThemeToggle";
import Wordmark from "./Wordmark";

/**
 * 全局导航（UI-Plan §5）：左侧汉堡（全屏 Sheet 导航，菜单项级联入场，参考 home-03）、
 * 中间文字 LOGO、右侧主题/语言/管理端入口。
 *
 * <p>v2.0 动效（§2.8）：A1 汉堡中杆 hover 伸长；A2 打开态三杆→X 动画；
 * A3 全屏层 blur-fade 级联入场 + 毛玻璃模糊底；A4 菜单条目 hover 左移 + 前缀"❧"淡入。
 * 注意：全屏层必须置于 header 之外（header 的 sticky + backdrop-blur 会创建
 * containing block，把内部 fixed 层压缩进导航条高度）。</p>
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
    <>
      <header className="sticky top-0 z-50 border-b border-border bg-background/95 backdrop-blur-sm">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4">
          {/* 左侧：汉堡按钮（A1：hover 中杆由 70% 伸至 100%） */}
          <button
            type="button"
            aria-label={t("nav.menu")}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen(true)}
            className="flex h-10 w-10 flex-col items-center justify-center gap-1.5 rounded-pill border border-border
                       transition-colors hover:bg-accent"
          >
            <span className="block h-px w-5 bg-foreground" />
            {/* 中杆：70% 宽，hover 伸满（globals .burger-bar-mid） */}
            <span className="burger-bar burger-bar-mid block h-px w-3.5 bg-foreground" />
            <span className="block h-px w-5 bg-foreground" />
          </button>

          {/* 中间：字标（A5 光环 hover 旋转在 Wordmark 内） */}
          <Link href="/" className="transition-opacity hover:opacity-80">
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
              className="hidden h-9 items-center rounded-pill border border-border px-3 text-xs
                         tracking-widest transition-colors hover:bg-accent hover:text-background sm:flex"
            >
              {t("nav.admin")}
            </a>
          </div>
        </div>
      </header>

      {/* A3 全屏导航层：毛玻璃模糊底（bg-background/85 + backdrop-blur），
          挂载即播放级联入场（不依赖视口检测，修复 fixed 层内 whileInView 不触发） */}
      <AnimatePresence>
        {menuOpen && (
          <motion.div
            className="fixed inset-0 z-[60] flex flex-col bg-background/85 backdrop-blur-xl"
            initial={{opacity: 0, y: -24}}
            animate={{opacity: 1, y: 0}}
            exit={{opacity: 0, y: -24}}
            transition={{duration: 0.5, ease: [0.22, 1, 0.36, 1]}}
          >
            <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-4">
              <Wordmark height={40} />
              {/* A2：X 由三杆旋转合成，关闭时旋转回落 */}
              <button
                type="button"
                aria-label={t("common.close")}
                onClick={() => setMenuOpen(false)}
                className="flex h-10 w-10 flex-col items-center justify-center gap-1.5 rounded-pill
                           border border-border transition-colors hover:bg-accent hover:text-background"
              >
                <motion.span
                  className="block h-px w-5 bg-foreground"
                  initial={{rotate: 0, y: -3.5}}
                  animate={{rotate: 45, y: 3.5}}
                  exit={{rotate: 0, y: -3.5}}
                  transition={{duration: 0.3, ease: [0.22, 1, 0.36, 1]}}
                />
                <motion.span
                  className="block h-px w-5 bg-foreground"
                  initial={{opacity: 1}}
                  animate={{opacity: 0}}
                  exit={{opacity: 1}}
                  transition={{duration: 0.2}}
                />
                <motion.span
                  className="block h-px w-5 bg-foreground"
                  initial={{rotate: 0, y: 3.5}}
                  animate={{rotate: -45, y: -3.5}}
                  exit={{rotate: 0, y: 3.5}}
                  transition={{duration: 0.3, ease: [0.22, 1, 0.36, 1]}}
                />
              </button>
            </div>
            <nav className="mx-auto flex w-full max-w-6xl flex-1 flex-col justify-center gap-2 px-4">
              {menuItems.map((item, index) => (
                // A3 级联入场：挂载即播放（delay 递增），不依赖 whileInView
                <motion.div
                  key={item.key}
                  initial={{opacity: 0, y: 24, filter: "blur(8px)"}}
                  animate={{opacity: 1, y: 0, filter: "blur(0px)"}}
                  exit={{opacity: 0, y: 12, filter: "blur(4px)"}}
                  transition={{delay: 0.08 + index * 0.06, duration: 0.5, ease: [0.22, 1, 0.36, 1]}}
                >
                  <Link
                    href={item.href}
                    onClick={() => setMenuOpen(false)}
                    className="group flex items-baseline gap-4 border-b border-border py-4"
                  >
                    <span className="font-display text-sm text-accent">
                      {String(index + 1).padStart(2, "0")}
                    </span>
                    {/* A4：hover 左移 + 前缀"❧"淡入（globals .menu-item） */}
                    <span className="menu-item font-display text-4xl font-black tracking-wide">
                      {t(`nav.${item.key}`)}
                    </span>
                  </Link>
                </motion.div>
              ))}
            </nav>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
