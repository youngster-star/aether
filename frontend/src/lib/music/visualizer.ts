/**
 * MusicVisualizer 渲染引擎（UI-Plan §8.4 三层架构的渲染层）
 *
 * <p>固定引擎（永远存在）：AudioContext.createAnalyser() 取时域/频域数据，
 * rAF 按 EffectConfig 逐层渲染 Canvas 2D；bind 映射 amplitude→响度、
 * freqBand→频段、beat→节拍脉冲。粒子计算走 OffscreenCanvas + Worker
 * （Blob 内联 worker，无独立文件），不可用时回退主线程绘制；
 * 切歌/换配置数值插值过渡（transition.duration）；移动端粒子减半 + 30fps；
 * prefers-reduced-motion 时仅渲染静态帧。</p>
 */

import {interpolateEffectConfig, type EffectConfig} from "./effect";

/** 音频特征帧（渲染输入） */
export interface AudioFeatures {
  /** 响度 0-1（时域 RMS） */
  level: number;
  /** 频段能量 0-1（fftSize/2 桶归一化） */
  bands: number[];
  /** 本帧是否节拍脉冲 */
  beat: boolean;
  /** 节拍脉冲强度衰减值 0-1（触发后约 400ms 衰减） */
  beatPulse: number;
}

/** 可视化控制器（详情页持有） */
export interface VisualizerHandle {
  /** 接入画布（尺寸自适应由引擎内 ResizeObserver 处理） */
  attach(canvas: HTMLCanvasElement): void;
  /** 开始渲染循环（首次调用创建 AudioContext 并连接 analyser） */
  start(): void;
  /** 暂停渲染循环（暂停播放时保留最后一帧） */
  stop(): void;
  /** 切换配置（带插值过渡；无过渡时立即切换） */
  applyConfig(config: EffectConfig, animate?: boolean): void;
  /**
   * 注册音频特征监听（沙箱层帧推送复用同一 analyser 数据；
   * 传 null 取消监听。AudioContext 不可用时不会回调。）
   */
  onFeatures(listener: ((features: AudioFeatures) => void) | null): void;
  /** 释放（rAF/worker/audioContext 全部回收） */
  destroy(): void;
}

/** 常量：节拍检测参数 */
const BEAT_THRESHOLD_RATIO = 1.35;
const BEAT_COOLDOWN_MS = 280;
const BEAT_DECAY_PER_SECOND = 2.4;

/** 移动端判定（粗指针或窄屏：粒子减半 + 30fps） */
function isLowPowerDevice(): boolean {
  if (typeof window === "undefined") {
    return false;
  }
  const coarsePointer = window.matchMedia?.("(pointer: coarse)").matches ?? false;
  const narrow = window.innerWidth < 768;
  return coarsePointer || narrow;
}

/** prefers-reduced-motion（静态帧模式） */
function prefersReducedMotion(): boolean {
  return typeof window !== "undefined"
      && (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false);
}

/**
 * 创建可视化控制器
 *
 * @param audio       播放中的 <audio> 元素（全局单例，见 lib/music/player.ts）
 * @param initialConfig 初始 EffectConfig（已解析）
 */
export function createVisualizer(audio: HTMLAudioElement, initialConfig: EffectConfig): VisualizerHandle {
  const lowPower = isLowPowerDevice();
  const reducedMotion = prefersReducedMotion();

  let canvas: HTMLCanvasElement | null = null;
  let ctx2d: CanvasRenderingContext2D | null = null;
  let width = 0;
  let height = 0;

  let audioContext: AudioContext | null = null;
  let analyser: AnalyserNode | null = null;
  let sourceNode: MediaElementAudioSourceNode | null = null;
  const timeDomain = new Uint8Array(256);
  const frequency = new Uint8Array(128);

  let currentConfig = initialConfig;
  let fromConfig = initialConfig;
  let transitionStart = -1;
  let rafId = 0;
  let running = false;
  let lastFrameTime = 0;
  let lowBandAverage = 0.2;
  let lastBeatAt = -Infinity;
  let beatPulse = 0;
  let destroyed = false;
  let featureListener: ((features: AudioFeatures) => void) | null = null;

  /** 粒子 worker（OffscreenCanvas 可用时） */
  let worker: Worker | null = null;
  let workerReady = false;
  /** 主线程粒子（worker 不可用回退） */
  let fallbackParticles: {x: number; y: number; vx: number; vy: number; size: number}[] = [];
  let resizeObserver: ResizeObserver | null = null;

  // ===== Worker 内联脚本（Blob 创建；粒子模拟 + OffscreenCanvas 绘制） =====
  const workerSource = `
let canvas = null, ctx = null, params = null, particles = [];
function seedParticles(count) {
  particles = [];
  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random(), y: Math.random(),
      vx: (Math.random() - 0.5) * 0.02,
      vy: -(0.004 + Math.random() * 0.012),
      size: params ? params.sizeRange[0] + Math.random() * (params.sizeRange[1] - params.sizeRange[0]) : 3,
    });
  }
}
self.onmessage = (event) => {
  const data = event.data;
  if (data.type === 'init') {
    canvas = data.canvas; ctx = canvas.getContext('2d');
    params = data.params; seedParticles(params.count);
    resize(data.width, data.height);
    self.postMessage({type: 'ready'});
  } else if (data.type === 'resize') {
    resize(data.width, data.height);
  } else if (data.type === 'config') {
    params = data.params;
    if (particles.length !== params.count) seedParticles(params.count);
  } else if (data.type === 'frame' && ctx && params) {
    const level = data.level, beat = data.beat;
    const [w, h] = [canvas.width, canvas.height];
    const speed = params.speed, opacity = params.opacity;
    const [minSize, maxSize] = params.sizeRange;
    const palette = params.palette;
    ctx.clearRect(0, 0, w, h);
    for (const p of particles) {
      const drift = (0.4 + level) * speed;
      p.x += p.vx * drift * 0.016; p.y += p.vy * drift * 0.016;
      if (p.y < -0.05) { p.y = 1.05; p.x = Math.random(); }
      if (p.x < -0.05 || p.x > 1.05) p.x = Math.random();
      const boost = beat ? 1.6 : 1;
      const size = (minSize + p.size % Math.max(maxSize - minSize, 0.1)) * boost * (0.6 + level * 0.8);
      ctx.globalAlpha = opacity * (0.35 + level * 0.65);
      ctx.fillStyle = palette[Math.floor(p.x * 97) % palette.length] || palette[0];
      ctx.beginPath();
      ctx.arc(p.x * w, p.y * h, size, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.globalAlpha = 1;
  }
};
function resize(w, h) { if (canvas) { canvas.width = w; canvas.height = h; } }
`;

  function ensureWorker(): void {
    if (worker || workerReady || destroyed) {
      return;
    }
    if (typeof OffscreenCanvas === "undefined" || typeof Worker === "undefined" || !canvas) {
      return;
    }
    try {
      const blob = new Blob([workerSource], {type: "text/javascript"});
      const url = URL.createObjectURL(blob);
      worker = new Worker(url);
      URL.revokeObjectURL(url);
      const offscreen = canvas.transferControlToOffscreen();
      worker.onmessage = (event) => {
        if (event.data?.type === "ready") {
          workerReady = true;
        }
      };
      worker.postMessage({
        type: "init",
        canvas: offscreen,
        width,
        height,
        params: particleParams(currentConfig),
      }, [offscreen]);
    } catch {
      worker = null; // worker 创建失败：回退主线程粒子
    }
  }

  function particleParams(config: EffectConfig) {
    const layer = config.layers.find((item) => item.type === "particles");
    const count = Math.max(1, Math.round((layer?.count ?? 120) * (lowPower ? 0.5 : 1)));
    return {
      count,
      sizeRange: layer?.sizeRange ?? [1, 6],
      speed: layer?.speed ?? 1,
      opacity: layer?.opacity ?? 0.7,
      palette: config.palette,
    };
  }

  // ===== 主线程渲染 =====

  function resize(): void {
    if (!canvas) {
      return;
    }
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const nextWidth = Math.max(1, Math.round(canvas.clientWidth * dpr));
    const nextHeight = Math.max(1, Math.round(canvas.clientHeight * dpr));
    if (nextWidth !== width || nextHeight !== height) {
      width = nextWidth;
      height = nextHeight;
      if (worker && workerReady) {
        worker.postMessage({type: "resize", width, height});
      }
      if (!worker) {
        canvas.width = width;
        canvas.height = height;
      }
      // 主线程回退粒子重排布
      if (!worker && ctx2d) {
        seedFallbackParticles(particleParams(currentConfig).count);
      }
    }
  }

  function seedFallbackParticles(count: number): void {
    fallbackParticles = Array.from({length: count}, () => ({
      x: Math.random(),
      y: Math.random(),
      vx: (Math.random() - 0.5) * 0.02,
      vy: -(0.004 + Math.random() * 0.012),
      size: 1 + Math.random() * 5,
    }));
  }

  /**
   * 读取音频特征（analyser 时域/频域 → AudioFeatures）
   */
  function readFeatures(now: number): AudioFeatures {
    if (!analyser) {
      return {level: 0, bands: new Array(16).fill(0), beat: false, beatPulse};
    }
    analyser.getByteTimeDomainData(timeDomain);
    analyser.getByteFrequencyData(frequency);
    let sumSquares = 0;
    for (let i = 0; i < timeDomain.length; i++) {
      const value = (timeDomain[i] - 128) / 128;
      sumSquares += value * value;
    }
    const level = Math.min(1, Math.sqrt(sumSquares / timeDomain.length) * 2.2);
    const bands: number[] = [];
    const bucketSize = Math.max(1, Math.floor(frequency.length / 16));
    for (let bucket = 0; bucket < 16; bucket++) {
      let sum = 0;
      for (let i = 0; i < bucketSize; i++) {
        sum += frequency[bucket * bucketSize + i] ?? 0;
      }
      bands.push(Math.min(1, sum / bucketSize / 255));
    }
    // 节拍检测：低频段（前 3 桶）能量超过滚动均值 × 阈值，冷却期内不重复触发
    const lowEnergy = (bands[0] + bands[1] + bands[2]) / 3;
    lowBandAverage = lowBandAverage * 0.96 + lowEnergy * 0.04;
    const isBeat = lowEnergy > lowBandAverage * BEAT_THRESHOLD_RATIO
        && lowEnergy > 0.08
        && now - lastBeatAt > BEAT_COOLDOWN_MS;
    if (isBeat) {
      lastBeatAt = now;
    }
    beatPulse = Math.max(0, beatPulse - BEAT_DECAY_PER_SECOND * ((now - lastFrameTime) / 1000));
    if (isBeat) {
      beatPulse = 1;
    }
    return {level, bands, beat: isBeat, beatPulse};
  }

  /** 过渡中的当前配置 */
  function frameConfig(now: number): EffectConfig {
    if (transitionStart < 0) {
      return currentConfig;
    }
    const duration = Math.max(1, currentConfig.transition?.duration ?? 800);
    const progress = (now - transitionStart) / duration;
    if (progress >= 1) {
      transitionStart = -1;
      return currentConfig;
    }
    return interpolateEffectConfig(fromConfig, currentConfig, progress);
  }

  function hexToRgba(hex: string | undefined, alpha: number, fallback: string): string {
    const normalized = hex && /^#[0-9a-fA-F]{6}$/.test(hex) ? hex : fallback;
    const r = parseInt(normalized.slice(1, 3), 16);
    const g = parseInt(normalized.slice(3, 5), 16);
    const b = parseInt(normalized.slice(5, 7), 16);
    return `rgba(${r},${g},${b},${alpha})`;
  }

  function drawBackground(config: EffectConfig): void {
    if (!ctx2d) {
      return;
    }
    const background = config.background;
    if (!background) {
      return;
    }
    if (background.type === "gradient" && background.to) {
      const gradient = ctx2d.createLinearGradient(0, 0, 0, height);
      gradient.addColorStop(0, hexToRgba(background.from, 1, "#0F1013"));
      gradient.addColorStop(1, hexToRgba(background.to, 1, "#1A1B1F"));
      ctx2d.fillStyle = gradient;
    } else {
      ctx2d.fillStyle = hexToRgba(background.from, 1, "#0F1013");
    }
    ctx2d.fillRect(0, 0, width, height);
  }

  /** 波形层：频段切片折线（底部 1/3 区域） */
  function drawWave(config: EffectConfig, features: AudioFeatures): void {
    if (!ctx2d) {
      return;
    }
    const layer = config.layers.find((item) => item.type === "wave");
    if (!layer) {
      return;
    }
    const band = layer.band ?? [0, 0.3];
    const startBucket = Math.floor(band[0] * features.bands.length);
    const endBucket = Math.max(startBucket + 1, Math.floor(band[1] * features.bands.length));
    const slice = features.bands.slice(startBucket, endBucket + 1);
    const amplitude = (layer.amplitude ?? 40) * (0.4 + features.level * 0.9);
    const color = hexToRgba(layer.color ?? config.palette[0], layer.opacity ?? 0.9, config.palette[0]);
    const baseY = height * 0.82;
    ctx2d.beginPath();
    for (let i = 0; i <= 64; i++) {
      const x = (i / 64) * width;
      const bucketIndex = Math.min(slice.length - 1, Math.floor((i / 64) * slice.length));
      const y = baseY - slice[bucketIndex] * amplitude - Math.sin((i / 64) * Math.PI * 2 + features.level * 3) * 6;
      if (i === 0) {
        ctx2d.moveTo(x, y);
      } else {
        ctx2d.lineTo(x, y);
      }
    }
    ctx2d.strokeStyle = color;
    ctx2d.lineWidth = 2;
    ctx2d.stroke();
  }

  /** 圆环层：中心同心弧，节拍脉冲放大 */
  function drawRing(config: EffectConfig, features: AudioFeatures): void {
    if (!ctx2d) {
      return;
    }
    const layer = config.layers.find((item) => item.type === "ring");
    if (!layer) {
      return;
    }
    const amplitude = (layer.amplitude ?? 30) * (1 + features.beatPulse * 0.8);
    const baseRadius = Math.min(width, height) * 0.18;
    const color = hexToRgba(layer.color ?? config.palette[2], layer.opacity ?? 0.8, config.palette[2] ?? "#8B6F47");
    ctx2d.beginPath();
    ctx2d.arc(width / 2, height / 2, baseRadius + amplitude * features.level * 0.4, 0, Math.PI * 2);
    ctx2d.strokeStyle = color;
    ctx2d.lineWidth = 1.5 + features.beatPulse * 2;
    ctx2d.stroke();
  }

  /** 流线层：横向正弦流（speed 控制相位推进） */
  function drawFlowline(config: EffectConfig, features: AudioFeatures, phase: number): void {
    if (!ctx2d) {
      return;
    }
    const layer = config.layers.find((item) => item.type === "flowline");
    if (!layer) {
      return;
    }
    const count = Math.max(1, Math.round((layer.count ?? 60) * (lowPower ? 0.5 : 1)));
    const speed = layer.speed ?? 1;
    const color = hexToRgba(layer.color ?? config.palette[0], layer.opacity ?? 0.5, config.palette[0]);
    for (let line = 0; line < Math.min(count, 24); line++) {
      const y = (line / 24) * height;
      ctx2d.beginPath();
      for (let i = 0; i <= 48; i++) {
        const x = (i / 48) * width;
        const wave = Math.sin((i / 48) * Math.PI * 4 + phase * speed + line * 0.6);
        const yy = y + wave * 10 * (0.4 + features.level);
        if (i === 0) {
          ctx2d.moveTo(x, yy);
        } else {
          ctx2d.lineTo(x, yy);
        }
      }
      ctx2d.strokeStyle = color;
      ctx2d.lineWidth = 1;
      ctx2d.globalAlpha = 0.35;
      ctx2d.stroke();
    }
    ctx2d.globalAlpha = 1;
  }

  /** 文本层：绑定特征透明度脉冲 */
  function drawText(config: EffectConfig, features: AudioFeatures): void {
    if (!ctx2d) {
      return;
    }
    const layer = config.layers.find((item) => item.type === "text");
    if (!layer?.content) {
      return;
    }
    const value = layer.bind === "beat" ? features.beatPulse
        : layer.bind === "freqBand" ? features.bands[4] ?? 0 : features.level;
    ctx2d.fillStyle = hexToRgba(layer.color ?? config.palette[0], 0.35 + value * 0.6, config.palette[0]);
    ctx2d.font = `600 ${Math.round(height * 0.06)}px sans-serif`;
    ctx2d.textAlign = "center";
    ctx2d.textBaseline = "middle";
    ctx2d.fillText(layer.content, width / 2, height * 0.4);
  }

  /** 主线程粒子回退绘制 */
  function drawFallbackParticles(config: EffectConfig, features: AudioFeatures, dt: number): void {
    if (!ctx2d) {
      return;
    }
    const layer = config.layers.find((item) => item.type === "particles");
    if (!layer) {
      return;
    }
    const opacity = layer.opacity ?? 0.7;
    const speed = layer.speed ?? 1;
    const range = layer.sizeRange ?? [1, 6];
    for (const particle of fallbackParticles) {
      const drift = (0.4 + features.level) * speed * dt * 0.06;
      particle.x += particle.vx * drift;
      particle.y += particle.vy * drift;
      if (particle.y < -0.05) {
        particle.y = 1.05;
        particle.x = Math.random();
      }
      if (particle.x < -0.05 || particle.x > 1.05) {
        particle.x = Math.random();
      }
      const boost = features.beat ? 1.6 : 1;
      const size = (range[0] + particle.size % Math.max(range[1] - range[0], 0.1)) * boost;
      ctx2d.globalAlpha = opacity * (0.35 + features.level * 0.65);
      ctx2d.fillStyle = config.palette[Math.floor(particle.x * 97) % config.palette.length] ?? config.palette[0];
      ctx2d.beginPath();
      ctx2d.arc(particle.x * width, particle.y * height, size, 0, Math.PI * 2);
      ctx2d.fill();
    }
    ctx2d.globalAlpha = 1;
  }

  function renderFrame(now: number, dt: number, features: AudioFeatures): void {
    if (!ctx2d) {
      return;
    }
    const config = frameConfig(now);
    drawBackground(config);
    if (worker && workerReady) {
      // 粒子层交给 worker（OffscreenCanvas 自动合成到主画布）
      const payload = new Uint8Array(features.bands.length);
      for (let i = 0; i < payload.length; i++) {
        payload[i] = features.bands[i] * 255;
      }
      worker.postMessage({type: "frame", level: features.level, beat: features.beat}, [payload.buffer]);
    } else {
      drawFallbackParticles(config, features, dt);
    }
    drawWave(config, features);
    drawRing(config, features);
    drawFlowline(config, features, now / 1000);
    drawText(config, features);
  }

  function loop(now: number): void {
    if (!running || destroyed) {
      return;
    }
    const dt = lastFrameTime > 0 ? Math.min((now - lastFrameTime) / 1000, 0.1) : 0.016;
    // 移动端 30fps 限帧（隔帧渲染）
    if (lowPower && now - lastFrameTime < 33) {
      rafId = requestAnimationFrame(loop);
      return;
    }
    lastFrameTime = now;
    const features = readFeatures(now);
    featureListener?.(features);
    renderFrame(now, dt, features);
    rafId = requestAnimationFrame(loop);
  }

  function connectAudio(): void {
    if (audioContext || destroyed) {
      return;
    }
    try {
      audioContext = new AudioContext();
      analyser = audioContext.createAnalyser();
      analyser.fftSize = 256;
      analyser.smoothingTimeConstant = 0.8;
      sourceNode = audioContext.createMediaElementSource(audio);
      sourceNode.connect(analyser);
      analyser.connect(audioContext.destination);
      if (audioContext.state === "suspended") {
        void audioContext.resume();
      }
    } catch {
      // AudioContext 不可用（浏览器限制等）：降级为静态特征渲染
      audioContext = null;
      analyser = null;
    }
  }

  return {
    attach(target: HTMLCanvasElement) {
      if (destroyed) {
        return;
      }
      canvas = target;
      // transferControlToOffscreen 以运行时存在性判定（旧浏览器无此 API 时回退主线程绘制）
      if ("transferControlToOffscreen" in canvas
          && typeof OffscreenCanvas !== "undefined" && typeof Worker !== "undefined") {
        ensureWorker();
      }
      if (!worker) {
        ctx2d = canvas.getContext("2d");
      }
      resize();
      resizeObserver = new ResizeObserver(() => resize());
      resizeObserver.observe(canvas);
    },

    start() {
      if (destroyed || running) {
        return;
      }
      running = true;
      connectAudio();
      if (reducedMotion) {
        // 降级：单帧静态渲染（无 rAF 循环）
        resize();
        renderFrame(performance.now(), 0.016, {level: 0.25, bands: new Array(16).fill(0.25), beat: false, beatPulse: 0});
        return;
      }
      lastFrameTime = 0;
      rafId = requestAnimationFrame(loop);
    },

    stop() {
      running = false;
      if (rafId) {
        cancelAnimationFrame(rafId);
        rafId = 0;
      }
    },

    applyConfig(config: EffectConfig, animate = true) {
      if (destroyed) {
        return;
      }
      fromConfig = frameConfig(performance.now());
      currentConfig = config;
      transitionStart = animate ? performance.now() : -1;
      const params = particleParams(config);
      if (worker && workerReady) {
        worker.postMessage({type: "config", params});
      } else if (params.count !== fallbackParticles.length) {
        seedFallbackParticles(params.count);
      }
    },

    onFeatures(listener) {
      featureListener = listener;
    },

    destroy() {
      destroyed = true;
      running = false;
      if (rafId) {
        cancelAnimationFrame(rafId);
      }
      resizeObserver?.disconnect();
      resizeObserver = null;
      worker?.terminate();
      worker = null;
      workerReady = false;
      try {
        sourceNode?.disconnect();
        analyser?.disconnect();
        void audioContext?.close();
      } catch {
        // 已关闭/已断开：忽略
      }
      audioContext = null;
      analyser = null;
      sourceNode = null;
    },
  };
}
