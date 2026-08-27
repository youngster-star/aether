import {getRequestConfig} from 'next-intl/server';

import {routing, type Locale} from './routing';

/**
 * 服务端 locale 词条加载（next-intl App Router 无路由方案）
 */
export default getRequestConfig(async ({requestLocale}) => {
  let locale = await requestLocale;
  if (!locale || !routing.locales.includes(locale as Locale)) {
    locale = routing.defaultLocale;
  }
  return {
    locale,
    messages: (await import(`../../messages/${locale}.json`)).default,
  };
});
