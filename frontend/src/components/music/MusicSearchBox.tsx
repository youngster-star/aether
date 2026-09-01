"use client";

import {useState} from "react";
import {useLocale, useTranslations} from "next-intl";

import {Link} from "@/i18n/navigation";
import {apiGet, ApiError} from "@/lib/api/client";
import type {MusicListVO, PageResult} from "@/lib/api/types";

/**
 * 单曲搜索框（UI-Plan §6.6：音乐搜索独立于文章搜索，§5.2 GET /music）
 */
export default function MusicSearchBox() {
  const t = useTranslations("music");
  const locale = useLocale();
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<MusicListVO[] | null>(null);
  const [searching, setSearching] = useState(false);

  /** 执行搜索（Enter 或按钮触发；空关键词清空结果） */
  const runSearch = async () => {
    const term = keyword.trim();
    if (!term) {
      setResults(null);
      return;
    }
    setSearching(true);
    try {
      const page = await apiGet<PageResult<MusicListVO>>("/music", {keyword: term});
      setResults(page.records);
    } catch (error) {
      setResults([]);
      if (error instanceof ApiError) {
        // 业务错误（如后端未启动时 5xx）静默展示空态，输入框可重试
      }
    } finally {
      setSearching(false);
    }
  };

  return (
    <section className="mt-8">
      <form
        className="flex items-center gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          void runSearch();
        }}
        role="search"
      >
        <input
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder={t("searchPlaceholder")}
          aria-label={t("search")}
          className="w-full rounded-md border border-border bg-card px-3 py-2 text-sm outline-none
                     transition-colors focus:border-accent"
        />
        <button
          type="submit"
          disabled={searching}
          className="rounded-md border border-accent px-4 py-2 text-sm text-accent transition-colors
                     hover:bg-accent hover:text-[var(--tint-on,#F3EAD8)] disabled:opacity-50"
        >
          {t("search")}
        </button>
      </form>

      {results !== null && (
        <div className="mt-4">
          <p className="text-xs text-muted">
            {locale === "zh"
                ? `找到 ${results.length} 首曲目`
                : `${results.length} track(s) found`}
          </p>
          <ul className="mt-2 divide-y divide-border">
            {results.map((track) => (
              <li key={track.id}>
                <Link
                  href={`/music/${track.id}`}
                  className="flex items-baseline justify-between gap-3 py-2 text-sm transition-colors hover:text-accent"
                >
                  <span className="truncate">
                    {track.title}
                    {track.artist && <span className="text-muted"> · {track.artist}</span>}
                  </span>
                  <span className="tabular-nums text-xs text-muted">{formatSeconds(track.duration)}</span>
                </Link>
              </li>
            ))}
            {results.length === 0 && (
              <li className="py-2 text-sm text-muted">{t("noResults")}</li>
            )}
          </ul>
        </div>
      )}
    </section>
  );
}

/**
 * 秒 → mm:ss
 */
function formatSeconds(seconds: number): string {
  if (!seconds || seconds <= 0) {
    return "00:00";
  }
  const minutes = Math.floor(seconds / 60);
  const rest = Math.floor(seconds % 60);
  return `${String(minutes).padStart(2, "0")}:${String(rest).padStart(2, "0")}`;
}
