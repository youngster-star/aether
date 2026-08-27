import {getLocale, getTranslations} from "next-intl/server";
import {notFound} from "next/navigation";

import {apiServerGet} from "@/lib/api/server";
import type {ArticleDetailVO, ArticleListVO, ArticleStyleConfig} from "@/lib/api/types";
import ArticleContent from "@/components/article/ArticleContent";
import ArticleDetailClient from "@/components/article/ArticleDetailClient";
import RelatedInline from "@/components/article/RelatedInline";
import RelatedPanel from "@/components/article/RelatedPanel";
import StickyTitleBar from "@/components/article/StickyTitleBar";

/**
 * 文章详情页（UI-Plan §6.3 / BackEnd-Plan §5.2 GET /articles/{id}）
 *
 * <p>标题 → 分类/标签/阅读数 → 封面 → 正文；吸顶标题栏（滚动 5-8%）；
 * RELATED ARTICLES（桌面右侧竖排按钮 + 抽屉 / 移动端底部区块）。
 * 阅读计数由浏览器 hydrate 补偿（SSR 请求带 X-Aether-No-Count）。</p>
 */
export default async function ArticleDetailPage({params}: {params: Promise<{id: string}>}) {
  const {id} = await params;
  const articleId = Number(id);
  const locale = await getLocale();
  const t = await getTranslations("articles");

  if (!Number.isInteger(articleId) || articleId <= 0) {
    notFound();
  }
  const [article, related] = await Promise.all([
    apiServerGet<ArticleDetailVO>(`/articles/${articleId}`, undefined, {noCount: true})
      .catch(() => null),
    apiServerGet<ArticleListVO[]>(`/articles/${articleId}/related`).catch(() => []),
  ]);
  if (article === null) {
    notFound();
  }
  // style_json 反序列化（结构见 BackEnd-Plan §6.3）
  let styleConfig: ArticleStyleConfig | null = null;
  if (article.style?.styleJson) {
    try {
      styleConfig = JSON.parse(article.style.styleJson) as ArticleStyleConfig;
    } catch {
      styleConfig = null;
    }
  }

  return (
    <div className="relative">
      <StickyTitleBar title={article.title} />

      <div className="mx-auto mt-10 max-w-6xl px-4">
        {/* 面包屑：首页 / 文章 / 标题（UI-Plan §5） */}
        <nav aria-label="breadcrumb" className="mb-8 text-xs text-muted">
          <ol className="flex flex-wrap items-center gap-2">
            <li>
              <a href="/aether/" className="hover:text-accent">
                {t("breadcrumbHome")}
              </a>
            </li>
            <li aria-hidden>·</li>
            <li>
              <a href="/aether/articles" className="hover:text-accent">
                {t("breadcrumbArticles")}
              </a>
            </li>
            <li aria-hidden>·</li>
            <li aria-current="page" className="line-clamp-1 max-w-[40vw] text-accent">
              {article.title}
            </li>
          </ol>
        </nav>

        {/* 标题区 */}
        <header className="mx-auto max-w-3xl">
          <h1 className="font-display text-3xl font-black leading-snug tracking-wide sm:text-4xl">
            {article.title}
          </h1>
          <div className="mt-4 flex flex-wrap items-center gap-2">
            {article.categories.map((category) => (
              <span
                key={category.id}
                className="rounded-pill border border-accent-2 px-2 py-0.5 text-xs text-accent-2"
              >
                {category.name}
              </span>
            ))}
            {article.tags.map((tag) => (
              <span key={tag.id} className="text-xs text-muted">
                #{tag.name}
              </span>
            ))}
            <span className="ml-auto text-xs text-muted">
              {article.wordCount} 字 ·{" "}
              <ArticleDetailClient id={article.id} initialReadingCount={article.readingCount} /> 次阅读
            </span>
          </div>
          {article.summary && (
            <p className="mt-4 border-l-2 border-accent pl-4 text-sm leading-relaxed text-muted">
              {article.summary}
            </p>
          )}
        </header>

        {/* 封面（可选；ProtectedImage 防下载阶段 3 图集时统一接入） */}
        {article.coverUrl && (
          // 签名 URL 动态生成（含 expires/sign），next/image 优化器会重写 URL 破坏签名，
          // 故用原生 img（签名过期刷新与防下载拦截由阶段 3 ProtectedImage 统一处理）
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={article.coverUrl}
            alt={article.title}
            className="mx-auto mt-8 max-h-[480px] w-auto rounded-md"
            draggable={false}
            onContextMenu={(event) => event.preventDefault()}
          />
        )}

        {/* 正文（独立样式注入 + DOMPurify） */}
        <div className="mt-10">
          <ArticleContent html={article.contentHtml} style={styleConfig} />
        </div>

        {/* 发布/更新时间 */}
        <p className="mx-auto mt-12 max-w-3xl border-t border-border pt-6 text-center text-xs text-muted">
          {locale === "zh" ? "发布于" : "Published"} {article.publishTime?.slice(0, 16)} ·{" "}
          {locale === "zh" ? "更新于" : "Updated"} {article.updateTime?.slice(0, 16)}
        </p>
      </div>

      {/* 移动端：正文底部相关区块 */}
      <RelatedInline articles={related} />

      {/* 桌面端：右侧竖排按钮 + 抽屉 */}
      <RelatedPanel articles={related} />
    </div>
  );
}
