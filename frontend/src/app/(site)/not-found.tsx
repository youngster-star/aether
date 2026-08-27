import Wordmark from "@/components/layout/Wordmark";
import {Link} from "@/i18n/navigation";

/**
 * 404 页面（羊皮卷风：字标 + 卷号 + 返回首页）
 */
export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center gap-8 px-4 py-32 text-center">
      <Wordmark height={64} />
      <p className="font-display text-6xl font-black text-accent">404</p>
      <p className="text-sm text-muted">这页羊皮纸还没有被写下内容</p>
      <Link
        href="/"
        className="rounded-pill border border-border px-6 py-2 text-sm transition-colors
                   hover:bg-accent hover:text-background"
      >
        返回首页
      </Link>
    </div>
  );
}
