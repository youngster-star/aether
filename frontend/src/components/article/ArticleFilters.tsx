"use client";

import {useTranslations} from "next-intl";
import {useRouter} from "@/i18n/navigation";
import {useState} from "react";

import type {CategoryVO, TagVO} from "@/lib/api/types";

/**
 * 文章列表筛选栏（UI-Plan §6.2）：搜索框 + 分类/标签下拉 + 最新/热门排序
 *
 * <p>提交即更新 URL searchParams，由服务端列表页重新拉数据（RSC 模式）。</p>
 */
export default function ArticleFilters({
  categories,
  tags,
  defaults,
}: {
  categories: CategoryVO[];
  tags: TagVO[];
  defaults: {keyword?: string; categoryId?: number; tagId?: number; sort?: string};
}) {
  const t = useTranslations("articles");
  const router = useRouter();
  const [keyword, setKeyword] = useState(defaults.keyword ?? "");
  const [categoryId, setCategoryId] = useState(defaults.categoryId ?? 0);
  const [tagId, setTagId] = useState(defaults.tagId ?? 0);
  const [sort, setSort] = useState(defaults.sort ?? "latest");

  const apply = () => {
    const params = new URLSearchParams();
    if (keyword.trim()) params.set("keyword", keyword.trim());
    if (categoryId > 0) params.set("categoryId", String(categoryId));
    if (tagId > 0) params.set("tagId", String(tagId));
    params.set("sort", sort);
    const query = params.toString();
    router.push(`/articles${query ? `?${query}` : ""}`);
  };

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        apply();
      }}
      className="flex flex-col gap-3"
    >
      {/* 搜索行 */}
      <div className="flex gap-2">
        <input
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder={t("searchPlaceholder")}
          className="h-11 flex-1 rounded-lg border border-border bg-card px-4 text-sm
                     outline-none placeholder:text-muted focus:border-accent"
        />
        <button
          type="submit"
          className="h-11 rounded-lg bg-primary px-5 text-sm font-bold text-background
                     transition-colors hover:bg-accent"
        >
          {t("search")}
        </button>
      </div>
      {/* 筛选行 */}
      <div className="flex flex-wrap items-center gap-2">
        <select
          aria-label={t("filterByCategory")}
          value={categoryId}
          onChange={(event) => setCategoryId(Number(event.target.value))}
          className="h-9 rounded-pill border border-border bg-card px-3 text-xs outline-none"
        >
          <option value={0}>{t("all")} · {t("filterByCategory")}</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        <select
          aria-label={t("filterByTag")}
          value={tagId}
          onChange={(event) => setTagId(Number(event.target.value))}
          className="h-9 rounded-pill border border-border bg-card px-3 text-xs outline-none"
        >
          <option value={0}>{t("all")} · {t("filterByTag")}</option>
          {tags.map((tag) => (
            <option key={tag.id} value={tag.id}>
              {tag.name}
            </option>
          ))}
        </select>
        <div className="ml-auto flex overflow-hidden rounded-pill border border-border">
          {(
            [
              {value: "latest", label: t("latest")},
              {value: "hot", label: t("hot")},
            ] as const
          ).map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => {
                setSort(option.value);
                // 排序切换即时生效（保留当前筛选条件）
                const params = new URLSearchParams(window.location.search);
                params.set("sort", option.value);
                router.push(`/articles?${params.toString()}`);
              }}
              className={`h-9 px-4 text-xs transition-colors ${
                sort === option.value
                  ? "bg-accent text-background"
                  : "bg-card hover:bg-accent hover:text-background"
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>
    </form>
  );
}
