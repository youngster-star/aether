import {getTranslations} from "next-intl/server";

import {apiServerGet} from "@/lib/api/server";
import type {BookListVO, PageResult} from "@/lib/api/types";
import BookCard from "@/components/book/BookCard";
import BlurFade from "@/components/ui/BlurFade";

/**
 * 书籍列表页（UI-Plan §6.7 列表 / BackEnd-Plan §5.2 GET /books）
 *
 * <p>推荐书籍置顶分组，其余按 id 降序；卡片 hover T2 反色（铜绿），
 * data-module="book"；B1/B2/B6 + blur-fade 级联入场。</p>
 */
export default async function BooksPage() {
  const t = await getTranslations("book");
  const tCommon = await getTranslations("common");

  const page = await apiServerGet<PageResult<BookListVO>>("/books", {
    page: 1,
    size: 48,
  }).catch(() => null);

  const recommended = page?.records.filter((book) => book.isRecommend === 1) ?? [];
  const others = page?.records.filter((book) => book.isRecommend !== 1) ?? [];

  return (
    <div className="mx-auto mt-10 max-w-6xl px-4" data-module="book">
      {/* 面包屑：首页 / 书籍（A6 下划线展开） */}
      <nav aria-label="breadcrumb" className="mb-6 text-xs text-muted">
        <ol className="flex items-center gap-2">
          <li>
            <a href="/aether/" className="crumb-link">
              {t("breadcrumbHome")}
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
        <p className="mt-16 text-center text-sm text-muted">{tCommon("loadError")}</p>
      ) : (
        <>
          {recommended.length > 0 && (
            <section className="mt-4">
              <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted">
                {t("recommended")}
              </h2>
              <div className="grid gap-4 sm:grid-cols-3 lg:grid-cols-4">
                {recommended.map((book, index) => (
                  <BlurFade key={book.id} delay={index * 0.05}>
                    <BookCard book={book} />
                  </BlurFade>
                ))}
              </div>
            </section>
          )}

          {others.length > 0 && (
            <section className="mt-10">
              {recommended.length > 0 && (
                <h2 className="mb-3 text-sm font-bold uppercase tracking-widest text-muted">
                  {t("allBooks")}
                </h2>
              )}
              <div className="grid gap-4 sm:grid-cols-3 lg:grid-cols-4">
                {others.map((book, index) => (
                  <BlurFade key={book.id} delay={index * 0.05}>
                    <BookCard book={book} />
                  </BlurFade>
                ))}
              </div>
            </section>
          )}

          {page.records.length === 0 && (
            <p className="mt-16 text-center text-sm text-muted">{t("empty")}</p>
          )}
        </>
      )}
    </div>
  );
}
