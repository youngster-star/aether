package top.heyqing.aether.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 工具类
 */
public final class IpUtil {

    private IpUtil() {
    }

    /**
     * 获取客户端真实 IP：优先取 X-Forwarded-For 第一个（nginx 已透传），
     * 无代理头时回退 remoteAddr。
     *
     * @param request HTTP 请求
     * @return 客户端 IP（IPv4/IPv6）
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // 格式为 "client, proxy1, proxy2"，第一个为真实客户端
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
