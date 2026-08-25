package top.heyqing.aether.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 工具类
 */
public final class IpUtil {

    private IpUtil() {
    }

    /**
     * 获取客户端真实 IP（BackEnd-Plan §4.5）：
     * 1. X-Real-IP：nginx 覆盖式设置（proxy_set_header X-Real-IP $remote_addr），可信
     * 2. X-Forwarded-For 第一个：无 X-Real-IP 时的回退（注意该头客户端可控，
     *    仅在可信代理覆盖设置时使用；当前部署拓扑由 nginx 统一提供 X-Real-IP）
     * 3. remoteAddr：直连场景兜底
     *
     * @param request HTTP 请求
     * @return 客户端 IP（IPv4/IPv6）
     */
    public static String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // 格式为 "client, proxy1, proxy2"，第一个为真实客户端
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
