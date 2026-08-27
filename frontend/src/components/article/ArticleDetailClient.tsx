"use client";

import {useEffect, useState} from "react";

import {apiGet} from "@/lib/api/client";
import type {ArticleDetailVO} from "@/lib/api/types";

/**
 * 详情页客户端 hydrate 组件：
 * 浏览器端重新请求详情接口，以真实访客 IP 触发阅读计数（后端 24h 去重），
 * 并把最新阅读数水合到页面（SSR 预取请求带 X-Aether-No-Count 不计数）。
 */
export default function ArticleDetailClient({
  id,
  initialReadingCount,
}: {
  id: number;
  initialReadingCount: number;
}) {
  const [readingCount, setReadingCount] = useState(initialReadingCount);

  useEffect(() => {
    let cancelled = false;
    apiGet<ArticleDetailVO>(`/articles/${id}`)
      .then((detail) => {
        if (!cancelled) {
          setReadingCount(detail.readingCount);
        }
      })
      .catch(() => {
        // 计数补偿失败不影响阅读体验（保持 SSR 初值）
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  return readingCount;
}
