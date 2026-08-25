package top.heyqing.aether.service.auth;

import top.heyqing.aether.model.dto.LoginRequest;
import top.heyqing.aether.model.vo.CaptchaVO;
import top.heyqing.aether.model.vo.LoginVO;
import top.heyqing.aether.model.vo.RefreshResult;

/**
 * 认证服务（cryptex 登录 + JWT 双 token，BackEnd-Plan §4.1/§4.2）
 */
public interface AuthService {

    /**
     * 生成图形验证码（登录失败累计 3 次后前端先调此接口）
     */
    CaptchaVO captcha();

    /**
     * 登录：仅校验密码，三级防护（限流/验证码/锁定）由 LoginProtectionService 保证
     *
     * @param request 登录请求
     * @param ip      客户端 IP（限流/防暴力维度）
     * @return Access Token（Refresh Token 经 Cookie 下发，见 AuthController）
     */
    LoginVO login(LoginRequest request, String ip);

    /**
     * 刷新 Access Token（Refresh Token 白名单轮换，旧 token 立即作废）
     *
     * @param refreshToken 请求 Cookie 中的 Refresh Token
     * @return 新 Access Token 与新 Refresh Token（Controller 写回 Cookie）
     */
    RefreshResult refresh(String refreshToken);

    /**
     * 登出：作废 Refresh Token（幂等）
     *
     * @param refreshToken 请求 Cookie 中的 Refresh Token（可空）
     */
    void logout(String refreshToken);
}
