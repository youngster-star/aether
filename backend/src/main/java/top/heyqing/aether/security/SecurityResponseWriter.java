package top.heyqing.aether.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import top.heyqing.aether.common.Result;

/**
 * Security 过滤器层 JSON 响应写出工具（BackEnd-Plan §3.1）
 *
 * <p>认证入口/拒绝处理器等位于过滤器链中，无法走 Controller 序列化，
 * 统一经此工具输出 Result JSON；HTTP 状态码统一 200，业务结果看 body.code。</p>
 */
final class SecurityResponseWriter {

    private SecurityResponseWriter() {
    }

    static void writeJson(HttpServletResponse response, Result<?> body) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getWriter(), body);
    }
}
