/**
 * EffectConfig 类型与解析（BackEnd-Plan §7.3 Schema v1）
 *
 * <p>后端落库前已过 Schema 校验；前端解析仍需容错（历史数据/手工输入），
 * 解析失败回退默认配置（粒子/波形/圆环三层，羊皮卷默认调色板）。</p>
 */

/** 图层类型枚举（§7.3 layers[].type） */
export type EffectLayerType = "particles" | "wave" | "ring" | "flowline" | "text";

/** 音频特征绑定（§7.3 layers[].bind） */
export type EffectBind = "amplitude" | "freqBand" | "beat";

/** 图层配置（各类型字段可选，渲染端按类型取用并给默认值） */
export interface EffectLayer {
  type: EffectLayerType;
  bind: EffectBind;
  /** 粒子/流线数量 */
  count?: number;
  /** 粒子尺寸区间 [min, max] */
  sizeRange?: [number, number];
  /** 运动速度倍率 */
  speed?: number;
  /** 整体不透明度 0-1 */
  opacity?: number;
  /** 频段范围 [start, end]（0-1 归一化，wave 用） */
  band?: [number, number];
  /** 振幅（wave/ring） */
  amplitude?: number;
  /** 颜色（可空=取调色板轮转） */
  color?: string;
  /** text 图层内容 */
  content?: string;
}

/** EffectConfig 结构（§7.3 Schema v1） */
export interface EffectConfig {
  version: number;
  palette: string[];
  layers: EffectLayer[];
  background: {type: "gradient" | "solid"; from: string; to?: string} | null;
  transition: {duration: number; easing: string} | null;
  sandboxCode: string | null;
}

/** 默认配置（解析失败/未生成时的兜底，覆盖三类音频绑定） */
export const DEFAULT_EFFECT_CONFIG: EffectConfig = {
  version: 1,
  palette: ["#E8C94A", "#1A1B1F", "#8B6F47"],
  layers: [
    {type: "particles", bind: "amplitude", count: 120, sizeRange: [1, 6], speed: 1.2, opacity: 0.7},
    {type: "wave", bind: "freqBand", band: [0, 0.3], amplitude: 40, color: "#E8C94A"},
    {type: "ring", bind: "beat", amplitude: 30, color: "#8B6F47"},
  ],
  background: {type: "gradient", from: "#0F1013", to: "#1A1B1F"},
  transition: {duration: 800, easing: "easeOutCubic"},
  sandboxCode: null,
};

/**
 * 解析 EffectConfig 原始 JSON（容错：非法/空 → 默认配置）
 */
export function parseEffectConfig(raw: string | null | undefined): EffectConfig {
  if (!raw || !raw.trim()) {
    return DEFAULT_EFFECT_CONFIG;
  }
  try {
    const parsed = JSON.parse(raw) as Partial<EffectConfig>;
    if (!parsed || typeof parsed !== "object" || parsed.version !== 1
        || !Array.isArray(parsed.layers) || parsed.layers.length === 0) {
      return DEFAULT_EFFECT_CONFIG;
    }
    return {
      version: 1,
      palette: Array.isArray(parsed.palette) && parsed.palette.length > 0
          ? parsed.palette.filter((c) => typeof c === "string") : DEFAULT_EFFECT_CONFIG.palette,
      layers: parsed.layers
          .filter((layer) => layer && typeof layer.type === "string" && typeof layer.bind === "string")
          .map((layer) => layer as EffectLayer),
      background: parsed.background ?? DEFAULT_EFFECT_CONFIG.background,
      transition: parsed.transition ?? DEFAULT_EFFECT_CONFIG.transition,
      sandboxCode: typeof parsed.sandboxCode === "string" ? parsed.sandboxCode : null,
    };
  } catch {
    return DEFAULT_EFFECT_CONFIG;
  }
}

/** 缓动函数（§7.3 transition.easing 枚举） */
function ease(easing: string, t: number): number {
  switch (easing) {
    case "easeInOutQuad":
      return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    case "linear":
      return t;
    default: // easeOutCubic
      return 1 - Math.pow(1 - t, 3);
  }
}

/**
 * 数值线性插值（切歌配置过渡用；undefined 保持目标值）
 */
function lerpNumber(from: number | undefined, to: number | undefined, t: number): number | undefined {
  if (to === undefined) {
    return undefined;
  }
  if (from === undefined) {
    return to;
  }
  return from + (to - from) * t;
}

/** #RRGGBB → [r, g, b]（非法值返回 null） */
function hexToRgb(hex: string | undefined): [number, number, number] | null {
  if (!hex || !/^#[0-9a-fA-F]{6}$/.test(hex)) {
    return null;
  }
  return [
    parseInt(hex.slice(1, 3), 16),
    parseInt(hex.slice(3, 5), 16),
    parseInt(hex.slice(5, 7), 16),
  ];
}

/** 颜色插值（任一端非法直接取目标色） */
function lerpColor(from: string | undefined, to: string | undefined, t: number): string | undefined {
  if (to === undefined) {
    return undefined;
  }
  const a = hexToRgb(from);
  const b = hexToRgb(to);
  if (!a || !b) {
    return to;
  }
  const mix = a.map((channel, index) => Math.round(channel + (b[index] - channel) * t));
  return `#${mix.map((v) => v.toString(16).padStart(2, "0")).join("")}`;
}

/**
 * 配置过渡插值（UI-Plan §8.4：切歌/换主题参数插值过渡）
 *
 * <p>结构以目标配置为准（图层增删立即切换），数值与颜色按 transition.duration
 * 内的进度 t∈[0,1] 插值；easing 由目标配置的 transition.easing 决定。</p>
 */
export function interpolateEffectConfig(from: EffectConfig, to: EffectConfig, progress: number): EffectConfig {
  const t = ease(to.transition?.easing ?? "easeOutCubic", Math.min(Math.max(progress, 0), 1));
  const layers = to.layers.map((target, index) => {
    const source = from.layers[index];
    return {
      ...target,
      count: lerpNumber(source?.count, target.count, t),
      speed: lerpNumber(source?.speed, target.speed, t),
      opacity: lerpNumber(source?.opacity, target.opacity, t),
      amplitude: lerpNumber(source?.amplitude, target.amplitude, t),
      color: lerpColor(source?.color, target.color, t),
    };
  });
  return {
    ...to,
    layers,
    palette: to.palette.map((color, index) => lerpColor(from.palette[index], color, t) ?? color),
    background: to.background && from.background
        ? {
            ...to.background,
            from: lerpColor(from.background.from, to.background.from, t) ?? to.background.from,
            to: lerpColor(from.background.to, to.background.to, t) ?? to.background.to,
          }
        : to.background,
  };
}
