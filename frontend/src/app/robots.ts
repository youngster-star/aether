import type {MetadataRoute} from "next";

/**
 * robots 全站禁爬（UI-Plan §9.1，用户明确要求；生产 nginx 直接托管）
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {userAgent: "*", disallow: "/aether/"},
  };
}
