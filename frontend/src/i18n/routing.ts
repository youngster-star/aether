import {defineRouting} from 'next-intl/routing';

/**
 * i18n 路由配置（UI-Plan §11：无语言路由前缀，URL 不变站内切换）
 *
 * <p>locale 由 Cookie（NEXT_LOCALE）+ 浏览器语言兜底决定，
 * 切换语言只重渲染不换 URL。</p>
 */
export const routing = defineRouting({
  locales: ['zh', 'en'],
  defaultLocale: 'zh',
  localePrefix: 'never',
});

export type Locale = (typeof routing.locales)[number];
