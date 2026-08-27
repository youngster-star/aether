"use client";

import {useEffect, useRef, useState} from "react";
import Artplayer from "artplayer";
import {useLocale, useTranslations} from "next-intl";

import type {VideoDetailVO} from "@/lib/api/types";

/**
 * 视频详情交互层（UI-Plan §6.5）
 *
 * <p>ArtPlayer 集成：倍速/清晰度（单源原画）/画中画/全屏由播放器内建设置面板提供；
 * 关键时间节点为右侧章节列表，点击 seek 跳转，播放中按当前进度高亮；
 * 播放源为后端签名 URL（ProtectedMedia 语义：页面不暴露原始存储地址）。
 * 占位数据（seed 无轨 MP4）播放失败时展示友好提示。</p>
 */
export default function VideoDetailClient({video}: {video: VideoDetailVO}) {
  const locale = useLocale();
  const t = useTranslations("videos");
  const containerRef = useRef<HTMLDivElement>(null);
  const artRef = useRef<Artplayer | null>(null);
  const [playFailed, setPlayFailed] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }
    const art = new Artplayer({
      container,
      url: video.playUrl,
      type: "mp4",
      autoplay: false,
      poster: video.coverUrl ?? undefined,
      lang: locale === "zh" ? "zh-cn" : "en",
      pip: true,
      setting: true,
      playbackRate: true,
      aspectRatio: true,
      fullscreen: true,
      fullscreenWeb: true,
      autoMini: true,
      // 清晰度：单源原画（后端当前单文件版本；多版本清晰度待管理端扩展）
      quality: [{default: true, html: locale === "zh" ? "原画" : "Original", url: video.playUrl}],
    });
    artRef.current = art;
    const onError = () => setPlayFailed(true);
    const onTimeUpdate = (event: Event) => {
      const element = event.target as HTMLVideoElement | null;
      if (element && Number.isFinite(element.currentTime)) {
        setCurrentTime(element.currentTime);
      }
    };
    art.on("video:error", onError);
    art.on("video:timeupdate", onTimeUpdate);
    return () => {
      art.off("video:error", onError);
      art.off("video:timeupdate", onTimeUpdate);
      art.destroy(false);
      artRef.current = null;
    };
  }, [video.id, video.playUrl, video.coverUrl, locale]);

  /** 章节点击跳转 */
  const seekTo = (timeOffset: number) => {
    const art = artRef.current;
    if (art) {
      art.seek = timeOffset;
      art.play();
    }
  };

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
      {/* 播放器容器（16:9 自适应） */}
      <div className="overflow-hidden rounded-md border border-border bg-black shadow-aether">
        <div ref={containerRef} className="aspect-video w-full" />
        {playFailed && (
          <p className="border-t border-border bg-background px-4 py-3 text-sm text-muted">
            {t("playError")}
          </p>
        )}
      </div>

      {/* 关键时间节点（UI-Plan §6.5 右侧列表点击跳转） */}
      <aside aria-label={t("chapters")} className="flex flex-col gap-1">
        <h2 className="font-display text-sm font-black tracking-widest text-muted uppercase">
          {t("chapters")}
        </h2>
        {video.chapters.length === 0 ? (
          <p className="mt-4 text-xs text-muted">{t("noChapters")}</p>
        ) : (
          <ol className="mt-2 flex max-h-[420px] flex-col gap-1 overflow-y-auto lg:max-h-none">
            {video.chapters.map((chapter) => {
              const active = chapter.timeOffset <= currentTime;
              return (
                <li key={chapter.id}>
                  <button
                    type="button"
                    onClick={() => seekTo(chapter.timeOffset)}
                    className={`group flex w-full items-center gap-3 rounded-md border px-3 py-2
                                text-left text-sm transition-colors
                                ${active ? "border-accent bg-accent/10" : "border-border hover:border-accent"}`}
                  >
                    <span
                      className={`text-xs tabular-nums ${active ? "text-accent" : "text-muted"}`}
                    >
                      {formatDuration(chapter.timeOffset)}
                    </span>
                    <span className="truncate">{chapter.title}</span>
                  </button>
                </li>
              );
            })}
          </ol>
        )}
      </aside>
    </div>
  );
}

/**
 * 秒 → mm:ss / h:mm:ss
 */
function formatDuration(seconds: number): string {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = Math.floor(seconds % 60);
  const pad = (value: number) => String(value).padStart(2, "0");
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${pad(minutes)}:${pad(rest)}`;
}
