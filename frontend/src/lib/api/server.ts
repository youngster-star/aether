import {ApiError} from "./client";
import type {ApiResult} from "./types";

/**
 * 服务端（RSC）API 请求：直接请求后端 origin，绕过 rewrite 代理
 *
 * <p>RSC 无 window，不能用相对路径 fetch；后端地址默认本地 8080，
 * 部署环境经 AETHER_BACKEND_ORIGIN 环境变量注入（nginx 反代后同域可传空）。
 * 页面数据全部 no-store（内容随发布即时可见）。</p>
 */

const BACKEND_ORIGIN = process.env.AETHER_BACKEND_ORIGIN ?? "http://localhost:8080";

export async function apiServerGet<T>(
  path: string,
  params?: Record<string, string | number | undefined>,
  options?: {noCount?: boolean},
): Promise<T> {
  const url = new URL(`${BACKEND_ORIGIN}/aether/api/v1${path}`);
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    });
  }
  const response = await fetch(url.toString(), {
    cache: "no-store",
    // RSC 预取详情时跳过阅读计数（渲染服务器 IP 非访客 IP，计数由浏览器 hydrate 补偿）
    headers: options?.noCount ? {"X-Aether-No-Count": "1"} : undefined,
  });
  if (!response.ok) {
    throw new ApiError(50001, `HTTP ${response.status}`);
  }
  const body = (await response.json()) as ApiResult<T>;
  if (body.code !== 0) {
    throw new ApiError(body.code, body.message);
  }
  return body.data;
}
