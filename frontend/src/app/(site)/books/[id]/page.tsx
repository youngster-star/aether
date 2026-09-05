import {getTranslations} from "next-intl/server";
import {notFound} from "next/navigation";

import {apiServerGet} from "@/lib/api/server";
import type {BookDetailVO, ChapterNodeVO} from "@/lib/api/types";
import ProtectedImage from "@/components/media/ProtectedImage";
import BookReader from "@/components/book/BookReader";

/**
 * 书籍详情页（UI-Plan §6.7 详情 / BackEnd-Plan §5.2 GET /books/{id}）
 *
 * <p>封面 + 简介 + 分类标签 + 章-节两级目录树（F5/A6）；
 * 「开始阅读」进入全屏阅读器（封面翻页 → 版权页 → 正文，BookReader 接管）。</p>
 */
export default async function BookDetailPage({params}: {params: Promise<{id: string}>}) {
  const {id} = await params;
  const bookId = Number(id);
  const t = await getTranslations("book");

  if (!Number.isInteger(bookId) || bookId <= 0) {
    notFound();
  }
  const book = await apiServerGet<BookDetailVO>(`/books/${bookId}`).catch(() => null);
  if (book === null) {
    notFound();
  }

  return (
    <div className="mx-auto mt-10 max-w-6xl px-4" data-module="book">
      {/* 面包屑：首页 / 书籍 / 书名（A6 下划线展开） */}
      <nav aria-label="breadcrumb" className="mb-8 text-xs text-muted">
        <ol className="flex flex-wrap items-center gap-2">
          <li>
            <a href="/aether/" className="crumb-link">
              {t("breadcrumbHome")}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li>
            <a href="/aether/books" className="crumb-link">
              {t("breadcrumbBooks")}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li aria-current="page" className="line-clamp-1 max-w-[40vw] text-accent">
            {book.title}
          </li>
        </ol>
      </nav>

      {/* 书籍头：封面 + 元信息 + 开始阅读 */}
      <header className="mb-10 flex flex-col gap-6 sm:flex-row sm:items-start">
        <div className="aspect-[2/3] w-40 shrink-0 overflow-hidden rounded-lg border border-border
                        bg-accent/10 shadow-aether sm:w-52">
          {book.coverUrl ? (
            <ProtectedImage src={book.coverUrl} alt={book.title} />
          ) : (
            <div className="flex h-full w-full items-center justify-center font-display text-5xl opacity-30">
              {book.title.slice(0, 1)}
            </div>
          )}
        </div>
        <div className="min-w-0 flex-1">
          <h1 className="font-display text-3xl font-black leading-snug tracking-wide sm:text-4xl">
            {book.title}
          </h1>
          <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-muted">
            {book.author && <span>{book.author}</span>}
            <span className="tabular-nums">{t("chapterCount", {count: book.totalChapters})}</span>
            {book.categories.map((category) => (
              <span key={category} className="rounded-pill border border-accent-2 px-2 py-0.5 text-accent-2">
                {category}
              </span>
            ))}
            {book.tags.map((tag) => (
              <span key={tag} className="rounded-pill border border-border px-2 py-0.5">
                {tag}
              </span>
            ))}
          </div>
          {book.intro && (
            <p className="mt-4 max-w-3xl border-l-2 border-accent pl-4 text-sm leading-relaxed text-muted">
              {book.intro}
            </p>
          )}
          <BookReader bookId={book.id} bookTitle={book.title}
                      ownershipType={book.ownershipType} chapters={book.chapters} />
        </div>
      </header>

      {/* 章-节两级目录树（F5 展开动画在阅读器内；本页静态列表，A6 hover） */}
      <section>
        <h2 className="mb-4 text-sm font-bold uppercase tracking-widest text-muted">
          {t("toc")}
        </h2>
        <ol className="divide-y divide-border overflow-hidden rounded-lg border border-border">
          {book.chapters.map((chapter) => (
            <ChapterRow key={chapter.id} chapter={chapter} />
          ))}
          {book.chapters.length === 0 && (
            <li className="px-4 py-8 text-center text-sm text-muted">{t("chapterNotFound")}</li>
          )}
        </ol>
      </section>
    </div>
  );
}

/**
 * 目录树行（章含子节；节缩进展示）
 */
function ChapterRow({chapter}: {chapter: ChapterNodeVO}) {
  return (
    <li>
      <div className="px-4 py-3">
        <span className="text-sm font-bold">{chapter.title}</span>
        <span className="ml-2 text-xs tabular-nums opacity-60">{chapter.wordCount} 字</span>
      </div>
      {(chapter.children ?? []).length > 0 && (
        <ol className="border-t border-dashed border-border/60">
          {(chapter.children ?? []).map((child) => (
            <li key={child.id} className="px-8 py-2 text-sm text-muted">
              {child.title}
            </li>
          ))}
        </ol>
      )}
    </li>
  );
}
