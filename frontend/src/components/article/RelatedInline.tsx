import {useTranslations} from "next-intl";

import type {ArticleListVO} from "@/lib/api/types";
import {Link} from "@/i18n/navigation";

/**
 * 移动端相关文章区块（UI-Plan §8.2：<lg 不渲染侧边栏，
 * 正文底部"其他文章推荐"区块降级）
 */
export default function RelatedInline({articles}: {articles: ArticleListVO[]}) {
  const t = useTranslations();
  if (articles.length === 0) {
    return null;
  }
  return (
    <section className="mx-auto mt-16 max-w-3xl px-4 lg:hidden">
      <h2 className="section-title text-xl">{t("articles.relatedTitle")}</h2>
      <div className="mt-4 flex flex-col gap-3">
        {articles.map((article) => (
          <Link
            key={article.id}
            href={`/articles/${article.id}`}
            className="hover-card group block rounded-lg border border-border p-4"
          >
            <p className="font-display text-sm font-bold leading-snug">{article.title}</p>
            <p className="hover-card-dim mt-1 text-xs text-muted">
              {article.publishTime?.slice(0, 10)} · {t("common.viewCount", {count: article.readingCount})}
            </p>
          </Link>
        ))}
      </div>
    </section>
  );
}
