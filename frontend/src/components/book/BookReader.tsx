"use client";

import {useCallback, useEffect, useRef, useState} from "react";
import {useTranslations} from "next-intl";
import {AnimatePresence, motion} from "framer-motion";

import {apiGet} from "@/lib/api/client";
import type {BookChapterContentVO, ChapterNodeVO} from "@/lib/api/types";

/**
 * 书籍阅读器（UI-Plan §6.7 / BackEnd-Plan §5.2 章节接口）
 *
 * <p>阅读态流程：封面页 → rotateY 翻页 → 版权声明页（ownership_type 决定文案）→ 正文；
 * 正文内：目录抽屉（章-节树）、字号/行距/阅读主题设置（localStorage 全局偏好）、
 * 阅读进度记忆（按书存 chapterId+percent）、←/→ 键盘翻章、ESC 退出。
 * 章节内容为后端排版 HTML（p.indent 段首空两格 + 段间半行距，§7.2）。</p>
 */

/** 阅读主题（纸色/护眼/夜间） */
const THEMES = {
  paper: {bg: "#F5F0E6", text: "#2A2622", muted: "#6B6459", border: "#D8CFC0"},
  green: {bg: "#DCE8DC", text: "#22302A", muted: "#5A6E60", border: "#B8CCBA"},
  night: {bg: "#161614", text: "#C8C2B8", muted: "#8A857C", border: "#33312C"},
} as const;

type ThemeKey = keyof typeof THEMES;

/** 阅读偏好（全局共享，localStorage） */
interface ReaderPrefs {
  fontSize: number;
  lineHeight: number;
  theme: ThemeKey;
}

const DEFAULT_PREFS: ReaderPrefs = {fontSize: 18, lineHeight: 1.8, theme: "paper"};

/** 按书记忆的阅读进度 */
interface BookProgress {
  chapterId: number;
  percent: number;
}

type ReaderMode = "closed" | "cover" | "copyright" | "reading";

export default function BookReader({
  bookId,
  bookTitle,
  ownershipType,
  chapters,
}: {
  bookId: number;
  bookTitle: string;
  ownershipType: number;
  chapters: ChapterNodeVO[];
}) {
  const t = useTranslations("book");

  const [mode, setMode] = useState<ReaderMode>("closed");
  const [current, setCurrent] = useState<BookChapterContentVO | null>(null);
  const [loading, setLoading] = useState(false);
  const [tocOpen, setTocOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [prefs, setPrefs] = useState<ReaderPrefs>(DEFAULT_PREFS);
  const [savedProgress, setSavedProgress] = useState<BookProgress | null>(null);

  // 章节内容缓存（会话内复用，翻回不重复请求）
  const chapterCache = useRef(new Map<number, BookChapterContentVO>());
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const progressTimer = useRef<number | null>(null);

  /** 从 localStorage 恢复偏好与进度（进入阅读器的事件回调内执行，避免 effect 内 setState） */
  const restoreFromStorage = useCallback(() => {
    try {
      const rawPrefs = localStorage.getItem("aether-reader-prefs");
      if (rawPrefs) {
        const parsed = JSON.parse(rawPrefs) as Partial<ReaderPrefs>;
        const parsedTheme = parsed.theme;
        setPrefs({
          fontSize: parsed.fontSize ?? DEFAULT_PREFS.fontSize,
          lineHeight: parsed.lineHeight ?? DEFAULT_PREFS.lineHeight,
          theme: (parsedTheme !== undefined && parsedTheme in THEMES
              ? parsedTheme : DEFAULT_PREFS.theme) as ThemeKey,
        });
      }
    } catch {
      // localStorage 不可用：使用默认偏好
    }
    try {
      const rawProgress = localStorage.getItem(`aether-book-${bookId}`);
      if (rawProgress) {
        setSavedProgress(JSON.parse(rawProgress) as BookProgress);
      } else {
        setSavedProgress(null);
      }
    } catch {
      setSavedProgress(null);
    }
  }, [bookId]);

  const persistPrefs = useCallback((next: ReaderPrefs) => {
    setPrefs(next);
    try {
      localStorage.setItem("aether-reader-prefs", JSON.stringify(next));
    } catch {
      // 忽略持久化失败
    }
  }, []);

  /** 拉取章节内容（带会话缓存） */
  const loadChapter = useCallback(async (chapterId: number) => {
    const cached = chapterCache.current.get(chapterId);
    if (cached) {
      setCurrent(cached);
      return cached;
    }
    setLoading(true);
    try {
      const content = await apiGet<BookChapterContentVO>(`/books/${bookId}/chapters/${chapterId}`);
      chapterCache.current.set(chapterId, content);
      setCurrent(content);
      return content;
    } catch {
      return null;
    } finally {
      setLoading(false);
    }
  }, [bookId]);

  /** 进入阅读（恢复进度或从封面开始） */
  const enterReader = useCallback(() => {
    // 先从 localStorage 恢复偏好与进度（事件回调内 setState 合规）
    restoreFromStorage();
    let progress: BookProgress | null = null;
    try {
      const raw = localStorage.getItem(`aether-book-${bookId}`);
      progress = raw ? JSON.parse(raw) as BookProgress : null;
    } catch {
      progress = null;
    }
    if (progress && progress.chapterId > 0) {
      setMode("reading");
      void loadChapter(progress.chapterId);
      return;
    }
    setMode("cover");
  }, [restoreFromStorage, bookId, loadChapter]);

  /** 翻页进入正文（版权页点击或 → 键） */
  const enterReading = useCallback(async () => {
    setMode("reading");
    const first = firstChapterId(chapters);
    await loadChapter(savedProgress?.chapterId ?? first);
  }, [chapters, savedProgress, loadChapter]);

  /** 切换章节（prev/next） */
  const gotoChapter = useCallback(async (chapterId: number) => {
    const content = await loadChapter(chapterId);
    if (content) {
      // 保存进度（章节锚点）
      try {
        localStorage.setItem(`aether-book-${bookId}`,
            JSON.stringify({chapterId, percent: 0} satisfies BookProgress));
      } catch {
        // 忽略持久化失败
      }
      setSavedProgress({chapterId, percent: 0});
      scrollRef.current?.scrollTo({top: 0});
    }
  }, [bookId, loadChapter]);

  /** 滚动节流保存进度（0.5s） */
  const onScroll = useCallback(() => {
    const container = scrollRef.current;
    if (!container || !current) {
      return;
    }
    if (progressTimer.current !== null) {
      return;
    }
    progressTimer.current = window.setTimeout(() => {
      progressTimer.current = null;
      const max = container.scrollHeight - container.clientHeight;
      const percent = max > 0 ? Math.min(1, container.scrollTop / max) : 0;
      const progress = {chapterId: current.chapterId, percent};
      try {
        localStorage.setItem(`aether-book-${bookId}`, JSON.stringify(progress));
      } catch {
        // 忽略持久化失败
      }
      setSavedProgress(progress);
    }, 500);
  }, [bookId, current]);

  /** 键盘：←/→ 翻章/翻页，ESC 退出 */
  useEffect(() => {
    if (mode === "closed") {
      return;
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMode("closed");
        setTocOpen(false);
        setSettingsOpen(false);
        return;
      }
      if (event.key === "ArrowRight") {
        if (mode === "cover") {
          setMode("copyright");
        } else if (mode === "copyright") {
          void enterReading();
        } else if (current?.next) {
          void gotoChapter(current.next.chapterId);
        }
        return;
      }
      if (event.key === "ArrowLeft" && mode === "reading" && current?.prev) {
        void gotoChapter(current.prev.chapterId);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [mode, current, enterReading, gotoChapter]);

  // 阅读态锁 body 滚动（阅读器内部滚动）
  useEffect(() => {
    if (mode === "closed") {
      return;
    }
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previous;
    };
  }, [mode]);

  // 恢复进度滚动位置（章节加载后按 percent 定位）
  useEffect(() => {
    if (mode !== "reading" || !current || loading) {
      return;
    }
    const container = scrollRef.current;
    if (container && savedProgress?.chapterId === current.chapterId && savedProgress.percent > 0.02) {
      const max = container.scrollHeight - container.clientHeight;
      container.scrollTop = max * savedProgress.percent;
    }
  }, [mode, current, loading, savedProgress]);

  if (chapters.length === 0) {
    return null;
  }

  const theme = THEMES[prefs.theme];
  const progressPercent = current && current.totalChapters > 0
      ? Math.round((current.orderNo / current.totalChapters) * 100)
      : 0;

  return (
    <>
      {/* 入口按钮（恢复进度时显示继续阅读） */}
      <div className="mt-5 flex flex-wrap gap-3">
        <button
          type="button"
          onClick={enterReader}
          data-module="book"
          className="rounded-md bg-accent px-5 py-2.5 text-sm font-bold text-white
                     shadow-aether transition-transform hover:-translate-y-0.5"
        >
          {savedProgress ? t("resumeReading") : t("startReading")}
        </button>
      </div>

      {/* 全屏阅读器（cover → copyright → reading） */}
      <AnimatePresence>
        {mode !== "closed" && (
          <motion.div
            key="reader-overlay"
            initial={{opacity: 0}}
            animate={{opacity: 1}}
            exit={{opacity: 0}}
            transition={{duration: 0.25}}
            className="fixed inset-0 z-[90]"
            style={{backgroundColor: theme.bg, color: theme.text}}
            role="dialog"
            aria-modal="true"
            aria-label={`${bookTitle} reader`}
          >
            {mode === "cover" && (
              <CoverPage
                title={bookTitle}
                onFlip={() => setMode("copyright")}
                flipHint={t("flipHint")}
              />
            )}
            {mode === "copyright" && (
              <CopyrightPage
                ownershipType={ownershipType}
                onFlip={() => void enterReading()}
                onBack={() => setMode("cover")}
                flipHint={t("flipHint")}
                title={t("copyrightTitle")}
                text={ownershipType === 1 ? t("copyrightOwner1") : t("copyrightOwner2")}
              />
            )}
            {mode === "reading" && (
              <div className="flex h-full flex-col">
                {/* 顶栏：书名/章节 + 进度 + 目录/设置/退出 */}
                <header
                  className="flex items-center gap-3 border-b px-4 py-2.5 text-xs"
                  style={{borderColor: theme.border}}
                >
                  <button type="button" onClick={() => setMode("closed")}
                          className="rounded border px-2 py-1 font-bold"
                          style={{borderColor: theme.border}}>
                    {t("exitReader")}
                  </button>
                  <button type="button" onClick={() => {setTocOpen(true); setSettingsOpen(false);}}
                          className="rounded border px-2 py-1 font-bold"
                          style={{borderColor: theme.border}}>
                    {t("openToc")}
                  </button>
                  <div className="min-w-0 flex-1 truncate text-center opacity-75">
                    {current?.title ?? bookTitle}
                    {current && (
                      <span className="ml-2 tabular-nums">{t("progressPercent", {percent: progressPercent})}</span>
                    )}
                  </div>
                  <button type="button" onClick={() => {setSettingsOpen(true); setTocOpen(false);}}
                          className="rounded border px-2 py-1 font-bold"
                          style={{borderColor: theme.border}}>
                    {t("settings")}
                  </button>
                </header>

                {/* 正文（可滚动；排版样式由 .book-content 全局类保证） */}
                <div ref={scrollRef} onScroll={onScroll}
                     className="relative flex-1 overflow-y-auto px-4 py-8"
                     style={{scrollBehavior: "auto"}}>
                  <div className="mx-auto max-w-2xl">
                    <h2 className="mb-6 text-center font-display text-2xl font-black">
                      {current?.title}
                    </h2>
                    {loading && current === null && (
                      <p className="text-center text-sm opacity-60">…</p>
                    )}
                    {current && (
                      <div
                        className="book-content"
                        style={{
                          ["--reader-font-size" as string]: `${prefs.fontSize}px`,
                          ["--reader-line-height" as string]: String(prefs.lineHeight),
                        }}
                        dangerouslySetInnerHTML={{__html: current.contentHtml}}
                      />
                    )}
                  </div>
                </div>

                {/* 底部 prev/next */}
                <footer
                  className="flex items-center justify-between border-t px-4 py-2.5 text-xs"
                  style={{borderColor: theme.border}}
                >
                  <button type="button" disabled={!current?.prev}
                          onClick={() => current?.prev && void gotoChapter(current.prev.chapterId)}
                          className="rounded border px-3 py-1.5 font-bold disabled:opacity-30"
                          style={{borderColor: theme.border}}>
                    ← {t("prevChapter")}
                  </button>
                  <span className="opacity-50">{t("keyboardHint")}</span>
                  <button type="button" disabled={!current?.next}
                          onClick={() => current?.next && void gotoChapter(current.next.chapterId)}
                          className="rounded border px-3 py-1.5 font-bold disabled:opacity-30"
                          style={{borderColor: theme.border}}>
                    {t("nextChapter")} →
                  </button>
                </footer>

                {/* 目录抽屉（章-节树） */}
                <AnimatePresence>
                  {tocOpen && (
                    <Drawer onClose={() => setTocOpen(false)} theme={theme} title={t("toc")}>
                      <ChapterTree chapters={chapters} theme={theme}
                                   activeId={current?.chapterId}
                                   onSelect={(id) => {
                                     setTocOpen(false);
                                     void gotoChapter(id);
                                   }} />
                    </Drawer>
                  )}
                </AnimatePresence>

                {/* 设置面板（字号/行距/主题） */}
                <AnimatePresence>
                  {settingsOpen && (
                    <Drawer onClose={() => setSettingsOpen(false)} theme={theme} title={t("settings")}>
                      <div className="flex flex-col gap-5 text-sm">
                        <div>
                          <p className="mb-2 font-bold opacity-80">{t("fontSize")}</p>
                          <div className="flex gap-2">
                            {[16, 18, 20].map((size) => (
                              <button key={size} type="button"
                                      onClick={() => persistPrefs({...prefs, fontSize: size})}
                                      className="flex-1 rounded border px-2 py-1.5 tabular-nums"
                                      style={{
                                        borderColor: theme.border,
                                        backgroundColor: prefs.fontSize === size ? theme.border : "transparent",
                                      }}>
                                {size}px
                              </button>
                            ))}
                          </div>
                        </div>
                        <div>
                          <p className="mb-2 font-bold opacity-80">{t("lineHeight")}</p>
                          <div className="flex gap-2">
                            {[1.6, 1.8, 2.0].map((value) => (
                              <button key={value} type="button"
                                      onClick={() => persistPrefs({...prefs, lineHeight: value})}
                                      className="flex-1 rounded border px-2 py-1.5 tabular-nums"
                                      style={{
                                        borderColor: theme.border,
                                        backgroundColor: prefs.lineHeight === value ? theme.border : "transparent",
                                      }}>
                                {value}
                              </button>
                            ))}
                          </div>
                        </div>
                        <div>
                          <p className="mb-2 font-bold opacity-80">{t("theme")}</p>
                          <div className="flex gap-2">
                            {(Object.keys(THEMES) as ThemeKey[]).map((key) => (
                              <button key={key} type="button"
                                      onClick={() => persistPrefs({...prefs, theme: key})}
                                      className="flex-1 rounded border px-2 py-1.5"
                                      style={{
                                        borderColor: theme.border,
                                        backgroundColor: prefs.theme === key ? theme.border : "transparent",
                                      }}>
                                {key === "paper" ? t("themePaper") : key === "green" ? t("themeGreen") : t("themeNight")}
                              </button>
                            ))}
                          </div>
                        </div>
                      </div>
                    </Drawer>
                  )}
                </AnimatePresence>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}

/**
 * 封面页（第一页：点击/→ 翻页，rotateY 过渡由 AnimatePresence 模式切换承担）
 */
function CoverPage({title, onFlip, flipHint}: {title: string; onFlip: () => void; flipHint: string}) {
  return (
    <motion.button
      type="button"
      onClick={onFlip}
      initial={{rotateY: 0}}
      exit={{rotateY: -90, opacity: 0}}
      transition={{duration: 0.45, ease: "easeInOut"}}
      className="flex h-full w-full flex-col items-center justify-center gap-6"
    >
      <span className="font-display text-5xl font-black tracking-widest sm:text-6xl">
        {title.slice(0, 1)}
      </span>
      <h2 className="font-display text-2xl font-black tracking-wide sm:text-3xl">{title}</h2>
      <span className="text-xs opacity-50">{flipHint}</span>
    </motion.button>
  );
}

/**
 * 版权声明页（第二页：文案由 ownership_type 决定；点击进入正文）
 */
function CopyrightPage({
  ownershipType,
  onFlip,
  onBack,
  title,
  text,
  flipHint,
}: {
  ownershipType: number;
  onFlip: () => void;
  onBack: () => void;
  title: string;
  text: string;
  flipHint: string;
}) {
  return (
    <motion.div
      initial={{rotateY: 90, opacity: 0}}
      animate={{rotateY: 0, opacity: 1}}
      exit={{opacity: 0}}
      transition={{duration: 0.45, ease: "easeInOut"}}
      className="flex h-full w-full flex-col items-center justify-center gap-8 px-6"
    >
      <div className="max-w-md text-center">
        <h3 className="mb-4 font-display text-lg font-black tracking-widest">{title}</h3>
        <p className="text-sm leading-loose opacity-80">{text}</p>
        <p className="mt-6 text-xs opacity-50">
          {ownershipType === 1 ? "© AETHER" : "Original Copyright"}
        </p>
      </div>
      <div className="flex gap-3 text-xs">
        <button type="button" onClick={onBack} className="rounded border px-3 py-1.5 opacity-60">
          ←
        </button>
        <button type="button" onClick={onFlip} className="rounded border px-4 py-1.5 font-bold">
          {flipHint}
        </button>
      </div>
    </motion.div>
  );
}

/**
 * 侧边抽屉（目录/设置共用；右侧滑入）
 */
function Drawer({
  title,
  onClose,
  theme,
  children,
}: {
  title: string;
  onClose: () => void;
  theme: {bg: string; text: string; muted: string; border: string};
  children: React.ReactNode;
}) {
  return (
    <>
      <motion.button
        type="button"
        aria-label="close"
        onClick={onClose}
        initial={{opacity: 0}}
        animate={{opacity: 0.4}}
        exit={{opacity: 0}}
        className="absolute inset-0 cursor-default bg-black"
      />
      <motion.aside
        initial={{x: "100%"}}
        animate={{x: 0}}
        exit={{x: "100%"}}
        transition={{type: "spring", stiffness: 320, damping: 32}}
        className="absolute right-0 top-0 flex h-full w-72 flex-col border-l"
        style={{backgroundColor: theme.bg, color: theme.text, borderColor: theme.border}}
      >
        <div className="flex items-center justify-between border-b px-4 py-3"
             style={{borderColor: theme.border}}>
          <span className="font-display text-sm font-black tracking-widest">{title}</span>
          <button type="button" onClick={onClose} className="text-sm opacity-60">✕</button>
        </div>
        <div className="flex-1 overflow-y-auto p-3">{children}</div>
      </motion.aside>
    </>
  );
}

/**
 * 章-节树（点击跳章；当前章高亮）
 */
function ChapterTree({
  chapters,
  activeId,
  onSelect,
  theme,
}: {
  chapters: ChapterNodeVO[];
  activeId?: number;
  onSelect: (chapterId: number) => void;
  theme: {bg: string; text: string; muted: string; border: string};
}) {
  return (
    <ul className="flex flex-col gap-0.5 text-sm">
      {chapters.map((chapter) => (
        <li key={chapter.id}>
          <button
            type="button"
            onClick={() => onSelect(chapter.id)}
            className="block w-full truncate rounded px-2 py-1.5 text-left font-bold"
            style={{
              backgroundColor: activeId === chapter.id ? theme.border : "transparent",
            }}
          >
            {chapter.title}
          </button>
          {(chapter.children ?? []).map((child) => (
            <button
              key={child.id}
              type="button"
              onClick={() => onSelect(child.id)}
              className="block w-full truncate rounded px-6 py-1.5 text-left text-xs"
              style={{
                backgroundColor: activeId === child.id ? theme.border : "transparent",
                color: theme.muted,
              }}
            >
              {child.title}
            </button>
          ))}
        </li>
      ))}
    </ul>
  );
}

/**
 * 取第一章 ID（目录树线性首个章）
 */
function firstChapterId(chapters: ChapterNodeVO[]): number {
  return chapters[0]?.id ?? 0;
}
