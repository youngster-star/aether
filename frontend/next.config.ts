import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // path 部署：www.heyqing.top/aether（BackEnd-Plan §11.3）
  basePath: "/aether",
  // 开发环境：将 /aether/api/* 代理到本地后端（后端 context-path 同为 /aether/api，透传不动路径）
  // 注意：Next 16 默认会给 rewrite 的 source/destination 自动加 basePath 前缀，
  // 外部代理需显式 basePath: false（见 node_modules/next/dist/docs rewrites 文档）
  async rewrites() {
    return [
      {
        source: "/aether/api/:path*",
        destination: "http://localhost:8080/aether/api/:path*",
        basePath: false,
      },
    ];
  },
};

export default nextConfig;
