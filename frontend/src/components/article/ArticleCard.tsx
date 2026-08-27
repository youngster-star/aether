"use client";

import {useTranslations} from "next-intl";

import type {ArticleListVO} from "@/lib/api/types";
import {Link} from "@/i18n/navigation";

/**
 * 文章卡片（UI-Plan §6.2）：标题 + 简介 + 分类标签 + 阅读数（无创建时间）；
 * hover 整卡反色（§2.6 .hover-card）；封面（可选，ProtectedImage 防下载）
 */
export default function ArticleCard({article, rank}: {article: ArticleListVO; rank?: number}) {
  const t = useTranslations();
  return (
    <Link href={`/articles/${article.id}`} className="block">
      <article className="hover-card group relative h-full overflow-hidden rounded-lg border border-border p-6 shadow-aether">
        {/* 热门排名数字（主页热门区块，kinetic-text 语义以字重渐变呈现） */}
        {rank !== undefined && (
          <span
            aria-hidden
            className="absolute -right-2 -top-4 font-display text-8xl font-black leading-none
                       text-accent opacity-15 transition-opacity group-hover:opacity-30"
          >
            {rank}
          </span>
        )}
        <h3 className="relative font-display text-xl font-black leading-snug tracking-wide">
          {article.title}
        </h3>
        {article.summary && (
          <p className="hover-card-dim relative mt-3 line-clamp-3 text-sm text-muted">
            {article.summary}
          </p>
        )}
        <div className="relative mt-4 flex flex-wrap items-center gap-2">
          {article.categories.map((category) => (
            <span
              key={category.id}
              className="rounded-pill border border-accent-2 px-2 py-0.5 text-xs text-accent-2"
            >
              {category.name}
            </span>
          ))}
          {article.tags.slice(0, 3).map((tag) => (
            <span key={tag.id} className="text-xs text-muted hover-card-dim">
              #{tag.name}
            </span>
          ))}
          <span className="ml-auto text-xs text-muted hover-card-dim">
            {t("common.viewCount", {count: article.readingCount})}
          </span>
        </div>
      </article>
    </Link>
  );
}
