package top.heyqing.aether.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.Result;

/**
 * 权限不足入口：已认证但无权访问时返回统一 Result 20001
 * （当前仅单一 ADMIN 角色，此场景主要覆盖 denyAll 兜底与未来角色扩展）
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        SecurityResponseWriter.writeJson(response, Result.fail(ErrorCode.UNAUTHORIZED));
    }
}
