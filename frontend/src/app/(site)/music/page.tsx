import {getLocale, getTranslations} from "next-intl/server";

import {apiServerGet} from "@/lib/api/server";
import type {MusicAlbumListVO, PageResult} from "@/lib/api/types";
import MusicAlbumCard from "@/components/music/MusicAlbumCard";
import MusicSearchBox from "@/components/music/MusicSearchBox";
import BlurFade from "@/components/ui/BlurFade";

/**
 * 音乐列表页（UI-Plan §6.6 合集页 / BackEnd-Plan §5.2 GET /music/albums）
 *
 * <p>自定义合集与固定合集（含认证信息）分组展示；单曲搜索独立于文章搜索；
 * 卡片 hover 整卡反色（T2，钢笔墨水蓝），blur-fade 级联入场。</p>
 */
export default async function MusicPage() {
  const t = await getTranslations("music");
  const locale = await getLocale();

  const page = await apiServerGet<PageResult<MusicAlbumListVO>>("/music/albums", {
    page: 1,
    size: 24,
  }).catch(() => null);

  const customAlbums = page?.records.filter((album) => album.type === 1) ?? [];
  const certifiedAlbums = page?.records.filter((album) => album.type === 2) ?? [];

  return (
    <div className="mx-auto mt-10 max-w-6xl px-4" data-module="music">
      {/* 面包屑：首页 / 音乐（A6 下划线展开） */}
      <nav aria-label="breadcrumb" className="mb-6 text-xs text-muted">
        <ol className="flex items-center gap-2">
          <li>
            <a href="/aether/" className="crumb-link">
              {locale === "zh" ? "首页" : "Home"}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li aria-current="page" className="text-accent">
            {t("listTitle")}
          </li>
        </ol>
      </nav>

      <h1 className="section-title mb-6 text-3xl">{t("listTitle")}</h1>

      {page === null ? (
        <p className="mt-16 text-center text-sm text-muted">
          {locale === "zh" ? "加载失败，请确认后端已启动" : "Failed to load, check backend"}
        </p>
      ) : (
        <>
          {/* 固定合集（认证专辑） */}
          {certifiedAlbums.length > 0 && (
            <section className="mt-4">
              <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted">
                {t("certifiedAlbums")}
              </h2>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {certifiedAlbums.map((album, index) => (
                  <BlurFade key={album.id} delay={index * 0.05}>
                    <MusicAlbumCard album={album} />
                  </BlurFade>
                ))}
              </div>
            </section>
          )}

          {/* 自定义合集 */}
          {customAlbums.length > 0 && (
            <section className="mt-10">
              <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted">
                {t("customAlbums")}
              </h2>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {customAlbums.map((album, index) => (
                  <BlurFade key={album.id} delay={index * 0.05}>
                    <MusicAlbumCard album={album} />
                  </BlurFade>
                ))}
              </div>
            </section>
          )}

          {page.records.length === 0 && (
            <p className="mt-16 text-center text-sm text-muted">{t("empty")}</p>
          )}

          {/* 单曲搜索（独立于文章搜索，§5.2 仅音乐可搜索） */}
          <MusicSearchBox />
        </>
      )}
    </div>
  );
}
