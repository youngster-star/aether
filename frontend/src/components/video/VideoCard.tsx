"use client";

import type {VideoListVO} from "@/lib/api/types";
import {Link} from "@/i18n/navigation";
import ProtectedImage from "@/components/media/ProtectedImage";

/**
 * 视频卡片（UI-Plan §6.5）：封面 + 标题 + 时长（后端探测），hover 整卡反色
 */
export default function VideoCard({video}: {video: VideoListVO}) {
  return (
    <Link href={`/videos/${video.id}`} className="block">
      <article
        data-module="article"
        className="hover-card group relative h-full overflow-hidden rounded-lg border border-border shadow-aether"
      >
        {/* 封面（16:9）+ 时长角标（.card-img hover scale + 分区色薄纱罩） */}
        <div className="card-img relative aspect-video bg-accent/10">
          {video.coverUrl && (
            <div className="h-full w-full">
              <ProtectedImage src={video.coverUrl} alt={video.title} />
            </div>
          )}
          <span className="absolute bottom-2 right-2 rounded-pill bg-black/60 px-2 py-0.5 text-xs
                           tabular-nums text-white backdrop-blur-sm">
            {formatDuration(video.duration)}
          </span>
        </div>
        <div className="p-4">
          <h3 className="truncate font-display text-lg font-black leading-snug tracking-wide">
            {video.title}
          </h3>
          {video.intro && (
            <p className="hover-card-dim mt-2 line-clamp-2 text-sm text-muted">{video.intro}</p>
          )}
        </div>
      </article>
    </Link>
  );
}

/**
 * 秒 → mm:ss / h:mm:ss
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
