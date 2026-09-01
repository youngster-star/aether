import {getTranslations} from "next-intl/server";
import {notFound} from "next/navigation";

import {apiServerGet} from "@/lib/api/server";
import type {MusicAlbumDetailVO} from "@/lib/api/types";
import ProtectedImage from "@/components/media/ProtectedImage";

/**
 * 合集详情页（UI-Plan §4.1 music/albums/[id] / §6.6 / BackEnd-Plan §5.2 GET /music/albums/{id}）
 *
 * <p>合集头（封面 + 认证信息展示）+ 曲目列表；曲目行点击进入单曲详情页播放
 * （/music/{trackId}，全局 Audio 单例在单曲页接管播放）。</p>
 */
export default async function MusicAlbumDetailPage({params}: {params: Promise<{id: string}>}) {
  const {id} = await params;
  const albumId = Number(id);
  const t = await getTranslations("music");

  if (!Number.isInteger(albumId) || albumId <= 0) {
    notFound();
  }
  const album = await apiServerGet<MusicAlbumDetailVO>(`/music/albums/${albumId}`).catch(() => null);
  if (album === null) {
    notFound();
  }

  return (
    <div className="mx-auto mt-10 max-w-6xl px-4" data-module="music">
      {/* 面包屑：首页 / 音乐 / 合集标题（A6 下划线展开） */}
      <nav aria-label="breadcrumb" className="mb-8 text-xs text-muted">
        <ol className="flex flex-wrap items-center gap-2">
          <li>
            <a href="/aether/" className="crumb-link">
              {t("breadcrumbHome")}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li>
            <a href="/aether/music" className="crumb-link">
              {t("breadcrumbMusic")}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li aria-current="page" className="line-clamp-1 max-w-[40vw] text-accent">
            {album.title}
          </li>
        </ol>
      </nav>

      {/* 合集头：封面 + 标题 + 认证/介绍 */}
      <header className="mb-8 flex flex-col gap-6 sm:flex-row sm:items-start">
        <div className="aspect-square w-40 shrink-0 overflow-hidden rounded-lg border border-border
                        bg-accent/10 shadow-aether sm:w-52">
          {album.coverUrl && (
            <ProtectedImage src={album.coverUrl} alt={album.title} />
          )}
        </div>
        <div className="min-w-0">
          <h1 className="font-display text-3xl font-black leading-snug tracking-wide sm:text-4xl">
            {album.title}
          </h1>
          <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-muted">
            <span className="rounded-pill border border-accent-2 px-2 py-0.5 text-accent-2">
              {album.type === 2 ? t("certifiedAlbums") : t("customAlbums")}
            </span>
            <span className="tabular-nums">{t("trackCount", {count: album.tracks.length})}</span>
          </div>
          {album.certification && (
            <p className="mt-3 inline-block rounded-md border border-dashed border-accent-2/60
                          bg-accent/5 px-3 py-1.5 text-xs text-accent-2">
              {t("certification")} · {album.certification}
            </p>
          )}
          {album.intro && (
            <p className="mt-4 max-w-3xl border-l-2 border-accent pl-4 text-sm leading-relaxed text-muted">
              {album.intro}
            </p>
          )}
        </div>
      </header>

      {/* 曲目列表（行 hover 分区色反色，点击进入单曲详情播放） */}
      <ol className="divide-y divide-border overflow-hidden rounded-lg border border-border">
        {album.tracks.map((track, index) => (
          <li key={track.id}>
            <a
              href={`/aether/music/${track.id}`}
              data-module="music"
              className="group flex items-baseline gap-4 px-4 py-3 transition-colors
                         hover:bg-[var(--tint)] hover:text-[var(--tint-on)]"
            >
              <span className="w-6 shrink-0 text-xs tabular-nums opacity-60">{index + 1}</span>
              <span className="min-w-0 flex-1 truncate text-sm font-bold">
                {track.title}
                {track.artist && (
                  <span className="ml-2 text-xs font-normal opacity-70">{track.artist}</span>
                )}
              </span>
              <span className="shrink-0 text-xs tabular-nums opacity-70">
                {formatSeconds(track.duration)}
              </span>
            </a>
          </li>
        ))}
        {album.tracks.length === 0 && (
          <li className="px-4 py-8 text-center text-sm text-muted">{t("empty")}</li>
        )}
      </ol>
    </div>
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
