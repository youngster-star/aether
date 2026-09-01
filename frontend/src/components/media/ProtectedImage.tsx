"use client";

import {useCallback, useRef, useState} from "react";

import {apiGet} from "@/lib/api/client";

/**
 * 防下载图片组件（UI-Plan §9.2 前端防线，核心在后端签名 URL）
 *
 * <p>统一拦截 contextmenu（右键）/dragstart（拖拽），user-select 与 iOS 长按
 * 呼出菜单一并禁用；src 只接收后端签名 URL（VO coverUrl/url 字段），
 * 不暴露任何原始存储地址。签名过期（图片加载失败，后端返回 20001 业务码，
 * 浏览器 img 无法感知业务码故以 onError 判定）自动重新拉取刷新，三种模式：
 * ① refreshPath（推荐）：本组件内部重拉详情接口取新 coverUrl——服务端页面
 *    只传字符串，规避 RSC 函数序列化限制；
 * ② refresh 回调：父组件（client）自行重拉并返回新 URL；
 * ③ onExpire：列表级场景由父组件统一刷新全部图片。</p>
 */
export default function ProtectedImage({
  src,
  alt = "",
  className,
  loading = "lazy",
  onLoaded,
  onExpire,
  refresh,
  refreshPath,
  noCount = false,
}: {
  src: string;
  alt?: string;
  className?: string;
  loading?: "lazy" | "eager";
  onLoaded?: () => void;
  /** 列表级过期回调（父组件负责刷新数据，如整页重拉） */
  onExpire?: () => void;
  /** 单图过期自刷新回调：返回新签名 URL（client 父组件场景） */
  refresh?: () => Promise<string | null>;
  /** 单图过期自刷新路径（服务端页面场景）：重拉详情取 coverUrl 字段 */
  refreshPath?: string;
  /** refreshPath 重拉时附带 X-Aether-No-Count（文章阅读计数不重复累计） */
  noCount?: boolean;
}) {
  // 当前生效的 src（refresh 成功后替换为新签名 URL）
  const [currentSrc, setCurrentSrc] = useState(src);
  const refreshing = useRef(false);

  // 外部 src 变化（SSR 数据更新等）时重置：渲染期间调整 state（React 官方推荐模式）
  const [prevSrc, setPrevSrc] = useState(src);
  if (prevSrc !== src) {
    setPrevSrc(src);
    setCurrentSrc(src);
  }

  const prevent = useCallback((event: React.SyntheticEvent) => {
    event.preventDefault();
  }, []);

  const handleError = useCallback(async () => {
    if (refreshing.current) {
      return; // 已有刷新进行中，避免并发重拉
    }
    refreshing.current = true;
    try {
      if (refreshPath) {
        // 模式①：内部重拉详情接口换新签名 URL
        const detail = await apiGet<{coverUrl?: string | null}>(
          refreshPath,
          undefined,
          noCount ? {noCount: true} : undefined,
        ).catch(() => null);
        if (detail?.coverUrl) {
          setCurrentSrc(detail.coverUrl);
        }
      } else if (refresh) {
        // 模式②：父组件回调
        const next = await refresh();
        if (next) {
          setCurrentSrc(next);
        }
      } else {
        // 模式③：列表级回调
        onExpire?.();
      }
    } finally {
      refreshing.current = false;
    }
  }, [refreshPath, noCount, refresh, onExpire]);

  return (
    // 用 div 包一层：部分浏览器对 img 的 user-select 不生效
    <div className={`select-none ${className ?? ""}`}>
      {/* eslint-disable-next-line @next/next/no-img-element -- 签名 URL 经 next/image 优化器会被重写破坏签名 */}
      <img
        src={currentSrc}
        alt={alt}
        loading={loading}
        draggable={false}
        onContextMenu={prevent}
        onDragStart={prevent}
        onMouseDown={prevent}
        onError={handleError}
        onLoad={onLoaded}
        className="h-full w-full object-cover"
        style={{WebkitTouchCallout: "none", userSelect: "none"}}
      />
    </div>
  );
}
