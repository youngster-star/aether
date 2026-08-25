package top.heyqing.aether.service.impl.auth;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.LoginRequest;
import top.heyqing.aether.model.entity.SysUser;
import top.heyqing.aether.model.vo.CaptchaVO;
import top.heyqing.aether.model.vo.RefreshResult;
import top.heyqing.aether.repository.SysUserRepository;
import top.heyqing.aether.security.CaptchaService;
import top.heyqing.aether.security.JwtService;
import top.heyqing.aether.security.LoginProtectionService;
import top.heyqing.aether.security.SecurityConst;
import top.heyqing.aether.service.auth.AuthService;

/**
 * 认证服务实现（BackEnd-Plan §4.1/§4.2）
 *
 * <p>登录流程（三级防护）：入口限流 → 锁定检查 → 验证码要求检查 → Argon2 密码校验；
 * 失败计数由 LoginProtectionService 维护，成功后重置。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** 管理员登录名（内部固定，登录接口不收用户名） */
    private static final String ADMIN_USERNAME = "cryptex";

    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginProtectionService protectionService;
    private final CaptchaService captchaService;
    private final JwtService jwtService;

    public AuthServiceImpl(SysUserRepository sysUserRepository, PasswordEncoder passwordEncoder,
                           LoginProtectionService protectionService, CaptchaService captchaService,
                           JwtService jwtService) {
        this.sysUserRepository = sysUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.protectionService = protectionService;
        this.captchaService = captchaService;
        this.jwtService = jwtService;
    }

    @Override
    public CaptchaVO captcha(String ip) {
        // 验证码接口限流：防刷占缓存（BackEnd-Plan §4.1 辅助防护）
        protectionService.checkCaptchaRate(ip);
        return captchaService.create();
    }

    @Override
    public RefreshResult login(LoginRequest request, String ip) {
        // 级别 1：入口限流（含成功请求）
        protectionService.checkRateLimit(ip);
        // 级别 3：锁定期间直接拒绝
        protectionService.checkLocked(ip);
        // 级别 2：失败累计 ≥3 次后强制图形验证码（验证码错误不增加失败计数，防呆）
        if (protectionService.requireCaptcha(ip)
                && !captchaService.verify(request.captchaId(), request.captchaCode())) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }

        SysUser user = sysUserRepository.findByUsername(ADMIN_USERNAME)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_ERROR));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // 密码错误：累计失败（第 10 次触发锁定并抛 20005）
            protectionService.recordFailure(ip);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 登录成功：重置防护状态 + 更新登录信息 + 签发双 token（Access 入 body，Refresh 入 Cookie）
        protectionService.recordSuccess(ip);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        sysUserRepository.save(user);

        String accessToken = jwtService.createAccessToken(user.getId(), SecurityConst.ROLE_ADMIN);
        String refreshToken = jwtService.issueRefreshToken(user.getId());
        return new RefreshResult(accessToken, refreshToken);
    }

    @Override
    public RefreshResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // 轮换：白名单原子作废旧 jti（防重放），签发新 Refresh（由 Controller 写回 Cookie）
        JwtService.RefreshRotation rotation = jwtService.rotateRefreshToken(refreshToken);
        String accessToken = jwtService.createAccessToken(rotation.userId(), SecurityConst.ROLE_ADMIN);
        return new RefreshResult(accessToken, rotation.token());
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        jwtService.revokeRefreshToken(refreshToken);
    }
}
