package top.heyqing.aether.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.heyqing.aether.exception.BusinessException;

/**
 * JWT 认证过滤器（BackEnd-Plan §4.2）
 *
 * <p>解析 Authorization: Bearer 中的 Access Token，有效则注入 SecurityContext；
 * 无效/过期时不阻断请求（放行到 Security 决策），错误码经请求属性传递给
 * {@link RestAuthEntryPoint}，最终返回 20001/20002。</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** 请求属性名：token 解析失败时的错误码 */
    public static final String AUTH_ERROR_ATTR = "aether.authErrorCode";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parseAccessToken(header.substring(7));
                Long userId = Long.valueOf(claims.getSubject());
                String role = claims.get("role", String.class);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority(role)));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (BusinessException e) {
                // 让 Security 拦截未认证请求，entryPoint 按此属性返回对应错误码
                request.setAttribute(AUTH_ERROR_ATTR, e.getErrorCode().getCode());
            }
        }
        filterChain.doFilter(request, response);
    }
}
