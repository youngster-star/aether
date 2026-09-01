"use client";

import {useTranslations} from "next-intl";

import type {AlbumListVO} from "@/lib/api/types";
import {Link} from "@/i18n/navigation";
import ProtectedImage from "@/components/media/ProtectedImage";

/**
 * 图集卡片（UI-Plan §6.4 lens 语义：封面 + 题目 + 简介，去按钮点击跳详情）
 *
 * <p>magicui lens 放大镜自实现简化版：hover 封面缓慢放大 + 反色遮罩，
 * 卡身 hover 反色（§2.6 .hover-card）。</p>
 */
export default function AlbumCard({album}: {album: AlbumListVO}) {
  const t = useTranslations("albums");
  return (
    <Link href={`/albums/${album.id}`} className="block">
      <article
        data-module="album"
        className="hover-card group relative h-full overflow-hidden rounded-lg border border-border shadow-aether"
      >
        {/* 封面区（4:3，lens 放大语义：.card-img hover scale 1.04 + 分区色薄纱罩） */}
        <div className="card-img relative aspect-[4/3] bg-accent/10">
          {album.coverUrl && (
            <div className="h-full w-full">
              <ProtectedImage src={album.coverUrl} alt={album.title} />
            </div>
          )}
          <span className="pointer-events-none absolute right-2 top-2 rounded-pill bg-background/70 px-2 py-0.5 text-xs text-muted backdrop-blur-sm">
            {t("photoCount", {count: album.imageCount})}
          </span>
        </div>
        {/* 文字区 */}
        <div className="p-4">
          <h3 className="font-display text-lg font-black leading-snug tracking-wide">
            {album.title}
          </h3>
          {album.intro && (
            <p className="hover-card-dim mt-2 line-clamp-2 text-sm text-muted">{album.intro}</p>
          )}
        </div>
      </article>
    </Link>
  );
}
