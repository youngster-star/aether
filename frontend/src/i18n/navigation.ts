import {createNavigation} from 'next-intl/navigation';

import {routing} from './routing';

/**
 * 无前缀导航工具（Link/useRouter 等），全站统一走本入口，
 * 禁止直接 import next/link（保证 locale 逻辑一致，UI-Plan §4.2）
 */
export const {Link, redirect, usePathname, useRouter, getPathname} = createNavigation(routing);
