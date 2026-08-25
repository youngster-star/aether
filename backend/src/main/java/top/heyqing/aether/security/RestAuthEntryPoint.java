package top.heyqing.aether.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.Result;

/**
 * 未认证入口：受保护接口无有效 Access Token 时返回统一 Result（BackEnd-Plan §3.1/§4.2）
 *
 * <p>默认 20001；若 JwtAuthFilter 已识别为 token 过期则返回 20002。
 * 响应体为 JSON Result，HTTP 状态码统一 200（业务结果看 body.code）。</p>
 */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Integer authErrorCode = (Integer) request.getAttribute(JwtAuthFilter.AUTH_ERROR_ATTR);
        ErrorCode errorCode = ErrorCode.fromCode(
                authErrorCode == null ? ErrorCode.UNAUTHORIZED.getCode() : authErrorCode);
        SecurityResponseWriter.writeJson(response, Result.fail(errorCode));
    }
}
