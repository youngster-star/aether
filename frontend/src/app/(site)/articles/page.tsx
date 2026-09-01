import {getLocale} from "next-intl/server";

import {apiServerGet} from "@/lib/api/server";
import type {ArticleListVO, CategoryVO, PageResult, TagVO} from "@/lib/api/types";
import ArticleCard from "@/components/article/ArticleCard";
import ArticleFilters from "@/components/article/ArticleFilters";
import BlurFade from "@/components/ui/BlurFade";

/**
 * 文章列表页（UI-Plan §6.2 / BackEnd-Plan §5.2 GET /articles）
 *
 * <p>搜索（标题/简介/内容）+ 分类/标签过滤 + 最新/热门排序；
 * 查询参数变化由 ArticleFilters 更新 URL，服务端重新渲染。</p>
 */
export default async function ArticlesPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const locale = await getLocale();
  const keyword = firstOf(params.keyword);
  const categoryId = Number(firstOf(params.categoryId) ?? 0) || undefined;
  const tagId = Number(firstOf(params.tagId) ?? 0) || undefined;
  const sort = firstOf(params.sort) ?? "latest";

  const [page, categories, tags] = await Promise.all([
    apiServerGet<PageResult<ArticleListVO>>("/articles", {
      page: 1,
      size: 20,
      keyword,
      categoryId,
      tagId,
      sort,
    }).catch(() => null),
    apiServerGet<CategoryVO[]>("/categories", {bizType: "article"}).catch(() => []),
    apiServerGet<TagVO[]>("/tags", {bizType: "article"}).catch(() => []),
  ]);

  return (
    <div className="mx-auto mt-10 max-w-6xl px-4" data-module="article">
      {/* 面包屑（UI-Plan §5 Breadcrumb：首页 / 文章；A6 下划线展开） */}
      <nav aria-label="breadcrumb" className="mb-6 text-xs text-muted">
        <ol className="flex items-center gap-2">
          <li>
            <a href="/aether/" className="crumb-link">
              {locale === "zh" ? "首页" : "Home"}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li aria-current="page" className="text-accent">
            {locale === "zh" ? "文章" : "Articles"}
          </li>
        </ol>
      </nav>

      <h1 className="section-title mb-6 text-3xl">
        {locale === "zh" ? "文章" : "Articles"}
      </h1>

      <ArticleFilters
        categories={categories}
        tags={tags}
        defaults={{keyword, categoryId, tagId, sort}}
      />

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
            {locale === "zh" ? `共 ${page.total} 篇` : `${page.total} articles`}
          </p>
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {page.records.map((article, index) => (
              <BlurFade key={article.id} delay={index * 0.05}>
                <ArticleCard article={article} />
              </BlurFade>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

/** searchParams 值可能是数组，取第一个 */
function firstOf(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
