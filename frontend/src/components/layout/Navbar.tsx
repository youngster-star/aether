"use client";

import {AnimatePresence, motion} from "framer-motion";
import {useTranslations} from "next-intl";
import {useEffect, useState} from "react";

import {Link} from "@/i18n/navigation";
import LocaleSwitcher from "./LocaleSwitcher";
import ThemeToggle from "./ThemeToggle";
import Wordmark from "./Wordmark";

/**
 * 全局导航（UI-Plan §5）：左侧汉堡（点击弹出导航面板，交互与文章页
 * "查看更多文章"一致——实色覆盖层完全遮住原页面 + 左侧滑出面板，
 * 点右上角叉号 / 覆盖层空白处 / ESC 关闭）、中间文字 LOGO、
 * 右侧主题/语言/管理端入口。
 *
 * <p>v2.0 动效（§2.8）：A1 汉堡中杆 hover 伸长；A2 打开态三杆→X 动画；
 * A3 面板 blur-fade 级联入场；A4 菜单条目 hover 左移 + 前缀"❧"淡入。
 * 注意：覆盖层必须置于 header 之外（header 的 sticky + backdrop-blur 会创建
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

  // ESC 关闭
  useEffect(() => {
    if (!menuOpen) {
      return;
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMenuOpen(false);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [menuOpen]);

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

      {/* A3 导航覆盖层：与"查看更多文章"同款交互——实色羊皮纸底完全遮住原页面
          （非半透明虚化），点覆盖层空白处关闭 */}
      <AnimatePresence>
        {menuOpen && (
          <motion.div
            className="fixed inset-0 z-[60] bg-background"
            initial={{opacity: 0}}
            animate={{opacity: 1}}
            exit={{opacity: 0}}
            transition={{duration: 0.35, ease: [0.22, 1, 0.36, 1]}}
            onClick={() => setMenuOpen(false)}
          >
            {/* 关闭按钮：顶部最右侧（与 RelatedPanel 的 X 同位，交互统一） */}
            <button
              type="button"
              aria-label={t("common.close")}
              onClick={() => setMenuOpen(false)}
              className="absolute right-5 top-5 z-[62] flex h-10 w-10 items-center justify-center
                         rounded-pill border border-border bg-card text-xl shadow-aether
                         transition-colors hover:bg-accent hover:text-background"
            >
              ×
            </button>

            {/* 左侧滑出导航面板（书页卡底，与 RelatedPanel 面板同构；stopPropagation 防误触关闭） */}
            <motion.aside
              role="dialog"
              aria-modal="true"
              aria-label={t("nav.menu")}
              className="flex h-full w-full max-w-md flex-col border-r border-border bg-card shadow-aether"
              initial={{x: "-100%"}}
              animate={{x: 0}}
              exit={{x: "-100%"}}
              transition={{duration: 0.5, ease: [0.22, 1, 0.36, 1]}}
              onClick={(event) => event.stopPropagation()}
            >
              <div className="flex items-center justify-between border-b border-border px-6 py-4">
                <Wordmark height={34} />
                {/* A2：X 由双杆旋转合成（仅此面板内冗余关闭位，主关闭钮在覆盖层右上） */}
              </div>
              <nav className="flex flex-1 flex-col px-6 py-6">
                {menuItems.map((item, index) => (
                  // A3 级联入场：挂载即播放（delay 递增）
                  <motion.div
                    key={item.key}
                    initial={{opacity: 0, y: 16, filter: "blur(6px)"}}
                    animate={{opacity: 1, y: 0, filter: "blur(0px)"}}
                    exit={{opacity: 0, y: 8}}
                    transition={{delay: 0.12 + index * 0.05, duration: 0.4, ease: [0.22, 1, 0.36, 1]}}
                  >
                    <Link
                      href={item.href}
                      onClick={() => setMenuOpen(false)}
                      className="group flex items-baseline gap-4 border-b border-border py-4 last:border-b-0"
                    >
                      <span className="font-display text-xs text-accent">
                        {String(index + 1).padStart(2, "0")}
                      </span>
                      {/* A4：hover 左移 + 前缀"❧"淡入（globals .menu-item） */}
                      <span className="menu-item font-display text-3xl font-black tracking-wide">
                        {t(`nav.${item.key}`)}
                      </span>
                    </Link>
                  </motion.div>
                ))}
              </nav>
            </motion.aside>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
