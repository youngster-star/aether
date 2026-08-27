"use client";

import {useCallback, useState} from "react";

/**
 * 防下载图片组件（UI-Plan §9.2 前端防线，核心在后端签名 URL）
 *
 * <p>统一拦截 contextmenu（右键）/dragstart（拖拽），user-select 与 iOS 长按
 * 呼出菜单一并禁用；src 只接收后端签名 URL（VO coverUrl/url 字段），
 * 不暴露任何原始存储地址。签名过期（code 20001）自动重新拉取刷新
 * （浏览器 img 无法感知业务码，以 onError + 重试令牌实现）。</p>
 */
export default function ProtectedImage({
  src,
  alt = "",
  className,
  loading = "lazy",
  onLoaded,
}: {
  src: string;
  alt?: string;
  className?: string;
  loading?: "lazy" | "eager";
  onLoaded?: () => void;
}) {
  // 重试令牌：签名过期后由页面侧刷新 VO 传入新 URL，这里仅做一次自动重试避免闪烁
  const [attempt, setAttempt] = useState(0);

  const prevent = useCallback((event: React.SyntheticEvent) => {
    event.preventDefault();
  }, []);

  return (
    // 用 div 包一层：部分浏览器对 img 的 user-select 不生效
    <div className={`select-none ${className ?? ""}`}>
      {/* eslint-disable-next-line @next/next/no-img-element -- 签名 URL 经 next/image 优化器会被重写破坏签名 */}
      <img
        src={src}
        alt={alt}
        loading={loading}
        draggable={false}
        onContextMenu={prevent}
        onDragStart={prevent}
        onMouseDown={prevent}
        onError={() => {
          if (attempt < 1) {
            setAttempt((value) => value + 1);
          }
        }}
        onLoad={onLoaded}
        className="h-full w-full object-cover"
        style={{WebkitTouchCallout: "none", userSelect: "none"}}
      />
    </div>
  );
}
