"use client";

import { useEffect, useState } from "react";

/**
 * 主页（阶段0脚手架占位）
 * 阶段0完成标准：前端经 /aether/api/health 调通后端（BackEnd-Plan §5.2）
 * 完整主页版式见 UI-Plan §6.1，阶段2实现
 */
export default function Home() {
  const [status, setStatus] = useState<string>("检测中...");

  useEffect(() => {
    // 经 Next 开发代理（rewrites）转发到后端 http://localhost:8080/aether/api/health
    fetch("/aether/api/health")
      .then((res) => res.json())
      .then((data) => setStatus(data.status ?? "UNKNOWN"))
      .catch(() => setStatus("DOWN"));
  }, []);

  return (
    <main className="flex min-h-full flex-col items-center justify-center gap-4">
      <h1 className="text-4xl font-bold">Aether | 以太</h1>
      <p className="text-lg">
        后端连接状态：<span className="font-mono">{status}</span>
      </p>
    </main>
  );
}
