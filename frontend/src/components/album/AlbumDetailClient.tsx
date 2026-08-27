"use client";

import {useEffect, useRef, useState} from "react";
import {AnimatePresence, motion} from "framer-motion";
import {useTranslations} from "next-intl";

import type {AlbumImageVO} from "@/lib/api/types";
import ProtectedImage from "@/components/media/ProtectedImage";

/**
 * 图集详情交互层（UI-Plan §6.4）
 *
 * <p>瀑布流（columns-2 sm:columns-3）+ pixel-image 语义懒加载（进入视口才
 * 加载，blur 12px → 0 像素过渡）+ 点击预览层（不可下载：右键/拖拽全拦截，
 * 悬浮显示图片信息——大小必显、标题/介绍可选；ESC/遮罩关闭）。</p>
 */
export default function AlbumDetailClient({images}: {images: AlbumImageVO[]}) {
  const t = useTranslations("albums");
  const [preview, setPreview] = useState<AlbumImageVO | null>(null);

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

  return (
    <>
      <div className="columns-2 gap-4 sm:columns-3 [&>*]:mb-4">
        {images.map((image) => (
          <LazyPixelImage key={image.id} image={image} onOpen={() => setPreview(image)} />
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
 * 懒加载图片（pixel-image 语义自实现：进入视口加载，blur 12px→0 像素过渡）
 */
function LazyPixelImage({image, onOpen}: {image: AlbumImageVO; onOpen: () => void}) {
  const [visible, setVisible] = useState(false);
  const [loaded, setLoaded] = useState(false);
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

  return (
    <div ref={ref} className="break-inside-avoid">
      <button
        type="button"
        onClick={onOpen}
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
        {/* hover 遮罩提示 */}
        <span className="pointer-events-none absolute inset-0 flex items-end justify-center pb-3 text-xs
                         text-white opacity-0 transition-opacity duration-300 group-hover:opacity-100">
          <span className="rounded-pill bg-black/50 px-3 py-1 backdrop-blur-sm">
            {image.title ?? "⤢"}
          </span>
        </span>
      </button>
    </div>
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
