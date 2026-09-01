/**
 * 歌词解析与同步（BackEnd-Plan §7.6 / UI-Plan §6.6）
 *
 * <p>LRC 时间轴逐行解析（含多时间戳行），纯文本按"总行数 ÷ 总时长"均分兜底；
 * lyric_offset 全局偏移（毫秒）统一前移/后移全部行。</p>
 */

/** 单行歌词 */
export interface LyricLine {
  /** 行起始时间（秒，已应用全局偏移） */
  time: number;
  text: string;
}

/** 元数据标签行（[ti:]/[ar:]/[al:]/[by:]/[offset:] 等，不参与滚动渲染） */
const META_LINE = /^\[(?:ti|ar|al|by|offset|length|re|ve):/i;

/**
 * 解析歌词
 *
 * @param lyricText     歌词原文（LRC 或纯文本，可空）
 * @param offsetMs      全局偏移（毫秒，正=延后 负=提前）
 * @param totalDuration 曲目时长（秒，纯文本均分用；0 时纯文本退化为静态列表）
 */
export function parseLyrics(
    lyricText: string | null | undefined,
    offsetMs: number,
    totalDuration: number,
): LyricLine[] {
  if (!lyricText || !lyricText.trim()) {
    return [];
  }
  const lines = lyricText.split(/\r?\n/);
  const offsetSeconds = (Number.isFinite(offsetMs) ? offsetMs : 0) / 1000;
  const lrcLines: LyricLine[] = [];
  let hasTimestamp = false;
  for (const line of lines) {
    const matches = [...line.matchAll(/\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]/g)];
    if (matches.length > 0) {
      hasTimestamp = true;
      const text = line.replace(/\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]/g, "").trim();
      if (!text) {
        continue; // 纯时间标记行（无歌词内容）不渲染
      }
      for (const match of matches) {
        const minutes = Number(match[1]);
        const seconds = Number(match[2]);
        const fractionRaw = match[3] ?? "0";
        // 两位=百分秒 三位=毫秒，统一换算为秒
        const fraction = Number(fractionRaw) / (fractionRaw.length === 3 ? 1000 : 100);
        lrcLines.push({time: minutes * 60 + seconds + fraction + offsetSeconds, text});
      }
      continue;
    }
    if (line.trim()) {
      // 元数据行（[ti:xx] 等标签形态）跳过，其余留待纯文本均分
      if (META_LINE.test(line.trim())) {
        continue;
      }
      lrcLines.push({time: Number.NaN, text: line.trim()});
    }
  }
  if (hasTimestamp) {
    return lrcLines
        .filter((item) => Number.isFinite(item.time))
        .sort((a, b) => a.time - b.time);
  }
  // 纯文本兜底：按总行数 ÷ 总时长均分（粗略同步）
  const textLines = lrcLines.map((item) => item.text);
  if (textLines.length === 0 || totalDuration <= 0) {
    return textLines.map((text, index) => ({time: index * 2, text}));
  }
  const step = totalDuration / textLines.length;
  return textLines.map((text, index) => ({time: index * step + offsetSeconds, text}));
}

/**
 * 二分定位当前行（播放位置 → 应高亮的行下标；-1 = 尚未进入第一行）
 */
export function currentLyricIndex(lines: LyricLine[], position: number): number {
  let low = 0;
  let high = lines.length - 1;
  let result = -1;
  while (low <= high) {
    const mid = (low + high) >>> 1;
    if (lines[mid].time <= position) {
      result = mid;
      low = mid + 1;
    } else {
      high = mid - 1;
    }
  }
  return result;
}
