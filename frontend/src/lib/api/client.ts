import type {ApiResult} from './types';

/**
 * API 客户端（BackEnd-Plan §5.1：外部完整路径 /aether/api/v1）
 *
 * <p>统一解包 Result：code!=0 抛 ApiError（message 供 UI 直接展示）；
 * 开发环境经 next.config.ts rewrite 代理到本地后端。</p>
 */

const API_BASE = "/aether/api/v1";

export class ApiError extends Error {
  readonly code: number;

  constructor(code: number, message: string) {
    super(message);
    this.code = code;
  }
}

/**
 * GET 请求（自动拼接 query 参数并解包 Result）
 */
export async function apiGet<T>(
  path: string,
  params?: Record<string, string | number | undefined>,
  options?: {noCount?: boolean},
): Promise<T> {
  const url = new URL(`${API_BASE}${path}`, window.location.origin);
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    });
  }
  const response = await fetch(url.toString(), {
    cache: "no-store",
    // 封面签名过期重拉等非阅读场景跳过阅读计数（与 apiServerGet 的 noCount 语义一致）
    headers: options?.noCount ? {"X-Aether-No-Count": "1"} : undefined,
  });
  return unwrap<T>(response);
}

/**
 * 解包统一返回体（网络错误/非 2xx/业务 code 非 0 统一抛 ApiError）
 */
async function unwrap<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new ApiError(50001, `HTTP ${response.status}`);
  }
  const body = (await response.json()) as ApiResult<T>;
  if (body.code !== 0) {
    throw new ApiError(body.code, body.message);
  }
  return body.data;
}
