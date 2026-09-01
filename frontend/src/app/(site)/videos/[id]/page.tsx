import {getLocale, getTranslations} from "next-intl/server";
import {notFound} from "next/navigation";

import {apiServerGet} from "@/lib/api/server";
import type {VideoDetailVO} from "@/lib/api/types";
import VideoDetailClient from "@/components/video/VideoDetailClient";

/**
 * 视频详情页（UI-Plan §6.5 / BackEnd-Plan §5.2 GET /videos/{id}）
 *
 * <p>ArtPlayer 播放器（倍速/清晰度/画中画）+ 关键时间节点右侧列表点击跳转；
 * 播放源为签名 URL（ProtectedMedia，页面不暴露原始存储地址）。</p>
 */
export default async function VideoDetailPage({params}: {params: Promise<{id: string}>}) {
  const {id} = await params;
  const videoId = Number(id);
  const locale = await getLocale();
  const t = await getTranslations("videos");

  if (!Number.isInteger(videoId) || videoId <= 0) {
    notFound();
  }
  const video = await apiServerGet<VideoDetailVO>(`/videos/${videoId}`).catch(() => null);
  if (video === null) {
    notFound();
  }

  return (
    <div className="mx-auto mt-10 max-w-6xl px-4" data-module="article">
      {/* 面包屑：首页 / 视频 / 标题（A6 下划线展开） */}
      <nav aria-label="breadcrumb" className="mb-8 text-xs text-muted">
        <ol className="flex flex-wrap items-center gap-2">
          <li>
            <a href="/aether/" className="crumb-link">
              {locale === "zh" ? "首页" : "Home"}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li>
            <a href="/aether/videos" className="crumb-link">
              {t("breadcrumbVideos")}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li aria-current="page" className="line-clamp-1 max-w-[40vw] text-accent">
            {video.title}
          </li>
        </ol>
      </nav>

      {/* 标题区 */}
      <header className="mb-8">
        <h1 className="font-display text-3xl font-black leading-snug tracking-wide sm:text-4xl">
          {video.title}
        </h1>
        <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-muted">
          <span className="rounded-pill border border-accent-2 px-2 py-0.5 text-accent-2">
            {t("duration")}: {formatDuration(video.duration)}
          </span>
        </div>
        {video.intro && (
          <p className="mt-4 max-w-3xl border-l-2 border-accent pl-4 text-sm leading-relaxed text-muted">
            {video.intro}
          </p>
        )}
      </header>

      {/* 播放器 + 章节列表（客户端组件） */}
      <VideoDetailClient video={video} />
    </div>
  );
}

/**
 * 秒 → mm:ss / h:mm:ss（服务端渲染用）
 */
function formatDuration(seconds: number): string {
  if (!seconds || seconds <= 0) {
    return "00:00";
  }
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = Math.floor(seconds % 60);
  const pad = (value: number) => String(value).padStart(2, "0");
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${pad(minutes)}:${pad(rest)}`;
}
