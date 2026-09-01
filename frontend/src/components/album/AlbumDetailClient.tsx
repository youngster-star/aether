"use client";

import {useCallback, useEffect, useRef, useState} from "react";
import {AnimatePresence, motion} from "framer-motion";
import {useTranslations} from "next-intl";

import {apiGet} from "@/lib/api/client";
import type {AlbumDetailVO, AlbumImageVO} from "@/lib/api/types";
import ProtectedImage from "@/components/media/ProtectedImage";
import BlurFade from "@/components/ui/BlurFade";

/**
 * 图集详情交互层（UI-Plan §6.4）
 *
 * <p>瀑布流（columns-2 sm:columns-3）+ pixel-image 语义懒加载（进入视口才
 * 加载，blur 12px → 0 像素过渡）+ 点击预览层（不可下载：右键/拖拽全拦截，
 * 悬浮显示图片信息——大小必显、标题/介绍可选；ESC/遮罩关闭）。
 * 签名过期（§9.2）时客户端重拉详情换新签名 URL，并同步刷新预览层当前图。</p>
 */
export default function AlbumDetailClient({
  albumId,
  images: initialImages,
}: {
  albumId: number;
  images: AlbumImageVO[];
}) {
  const t = useTranslations("albums");
  const [images, setImages] = useState<AlbumImageVO[]>(initialImages);
  const [preview, setPreview] = useState<AlbumImageVO | null>(null);
  const refreshing = useRef(false);

  // ESC 关闭预览
  useEffect(() => {
    if (!preview) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setPreview(null);
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [preview]);

  /**
   * 签名过期刷新：重拉详情换新签名 URL；预览层同图同步更新
   */
  const refreshImages = useCallback(async () => {
    if (refreshing.current) {
      return; // 已有刷新进行中（多张图同时过期只触发一次重拉）
    }
    refreshing.current = true;
    try {
      const detail = await apiGet<AlbumDetailVO>(`/albums/${albumId}`);
      setImages(detail.images);
      setPreview((current) =>
        current === null
          ? null
          : detail.images.find((image) => image.id === current.id) ?? null,
      );
    } catch {
      // 后端暂不可用：保留现状，网络恢复后可再次触发
    } finally {
      refreshing.current = false;
    }
  }, [albumId]);

  return (
    <>
      <div className="columns-2 gap-4 sm:columns-3 [&>*]:mb-4">
        {images.map((image) => (
          <LazyPixelImage
            key={image.id}
            image={image}
            onOpen={() => setPreview(image)}
            onExpire={refreshImages}
          />
        ))}
      </div>

      {/* 预览层（不可下载） */}
      <AnimatePresence>
        {preview && (
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-label={preview.title ?? t("listTitle")}
            initial={{opacity: 0}}
            animate={{opacity: 1}}
            exit={{opacity: 0}}
            className="fixed inset-0 z-[70] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
            onClick={() => setPreview(null)}
          >
            {/* 图片容器：点击不冒泡关闭（方便在图片上操作） */}
            <motion.figure
              initial={{scale: 0.94, y: 16}}
              animate={{scale: 1, y: 0}}
              exit={{scale: 0.94, y: 16}}
              transition={{duration: 0.25, ease: [0.22, 1, 0.36, 1]}}
              className="relative max-h-[85vh] max-w-[85vw] select-none overflow-hidden rounded-md bg-background shadow-aether"
              onClick={(event) => event.stopPropagation()}
            >
              <ProtectedImage
                src={preview.url}
                alt={preview.title ?? ""}
                className="max-h-[75vh]"
                loading="eager"
                onExpire={refreshImages}
              />
              {/* 悬浮信息：大小必显，标题/介绍可选（UI-Plan §6.4） */}
              <figcaption className="flex flex-wrap items-center gap-x-4 gap-y-1 border-t border-border px-4 py-3 text-xs text-muted">
                {preview.title && (
                  <span className="font-display text-sm font-bold text-foreground">
                    {preview.title}
                  </span>
                )}
                {preview.intro && <span>{preview.intro}</span>}
                <span className="ml-auto whitespace-nowrap">
                  {preview.width && preview.height
                    ? `${preview.width}×${preview.height} · `
                    : ""}
                  {formatBytes(preview.size)}
                </span>
              </figcaption>
            </motion.figure>
            <p className="pointer-events-none absolute bottom-4 left-1/2 -translate-x-1/2 text-xs text-white/60">
              {t("previewTip")}
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}

/**
 * 懒加载图片（magicui 语义自实现）：
 * pixel-image——进入视口加载 + blur 12px→0 像素过渡；
 * blur-fade——卡片入场模糊淡入；
 * lens——hover 圆形放大镜跟随鼠标（zoomFactor 2，§6.4）。
 */
function LazyPixelImage({
  image,
  onOpen,
  onExpire,
}: {
  image: AlbumImageVO;
  onOpen: () => void;
  onExpire: () => void;
}) {
  const [visible, setVisible] = useState(false);
  const [loaded, setLoaded] = useState(false);
  // lens 放大镜位置（0~1 相对坐标），null = 未 hover
  const [lens, setLens] = useState<{x: number; y: number} | null>(null);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const node = ref.current;
    if (!node) {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          setVisible(true);
          observer.disconnect();
        }
      },
      {rootMargin: "200px"},
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  // lens：记录鼠标在图内的相对位置，圆形放大区按百分比取景（2 倍放大）
  const handleMove = (event: React.MouseEvent<HTMLButtonElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    setLens({
      x: Math.min(Math.max((event.clientX - rect.left) / rect.width, 0), 1),
      y: Math.min(Math.max((event.clientY - rect.top) / rect.height, 0), 1),
    });
  };
  const prevent = (event: React.SyntheticEvent) => event.preventDefault();

  return (
    <BlurFade>
      <div ref={ref} className="break-inside-avoid">
        <button
          type="button"
          onClick={onOpen}
          onMouseMove={handleMove}
          onMouseLeave={() => setLens(null)}
          onContextMenu={prevent}
          onDragStart={prevent}
          aria-label={image.title ?? ""}
          className="group relative block w-full overflow-hidden rounded-md border border-border
                     bg-accent/10 transition-colors hover:border-accent"
          style={{aspectRatio: image.width && image.height ? `${image.width} / ${image.height}` : "4 / 3"}}
        >
          {visible ? (
            <ProtectedImage
              src={image.url}
              alt={image.title ?? ""}
              onLoaded={() => setLoaded(true)}
              onExpire={onExpire}
            />
          ) : (
            // 未进入视口：占位色块（保持瀑布流高度）
            <div className="absolute inset-0" />
          )}
          {/* 像素过渡：blur(12px) → 0；未加载完成前显示底色 */}
          <div
            aria-hidden
            className={`pointer-events-none absolute inset-0 bg-accent/10 transition-[opacity,filter]
                        duration-500 ${loaded ? "opacity-0 blur-0" : "opacity-100 blur-xl"}`}
          />
          {/* lens 放大镜：圆形取景区跟随鼠标，签名 URL 作 2 倍背景图 */}
          {lens !== null && visible && loaded && (
            <span
              aria-hidden
              className="pointer-events-none absolute h-40 w-40 rounded-full border-2 border-background
                         shadow-aether"
              style={{
                left: `calc(${lens.x * 100}% - 80px)`,
                top: `calc(${lens.y * 100}% - 80px)`,
                backgroundImage: `url("${image.url}")`,
                backgroundSize: "200% 200%",
                backgroundPosition: `${lens.x * 100}% ${lens.y * 100}%`,
                backgroundRepeat: "no-repeat",
                backgroundColor: "var(--card)",
              }}
            />
          )}
          {/* hover 遮罩提示 */}
          <span className="pointer-events-none absolute inset-x-0 bottom-0 flex justify-center pb-3 text-xs
                           text-white opacity-0 transition-opacity duration-300 group-hover:opacity-100">
            <span className="rounded-pill bg-black/50 px-3 py-1 backdrop-blur-sm">
              {image.title ?? "⤢"}
            </span>
          </span>
        </button>
      </div>
    </BlurFade>
  );
}

/**
 * 字节数人性化展示（预览悬浮信息必显项，UI-Plan §6.4）
 */
function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
