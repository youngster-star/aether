"use client";

import {useEffect, useMemo, useRef, useState} from "react";
import {useLocale, useTranslations} from "next-intl";

import {Link} from "@/i18n/navigation";
import {apiGet} from "@/lib/api/client";
import type {MusicAlbumDetailVO, MusicDetailVO, MusicListVO} from "@/lib/api/types";
import ProtectedImage from "@/components/media/ProtectedImage";
import {parseEffectConfig} from "@/lib/music/effect";
import {currentLyricIndex, parseLyrics, type LyricLine} from "@/lib/music/lyric";
import {musicPlayer, type PlayableTrack, type PlayerSnapshot} from "@/lib/music/player";
import {createSandboxEffect, type SandboxHandle} from "@/lib/music/sandbox";
import {createVisualizer, type VisualizerHandle} from "@/lib/music/visualizer";

/**
 * 音乐详情交互层（UI-Plan §6.6 详情页 + §8.4 特效渲染 + §8.5 沙箱层）
 *
 * <p>播放走全局 Audio 单例（切页不断播，阶段 7 AI 悬浮窗"音乐"tab 复用同一实例）；
 * 背景为 MusicVisualizer 特效层（EffectConfig 驱动，切歌插值过渡）；沙箱代码存在时
 * 在 iframe sandbox 内受限执行，3s 未就绪/执行异常自动回退 config 渲染；
 * 歌词 LRC 逐行同步（当前行高亮 + 自动滚动到可视区中部，点击行 seek）。</p>
 */
export default function MusicDetailClient({music}: {music: MusicDetailVO}) {
  const t = useTranslations("music");
  const locale = useLocale();

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const sandboxLayerRef = useRef<HTMLDivElement>(null);
  const lyricsListRef = useRef<HTMLOListElement>(null);
  const visualizerRef = useRef<VisualizerHandle | null>(null);
  const sandboxRef = useRef<SandboxHandle | null>(null);
  const queueRef = useRef<PlayableTrack[]>([]);

  const [snapshot, setSnapshot] = useState<PlayerSnapshot>(() => musicPlayer.getSnapshot());

  /** 当前页曲目（页面主体；正在播放的可能不同） */
  const pageTrack = useMemo<PlayableTrack>(() => toPlayableTrack(music), [music]);

  /** 播放队列：合集上下文 → 全部曲目；独立单曲 → 单曲循环 */
  const [queue, setQueue] = useState<PlayableTrack[]>([pageTrack]);

  // ===== 队列装载（合集详情一次取齐） =====
  useEffect(() => {
    let cancelled = false;
    const loadQueue = async () => {
      if (!music.albumId) {
        setQueue([pageTrack]);
        queueRef.current = [pageTrack];
        return;
      }
      try {
        const album = await apiGet<MusicAlbumDetailVO>(`/music/albums/${music.albumId}`);
        if (cancelled) {
          return;
        }
        const tracks: PlayableTrack[] = album.tracks.map((item: MusicListVO) => ({
          id: item.id,
          title: item.title,
          artist: item.artist,
          fileUrl: item.fileUrl,
          lyricText: null,
          lyricOffset: 0,
          duration: item.duration,
          effectConfig: null,
          coverUrl: item.coverUrl,
        }));
        // 曲目列表不含歌词/特效（列表 VO 精简），以当前页详情补齐对应曲目元数据
        const merged = tracks.map((item) => item.id === music.id ? pageTrack : item);
        setQueue(merged);
        queueRef.current = merged;
      } catch {
        if (!cancelled) {
          setQueue([pageTrack]);
          queueRef.current = [pageTrack];
        }
      }
    };
    void loadQueue();
    return () => {
      cancelled = true;
    };
  }, [music.albumId, music.id, pageTrack]);

  // ===== 播放器快照订阅 =====
  useEffect(() => musicPlayer.subscribe(setSnapshot), []);

  // ===== 可视化引擎生命周期 =====
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return;
    }
    const audio = musicPlayer.getAudioElement();
    const visualizer = createVisualizer(audio, parseEffectConfig(music.effectConfig));
    visualizer.attach(canvas);
    visualizer.start();
    visualizerRef.current = visualizer;
    return () => {
      visualizer.destroy();
      visualizerRef.current = null;
    };
    // 详情页切换（路由变化）时重建引擎
  }, [music.id, music.effectConfig]);

  // ===== 切歌：特效配置插值过渡（§8.4 transition.duration） =====
  useEffect(() => {
    const visualizer = visualizerRef.current;
    if (!visualizer || !snapshot.current) {
      return;
    }
    visualizer.applyConfig(parseEffectConfig(snapshot.current.effectConfig), true);
  }, [snapshot.current?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  // ===== 沙箱层：当前曲目带 sandboxCode 时挂载（失败自动回退 config 渲染） =====
  // 容器 div 常驻渲染（空容器无视觉影响），挂载由 iframe 命令式完成，无需可见性 state
  useEffect(() => {
    const layer = sandboxLayerRef.current;
    if (!layer) {
      return;
    }
    const config = parseEffectConfig(snapshot.current?.effectConfig ?? music.effectConfig);
    const code = snapshot.current?.id === music.id ? config.sandboxCode : null;
    if (!code) {
      sandboxRef.current?.destroy();
      sandboxRef.current = null;
      return;
    }
    let cancelled = false;
    const sandbox = createSandboxEffect();
    void sandbox.mount(layer, code).then((ok) => {
      if (cancelled) {
        sandbox.destroy();
        return;
      }
      if (ok) {
        sandboxRef.current = sandbox;
        // 沙箱帧数据复用 visualizer 的 analyser 特征（同一音频源，不允许二次建源）
        visualizerRef.current?.onFeatures((features) => {
          sandbox.pushFrame({level: features.level, bands: features.bands, beat: features.beat});
        });
      } else {
        // 沙箱超时/执行异常：自动回退 config 渲染（画布特效保留）
        sandbox.destroy();
      }
    });
    return () => {
      cancelled = true;
      sandbox.destroy();
      visualizerRef.current?.onFeatures(null);
    };
  }, [snapshot.current?.id, music.id, music.effectConfig]); // eslint-disable-line react-hooks/exhaustive-deps

  // ===== 歌词 =====
  const lyrics = useMemo<LyricLine[]>(
    () => parseLyrics(music.lyricText, music.lyricOffset, music.duration || snapshot.duration),
    [music.lyricText, music.lyricOffset, music.duration, snapshot.duration],
  );
  const activeIndex = currentLyricIndex(lyrics, snapshot.position);

  // 当前行自动滚动到可视区中部（reduced-motion 直接跳转）
  useEffect(() => {
    if (activeIndex < 0 || !lyricsListRef.current) {
      return;
    }
    const line = lyricsListRef.current.children[activeIndex] as HTMLElement | undefined;
    if (line) {
      const reduced = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false;
      line.scrollIntoView({block: "center", behavior: reduced ? "auto" : "smooth"});
    }
  }, [activeIndex]);

  // ===== 播放控制 =====
  const playPause = () => {
    if (!snapshot.current) {
      // 未播放：从当前页曲目开始（合集队列已装载则整队可导航）
      musicPlayer.play(pageTrack, queueRef.current);
      return;
    }
    musicPlayer.toggle();
  };

  const isThisPlaying = snapshot.current?.id === music.id && snapshot.playing;
  const hasQueueNav = queue.length > 1;
  const totalSeconds = snapshot.duration > 0 ? snapshot.duration : music.duration;

  return (
    <div
      data-module="music"
      className="relative overflow-hidden rounded-xl border border-border shadow-aether"
    >
      {/* 特效背景层（MusicVisualizer；沙箱 iframe 命令式挂载于此容器，空容器无视觉影响） */}
      <canvas ref={canvasRef} className="absolute inset-0 h-full w-full" aria-hidden />
      <div ref={sandboxLayerRef} className="pointer-events-none absolute inset-0" aria-hidden />

      <div className="relative z-10 grid gap-8 bg-black/45 p-6 backdrop-blur-[2px] sm:p-8
                      lg:grid-cols-[280px_minmax(0,1fr)]">
        {/* 左列：封面 + 标题 + 控制条 */}
        <div className="flex flex-col gap-4">
          <div className="aspect-square w-full overflow-hidden rounded-lg bg-white/5">
            {music.coverUrl ? (
              <ProtectedImage src={music.coverUrl} alt={music.title} />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-4xl opacity-30" aria-hidden>
                ♫
              </div>
            )}
          </div>
          <div className="text-[#F3EAD8]">
            <h2 className="font-display text-2xl font-black leading-snug tracking-wide">{music.title}</h2>
            <p className="mt-1 text-sm opacity-80">
              {[music.artist, music.albumTitle].filter(Boolean).join(" · ") || t("unknownArtist")}
            </p>
            {music.albumId && (
              <Link
                href={`/music/${music.albumId}`}
                className="mt-2 inline-block text-xs text-[#F3EAD8]/70 underline-offset-4 hover:underline"
              >
                {music.albumTitle}
              </Link>
            )}
          </div>

          {/* 控制条：上一首 / 播放暂停 / 下一首 + 进度 + 音量 */}
          <div className="mt-2 flex flex-col gap-3 text-[#F3EAD8]">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => musicPlayer.previous()}
                disabled={!hasQueueNav}
                aria-label={t("prev")}
                className="rounded-full border border-[#F3EAD8]/40 px-3 py-1.5 text-xs transition-colors
                           hover:bg-[#F3EAD8]/15 disabled:opacity-40"
              >
                ⏮ {t("prev")}
              </button>
              <button
                type="button"
                onClick={playPause}
                aria-label={isThisPlaying ? t("pause") : t("play")}
                className="rounded-full bg-[#F3EAD8] px-4 py-1.5 text-xs font-bold text-[#1A1B1F]
                           transition-transform hover:scale-105"
              >
                {isThisPlaying ? `⏸ ${t("pause")}` : `▶ ${t("play")}`}
              </button>
              <button
                type="button"
                onClick={() => musicPlayer.next()}
                disabled={!hasQueueNav}
                aria-label={t("next")}
                className="rounded-full border border-[#F3EAD8]/40 px-3 py-1.5 text-xs transition-colors
                           hover:bg-[#F3EAD8]/15 disabled:opacity-40"
              >
                {t("next")} ⏭
              </button>
            </div>
            <div className="flex items-center gap-2 text-xs tabular-nums">
              <span>{formatSeconds(snapshot.position)}</span>
              <input
                type="range"
                min={0}
                max={Math.max(totalSeconds, 1)}
                step={0.1}
                value={Math.min(snapshot.position, totalSeconds)}
                onChange={(event) => musicPlayer.seek(Number(event.target.value))}
                aria-label={t("progress")}
                className="w-full accent-[#E8C94A]"
              />
              <span>{formatSeconds(totalSeconds)}</span>
            </div>
            <div className="flex items-center gap-2 text-xs">
              <span aria-hidden>🔊</span>
              <input
                type="range"
                min={0}
                max={1}
                step={0.01}
                value={snapshot.volume}
                onChange={(event) => musicPlayer.setVolume(Number(event.target.value))}
                aria-label={t("volume")}
                className="w-28 accent-[#E8C94A]"
              />
            </div>
          </div>
        </div>

        {/* 右列：歌词同步滚动（当前行 accent 高亮；点击行 seek） */}
        <div className="min-w-0">
          <p className="mb-3 text-xs uppercase tracking-widest text-[#F3EAD8]/60">{t("lyrics")}</p>
          {lyrics.length === 0 ? (
            <p className="py-10 text-sm text-[#F3EAD8]/60">{t("noLyrics")}</p>
          ) : (
            <ol
              ref={lyricsListRef}
              className="max-h-80 overflow-y-auto pr-2 text-sm leading-loose [scrollbar-width:thin]"
            >
              {lyrics.map((line, index) => (
                <li key={`${index}-${line.time}`}>
                  <button
                    type="button"
                    onClick={() => musicPlayer.seek(line.time)}
                    className={`block w-full truncate text-left transition-colors ${
                      index === activeIndex
                          ? "font-bold text-[#E8C94A]"
                          : "text-[#F3EAD8]/70 hover:text-[#F3EAD8]"
                    }`}
                  >
                    {line.text}
                  </button>
                </li>
              ))}
            </ol>
          )}
          {locale === "zh" && music.lyricOffset !== 0 && (
            <p className="mt-3 text-xs text-[#F3EAD8]/50">
              {t("offsetApplied", {offset: music.lyricOffset})}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

/**
 * 详情 VO → 可播放曲目（全局播放器结构）
 */
function toPlayableTrack(music: MusicDetailVO): PlayableTrack {
  return {
    id: music.id,
    title: music.title,
    artist: music.artist,
    fileUrl: music.fileUrl,
    lyricText: music.lyricText,
    lyricOffset: music.lyricOffset,
    duration: music.duration,
    effectConfig: music.effectConfig,
    coverUrl: music.coverUrl,
  };
}

/**
 * 秒 → mm:ss / h:mm:ss
 */
function formatSeconds(seconds: number): string {
  if (!seconds || seconds <= 0 || !Number.isFinite(seconds)) {
    return "00:00";
  }
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = Math.floor(seconds % 60);
  const pad = (value: number) => String(value).padStart(2, "0");
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${pad(minutes)}:${pad(rest)}`;
}
