import Link from "next/link";

import type {BookListVO} from "@/lib/api/types";
import ProtectedImage from "@/components/media/ProtectedImage";

/**
 * 书籍卡片（UI-Plan §6.7 列表：封面 + 书名 + 作者 + 一句话介绍/标签）
 *
 * <p>hover T2 反色（铜绿，data-module="book" 行级样式）；
 * 封面 ProtectedImage（§4.4 禁直接暴露存储 URL）。</p>
 */
export default function BookCard({book}: {book: BookListVO}) {
  return (
    <Link
      href={`/books/${book.id}`}
      data-module="book"
      className="group flex h-full flex-col overflow-hidden rounded-lg border border-border
                 bg-card shadow-aether transition-colors hover:bg-[var(--tint)] hover:text-[var(--tint-on)]"
    >
      {/* 封面（书籍竖版比例 2:3） */}
      <div className="aspect-[2/3] w-full overflow-hidden bg-accent/10">
        {book.coverUrl ? (
          <ProtectedImage src={book.coverUrl} alt={book.title} />
        ) : (
          <div className="flex h-full w-full items-center justify-center font-display text-4xl opacity-30">
            {book.title.slice(0, 1)}
          </div>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-1.5 p-4">
        <h3 className="font-display text-lg font-black leading-snug">
          {book.title}
        </h3>
        {book.author && (
          <p className="text-xs opacity-70">{book.author}</p>
        )}
        {book.intro && (
          <p className="mt-1 line-clamp-2 text-xs leading-relaxed opacity-75">
            {book.intro}
          </p>
        )}
        <div className="mt-auto flex items-center justify-between pt-2 text-xs opacity-80">
          <span className="tabular-nums">{book.totalChapters} 章</span>
          {book.tags.length > 0 && (
            <span className="truncate">{book.tags.slice(0, 2).join(" · ")}</span>
          )}
        </div>
      </div>
    </Link>
  );
}
