"use client";

import type {MusicAlbumListVO} from "@/lib/api/types";
import {Link} from "@/i18n/navigation";
import ProtectedImage from "@/components/media/ProtectedImage";
import {useTranslations} from "next-intl";

/**
 * 音乐合集卡片（UI-Plan §6.6 合集页）：封面 + 标题 + 类型（自定义/固定认证）
 * + 曲目数，hover 整卡反色（T2，钢笔墨水蓝 data-module="music"）
 */
export default function MusicAlbumCard({album}: {album: MusicAlbumListVO}) {
  const t = useTranslations("music");
  return (
    <Link href={`/music/${album.id}`} className="block">
      <article
        data-module="music"
        className="hover-card group relative h-full overflow-hidden rounded-lg border border-border shadow-aether"
      >
        {/* 封面（4:3）+ 类型角标（.card-img hover scale + 分区色薄纱罩） */}
        <div className="card-img relative aspect-[4/3] bg-accent/10">
          {album.coverUrl && (
            <div className="h-full w-full">
              <ProtectedImage src={album.coverUrl} alt={album.title} />
            </div>
          )}
          <span className="absolute left-2 top-2 rounded-pill border border-white/30 bg-black/50 px-2 py-0.5
                           text-xs text-white backdrop-blur-sm">
            {album.type === 2 ? t("certifiedAlbums") : t("customAlbums")}
          </span>
        </div>
        <div className="p-4">
          <h3 className="truncate font-display text-lg font-black leading-snug tracking-wide">
            {album.title}
          </h3>
          {album.certification && (
            <p className="mt-1 line-clamp-1 text-xs opacity-80" title={album.certification}>
              {t("certification")} · {album.certification}
            </p>
          )}
          {album.intro && (
            <p className="hover-card-dim mt-2 line-clamp-2 text-sm text-muted">{album.intro}</p>
          )}
          <p className="mt-2 text-xs tabular-nums opacity-75">
            {t("trackCount", {count: album.trackCount})}
          </p>
        </div>
      </article>
    </Link>
  );
}
