package top.heyqing.aether.controller.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import top.heyqing.aether.aspect.OperationLog;
import top.heyqing.aether.common.Result;
import top.heyqing.aether.config.JwtProperties;
import top.heyqing.aether.model.dto.LoginRequest;
import top.heyqing.aether.model.vo.CaptchaVO;
import top.heyqing.aether.model.vo.LoginVO;
import top.heyqing.aether.model.vo.RefreshResult;
import top.heyqing.aether.security.SecurityConst;
import top.heyqing.aether.service.auth.AuthService;
import top.heyqing.aether.util.IpUtil;

/**
 * 认证接口（BackEnd-Plan §5.2 认证 auth）
 *
 * <p>外部路径：/aether/api/v1/auth/**。登录仅输入密码（用户名内部固定 cryptex），
 * Refresh Token 经 HttpOnly Cookie（aether_refresh）下发与轮换。</p>
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 获取图形验证码（登录失败累计 3 次后前端调用）
     */
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(authService.captcha());
    }

    /**
     * 登录：仅密码；三级暴力防护（限流/验证码/锁定）见 LoginProtectionService
     */
    @PostMapping("/login")
    @OperationLog(module = "认证", action = "登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request,
                                 HttpServletRequest httpRequest, HttpServletResponse response) {
        LoginVO loginVO = authService.login(request, IpUtil.clientIp(httpRequest));
        applyRefreshCookie(response, loginVO.accessToken(), refreshMaxAgeSeconds());
        return Result.ok(loginVO);
    }

    /**
     * 刷新 Access Token：Refresh Token 轮换，旧 token 立即作废（防重放）
     */
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(
            @CookieValue(name = SecurityConst.REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        RefreshResult result = authService.refresh(refreshToken);
        applyRefreshCookie(response, result.refreshToken(), refreshMaxAgeSeconds());
        return Result.ok(new LoginVO(result.accessToken()));
    }

    /**
     * 登出：作废 Refresh Token 并清除 Cookie（幂等）
     */
    @PostMapping("/logout")
    @OperationLog(module = "认证", action = "登出")
    public Result<Void> logout(
            @CookieValue(name = SecurityConst.REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        applyRefreshCookie(response, "", 0);
        return Result.ok();
    }

    /**
     * 写入/清除 Refresh Cookie（HttpOnly + SameSite=Lax，防 XSS 读取与 CSRF）
     *
     * @param maxAgeSeconds 有效期秒数；0 表示删除 Cookie
     */
    private void applyRefreshCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(SecurityConst.REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshSecure())
                .sameSite("Lax")
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private int refreshMaxAgeSeconds() {
        return (int) Duration.ofDays(jwtProperties.getRefreshExpireDays()).toSeconds();
    }
}
