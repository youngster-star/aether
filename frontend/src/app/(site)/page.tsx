import {getLocale} from "next-intl/server";

import {apiServerGet} from "@/lib/api/server";
import type {AlbumListVO, ArticleListVO} from "@/lib/api/types";
import ArticleCard from "@/components/article/ArticleCard";
import AlbumCard from "@/components/album/AlbumCard";
import BlurFade from "@/components/ui/BlurFade";
import Hero from "@/components/home/Hero";
import {Link} from "@/i18n/navigation";

/**
 * 主页（UI-Plan §6.1）：Hero → 热门文章 → 最新文章 → 推荐图集
 *
 * <p>推荐图集为阶段 3 新增区块（§6.1-5，GET /albums/recommend）；
 * 公告轮播/推荐书籍/音乐区块属后续阶段（后端接口阶段 4-6 提供）。</p>
 */
export default async function HomePage() {
  const locale = await getLocale();
  const [hotArticles, latestArticles, recommendedAlbums] = await Promise.all([
    apiServerGet<ArticleListVO[]>("/articles/hot", {limit: 8}).catch(() => []),
    apiServerGet<{records: ArticleListVO[]}>("/articles", {page: 1, size: 6}).catch(() => ({
      records: [],
    })),
    apiServerGet<AlbumListVO[]>("/albums/recommend", {limit: 4}).catch(() => []),
  ]);

  return (
    <>
      <Hero locale={locale} />

      {/* 热门文章（§6.1-4：卡片网格 + hover 反色 + 排名数字 + 不显示创建时间） */}
      {hotArticles.length > 0 && (
        <section className="mx-auto mt-16 max-w-6xl px-4">
          <BlurFade>
            <h2 className="section-title text-2xl">{locale === "zh" ? "热门文章" : "Hot Articles"}</h2>
          </BlurFade>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {hotArticles.slice(0, 6).map((article, index) => (
              <BlurFade key={article.id} delay={index * 0.08}>
                <ArticleCard article={article} rank={index + 1} />
              </BlurFade>
            ))}
          </div>
        </section>
      )}

      {/* 最新文章（阶段 2 主页补充区块） */}
      {latestArticles.records.length > 0 && (
        <section className="mx-auto mt-16 max-w-6xl px-4">
          <BlurFade>
            <div className="flex items-baseline justify-between">
              <h2 className="section-title text-2xl">
                {locale === "zh" ? "最新文章" : "Latest Articles"}
              </h2>
              <Link
                href="/articles"
                className="text-sm text-accent underline-offset-4 hover:underline"
              >
                {locale === "zh" ? "查看全部 →" : "View all →"}
              </Link>
            </div>
          </BlurFade>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {latestArticles.records.map((article, index) => (
              <BlurFade key={article.id} delay={index * 0.08}>
                <ArticleCard article={article} />
              </BlurFade>
            ))}
          </div>
        </section>
      )}

      {/* 推荐图集（阶段 3 新增区块，§6.1-5：GET /albums/recommend，limit 4） */}
      {recommendedAlbums.length > 0 && (
        <section className="mx-auto mt-16 max-w-6xl px-4">
          <BlurFade>
            <div className="flex items-baseline justify-between">
              <h2 className="section-title text-2xl">
                {locale === "zh" ? "推荐图集" : "Recommended Albums"}
              </h2>
              <Link
                href="/albums"
                className="text-sm text-accent underline-offset-4 hover:underline"
              >
                {locale === "zh" ? "查看全部 →" : "View all →"}
              </Link>
            </div>
          </BlurFade>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {recommendedAlbums.slice(0, 4).map((album, index) => (
              <BlurFade key={album.id} delay={index * 0.08}>
                <AlbumCard album={album} />
              </BlurFade>
            ))}
          </div>
        </section>
      )}
    </>
  );
}
