package top.heyqing.aether.common;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.heyqing.aether.util.IpUtil;

/**
 * 请求链路过滤器：为每个请求生成 requestId（BackEnd-Plan §3.1）
 *
 * <p>requestId 同时写入响应头 X-Request-Id 与日志 MDC，贯穿全链路便于排障；
 * 客户端 IP 解析后放入 {@link RequestContext}，供限流/日志等场景使用。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // UUID 前 16 位十六进制作为链路 ID（足够唯一且简短）
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String clientIp = IpUtil.clientIp(request);
        RequestContext.set(requestId, clientIp);
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            RequestContext.clear();
        }
    }
}
