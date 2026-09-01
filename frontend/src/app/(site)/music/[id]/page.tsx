import {getTranslations} from "next-intl/server";
import {notFound} from "next/navigation";

import {apiServerGet} from "@/lib/api/server";
import type {MusicDetailVO} from "@/lib/api/types";
import MusicDetailClient from "@/components/music/MusicDetailClient";

/**
 * 音乐详情页（UI-Plan §6.6 / BackEnd-Plan §5.2 GET /music/{id}）
 *
 * <p>封面 + 歌手 + 歌词（滚动 + 当前行高亮）+ 进度条 + 音量；
 * 背景与主体为 MusicVisualizer AI 特效层（§8.4），沙箱代码走 iframe 受限执行（§8.5）；
 * 播放走全局 Audio 单例（切页不断播）。</p>
 */
export default async function MusicDetailPage({params}: {params: Promise<{id: string}>}) {
  const {id} = await params;
  const musicId = Number(id);
  const t = await getTranslations("music");

  if (!Number.isInteger(musicId) || musicId <= 0) {
    notFound();
  }
  const music = await apiServerGet<MusicDetailVO>(`/music/${musicId}`).catch(() => null);
  if (music === null) {
    notFound();
  }

  return (
    <div className="mx-auto mt-10 max-w-6xl px-4" data-module="music">
      {/* 面包屑：首页 / 音乐 / 标题（A6 下划线展开） */}
      <nav aria-label="breadcrumb" className="mb-8 text-xs text-muted">
        <ol className="flex flex-wrap items-center gap-2">
          <li>
            <a href="/aether/" className="crumb-link">
              {t("breadcrumbHome")}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li>
            <a href="/aether/music" className="crumb-link">
              {t("breadcrumbMusic")}
            </a>
          </li>
          <li aria-hidden>·</li>
          <li aria-current="page" className="line-clamp-1 max-w-[40vw] text-accent">
            {music.title}
          </li>
        </ol>
      </nav>

      {/* 特效背景 + 播放器/歌词主体（客户端组件） */}
      <MusicDetailClient music={music} />
    </div>
  );
}
