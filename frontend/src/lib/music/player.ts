/**
 * 全局音频播放单例（UI-Plan §6.6 悬浮播放 / §8.4 数据流）
 *
 * <p>后台 Audio 实例全局唯一：音乐详情页与后续 AI 助手悬浮窗"音乐"tab（阶段 7）
 * 共用同一实例，切页/收起悬浮窗后播放不中断。当前播放队列由详情页注入
 * （合集上下文 → 上一首/下一首）；状态经简易订阅分发（避免引全局状态库）。</p>
 */

/** 播放器快照（订阅分发用） */
export interface PlayerSnapshot {
  /** 当前曲目（未播放为 null） */
  current: PlayableTrack | null;
  playing: boolean;
  /** 当前播放位置（秒） */
  position: number;
  /** 曲目总时长（秒，元数据就绪后可得） */
  duration: number;
  /** 音量 0-1 */
  volume: number;
}

/** 可播放曲目（详情页/悬浮 tab 共用的最小结构） */
export interface PlayableTrack {
  id: number;
  title: string;
  artist: string | null;
  /** 音频签名 URL */
  fileUrl: string;
  /** 歌词文本（可空） */
  lyricText: string | null;
  /** 歌词全局偏移（毫秒） */
  lyricOffset: number;
  /** 后端时长（秒，元数据加载前用于展示） */
  duration: number;
  /** EffectConfig 原始 JSON（切歌插值过渡用，可空=默认配置） */
  effectConfig: string | null;
  /** 封面签名 URL（悬浮窗展示用，可空） */
  coverUrl: string | null;
}

type Listener = (snapshot: PlayerSnapshot) => void;

/**
 * 全局播放器（模块级单例，SSR 下不创建 Audio 元素）
 */
class GlobalPlayer {
  private audio: HTMLAudioElement | null = null;
  private listeners = new Set<Listener>();
  private snapshot: PlayerSnapshot = {
    current: null,
    playing: false,
    position: 0,
    duration: 0,
    volume: 1,
  };

  /** 当前播放队列（上一首/下一首导航用，由详情页注入） */
  private queue: PlayableTrack[] = [];

  private ensureAudio(): HTMLAudioElement {
    if (!this.audio) {
      const audio = new Audio();
      audio.preload = "metadata";
      audio.addEventListener("timeupdate", () => this.update({position: audio.currentTime}));
      audio.addEventListener("durationchange", () => this.update({duration: audio.duration || 0}));
      audio.addEventListener("play", () => this.update({playing: true}));
      audio.addEventListener("pause", () => this.update({playing: false}));
      audio.addEventListener("ended", () => this.next(true));
      audio.addEventListener("error", () => this.update({playing: false}));
      this.audio = audio;
    }
    return this.audio;
  }

  /**
   * 播放指定曲目（同曲目重复调用 = 续播/暂停切换由调用方控制）
   */
  play(track: PlayableTrack, queue: PlayableTrack[] = [track]): void {
    const audio = this.ensureAudio();
    this.queue = queue;
    if (this.snapshot.current?.id !== track.id || audio.src !== track.fileUrl) {
      audio.src = track.fileUrl;
      this.update({current: track, position: 0, duration: track.duration});
    }
    audio.volume = this.snapshot.volume;
    void audio.play().catch(() => this.update({playing: false}));
  }

  /** 暂停/续播 */
  toggle(): void {
    const audio = this.ensureAudio();
    if (!this.snapshot.current) {
      return;
    }
    if (audio.paused) {
      void audio.play().catch(() => this.update({playing: false}));
    } else {
      audio.pause();
    }
  }

  /** 定位（进度条拖动/歌词点击跳转） */
  seek(seconds: number): void {
    const audio = this.ensureAudio();
    if (Number.isFinite(audio.duration)) {
      audio.currentTime = Math.min(Math.max(seconds, 0), audio.duration);
    }
  }

  /** 音量调节（0-1） */
  setVolume(volume: number): void {
    this.ensureAudio().volume = Math.min(Math.max(volume, 0), 1);
    this.update({volume: this.snapshot.volume});
  }

  /** 上一首/下一首（队列循环；ended 自动调用） */
  next(fromEnded = false): void {
    if (this.queue.length < 2 || !this.snapshot.current) {
      if (fromEnded) {
        this.update({playing: false, position: 0});
      }
      return;
    }
    const index = this.queue.findIndex((item) => item.id === this.snapshot.current?.id);
    const nextTrack = this.queue[(index + 1) % this.queue.length];
    this.play(nextTrack, this.queue);
  }

  previous(): void {
    if (this.queue.length < 2 || !this.snapshot.current) {
      return;
    }
    const index = this.queue.findIndex((item) => item.id === this.snapshot.current?.id);
    const prevTrack = this.queue[(index - 1 + this.queue.length) % this.queue.length];
    this.play(prevTrack, this.queue);
  }

  /** 订阅快照（组件挂载时调用，返回退订函数） */
  subscribe(listener: Listener): () => void {
    this.listeners.add(listener);
    listener(this.snapshot);
    return () => this.listeners.delete(listener);
  }

  /** 获取当前快照（非订阅场景） */
  getSnapshot(): PlayerSnapshot {
    return this.snapshot;
  }

  /** 当前曲目 ID（详情页判断"正在播放哪首"用） */
  getCurrentTrackId(): number | null {
    return this.snapshot.current?.id ?? null;
  }

  /**
   * 取底层 Audio 元素（MusicVisualizer createMediaElementSource 用；
   * 全局唯一实例，禁止重复创建第二张音频源图；未选曲目时也预创建）
   */
  getAudioElement(): HTMLAudioElement {
    return this.ensureAudio();
  }

  private update(partial: Partial<PlayerSnapshot>): void {
    this.snapshot = {...this.snapshot, ...partial};
    this.listeners.forEach((listener) => listener(this.snapshot));
  }
}

const globalForPlayer = globalThis as unknown as {__aetherPlayer?: GlobalPlayer};

/** 模块级单例（dev 热重载下保持同一实例） */
export const musicPlayer: GlobalPlayer = globalForPlayer.__aetherPlayer ?? new GlobalPlayer();
globalForPlayer.__aetherPlayer = musicPlayer;
