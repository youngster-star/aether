import {getLocale, getTranslations} from "next-intl/server";

import {apiServerGet} from "@/lib/api/server";
import type {AlbumListVO, PageResult} from "@/lib/api/types";
import AlbumCard from "@/components/album/AlbumCard";
import BlurFade from "@/components/ui/BlurFade";

/**
 * 图集列表页（UI-Plan §6.4 / BackEnd-Plan §5.2 GET /albums）
 *
 * <p>Lens 卡片网格 + blur-fade 级联入场；keyword 搜索接口已就绪
 * （后端支持），搜索 UI 待管理端配套后统一补充。</p>
 */
export default async function AlbumsPage() {
  const t = await getTranslations("albums");
  const locale = await getLocale();

  const page = await apiServerGet<PageResult<AlbumListVO>>("/albums", {
    page: 1,
    size: 24,
  }).catch(() => null);

  return (
    <div className="mx-auto mt-10 max-w-6xl px-4" data-module="album">
      {/* 面包屑：首页 / 图集（A6 下划线展开） */}
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
      ) : page.records.length === 0 ? (
        <p className="mt-16 text-center text-sm text-muted">
          {locale === "zh" ? "暂无内容" : "Nothing here yet"}
        </p>
      ) : (
        <>
          <p className="mt-6 text-xs text-muted">
            {locale === "zh" ? `共 ${page.total} 个图集` : `${page.total} albums`}
          </p>
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {page.records.map((album, index) => (
              <BlurFade key={album.id} delay={index * 0.05}>
                <AlbumCard album={album} />
              </BlurFade>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
