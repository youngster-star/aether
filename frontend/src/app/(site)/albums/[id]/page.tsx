import {getLocale, getTranslations} from "next-intl/server";
import {notFound} from "next/navigation";

import {apiServerGet} from "@/lib/api/server";
import {apiGet} from "@/lib/api/client";
import type {AlbumDetailVO} from "@/lib/api/types";
import AlbumDetailClient from "@/components/album/AlbumDetailClient";
import ProtectedImage from "@/components/media/ProtectedImage";
import BlurFade from "@/components/ui/BlurFade";

/**
 * 图集详情页（UI-Plan §6.4 / BackEnd-Plan §5.2 GET /albums/{id}）
 *
 * <p>封面头部 + 介绍 + 图片瀑布流（columns-2 sm:columns-3，图片 blur-fade
 * 入场 + pixel-image 懒加载）+ 点击预览层（防下载，悬浮显示大小等参数）。</p>
 */
export default async function AlbumDetailPage({params}: {params: Promise<{id: string}>}) {
  const {id} = await params;
  const albumId = Number(id);
  const locale = await getLocale();
  const t = await getTranslations("albums");

  if (!Number.isInteger(albumId) || albumId <= 0) {
    notFound();
  }
  const album = await apiServerGet<AlbumDetailVO>(`/albums/${albumId}`).catch(() => null);
  if (album === null) {
    notFound();
  }

  return (
    <div className="mx-auto mt-10 max-w-6xl px-4">
      {/* 面包屑：首页 / 图集 / 标题（UI-Plan §6.4） */}
      <nav aria-label="breadcrumb" className="mb-8 text-xs text-muted">
        <ol className="flex flex-wrap items-center gap-2">
          <li>
            <a href="/aether/" className="hover:text-accent">
              {locale === "zh" ? "首页" : "Home"}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li>
            <a href="/aether/albums" className="hover:text-accent">
              {t("breadcrumbAlbums")}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li aria-current="page" className="line-clamp-1 max-w-[40vw] text-accent">
            {album.title}
          </li>
        </ol>
      </nav>

      {/* 封面头部 + 介绍 */}
      <header className="mb-10">
        <h1 className="font-display text-3xl font-black leading-snug tracking-wide sm:text-4xl">
          {album.title}
        </h1>
        {album.intro && (
          <p className="mt-4 max-w-3xl border-l-2 border-accent pl-4 text-sm leading-relaxed text-muted">
            {album.intro}
          </p>
        )}
        {album.coverUrl && (
          <BlurFade className="mt-8 overflow-hidden rounded-md border border-border shadow-aether">
            <div className="max-h-[420px]">
              {/* 签名过期（§9.2）客户端重拉详情换新签名 URL */}
              <ProtectedImage
                src={album.coverUrl}
                alt={album.title}
                refresh={async () =>
                  (await apiGet<AlbumDetailVO>(`/albums/${albumId}`).catch(() => null))?.coverUrl ??
                  null
                }
              />
            </div>
          </BlurFade>
        )}
      </header>

      {/* 瀑布流 + 预览层 */}
      <AlbumDetailClient albumId={album.id} images={album.images} />

      <p className="mt-12 border-t border-border pt-6 text-center text-xs text-muted">
        {locale === "zh" ? "创建于" : "Created"} {album.createTime?.slice(0, 16)}
      </p>
    </div>
  );
}
