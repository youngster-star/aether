import {NextResponse} from "next/server";
import type {NextRequest} from "next/server";

import {routing} from "./i18n/routing";

/**
 * Next 16 中间件（proxy 文件，UI-Plan §4.1/§11）：
 * 无语言路由前缀——locale 从 Cookie（NEXT_LOCALE）读取，
 * 经 X-NEXT-INTL-LOCALE header 注入给服务端 i18n requestLocale。
 *
 * <p>注：未使用 next-intl createMiddleware（其无前缀 rewrite 与 Next 16
 * basePath 组合存在 404 兼容问题，本文件以最小逻辑等价实现 cookie 方案）。
 * 管理端 JWT 校验在阶段 8 落地（§9.4）。</p>
 */
export default function proxy(request: NextRequest) {
  const cookieLocale = request.cookies.get("NEXT_LOCALE")?.value;
  if (cookieLocale && routing.locales.includes(cookieLocale as (typeof routing.locales)[number])) {
    const headers = new Headers(request.headers);
    headers.set("X-NEXT-INTL-LOCALE", cookieLocale);
    return NextResponse.next({request: {headers}});
  }
  // 无 Cookie：服务端按浏览器语言兜底（next-intl 默认行为）
  return NextResponse.next();
}

export const config = {
  // 仅拦截页面路由（API/静态资源不经过 locale 处理）
  matcher: ["/((?!_next|_vercel|.*\\..*).*)"],
};
