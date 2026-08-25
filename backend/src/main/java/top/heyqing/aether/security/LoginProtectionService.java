package top.heyqing.aether.security;

import java.time.Duration;

import org.springframework.stereotype.Service;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.cache.CacheStore;
import top.heyqing.aether.exception.BusinessException;

/**
 * cryptex 登录三级暴力破解防护（BackEnd-Plan §4.1）
 *
 * <pre>
 * 级别 1 限流  ：同一 IP 每分钟最多 5 次登录请求（含成功），超限 10003
 * 级别 2 验证码：同一 IP 登录失败累计 3 次后，必须携带图形验证码，错误 20004
 * 级别 3 锁定  ：连续失败 10 次锁定 15 分钟，解锁后再次触发锁定时长指数翻倍
 *              （15min→30min→60min→…上限 24h），锁定期间直接 20005
 * </pre>
 *
 * <p>登录成功后重置全部失败计数。验证码错误不计入失败次数（防呆，避免误锁）。</p>
 */
@Service
public class LoginProtectionService {

    /** 每分钟最大登录请求数 */
    private static final int RATE_LIMIT = 5;

    /** 验证码接口每分钟最大请求数（防刷占缓存；TTL 自愈，阈值宽松） */
    private static final int CAPTCHA_RATE_LIMIT = 10;

    /** 限流窗口 */
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);

    /** 失败 N 次后要求验证码 */
    private static final int CAPTCHA_THRESHOLD = 3;

    /** 失败 N 次触发锁定 */
    private static final int LOCK_THRESHOLD = 10;

    /** 基础锁定时长（秒）：15 分钟 */
    private static final long LOCK_BASE_SECONDS = 15 * 60L;

    /** 锁定上限：24 小时 */
    private static final long LOCK_MAX_SECONDS = 24 * 3600L;

    /** 失败计数保留时长 */
    private static final Duration FAIL_TTL = Duration.ofHours(24);

    private final CacheStore cacheStore;

    public LoginProtectionService(CacheStore cacheStore) {
        this.cacheStore = cacheStore;
    }

    /**
     * 级别 1：入口限流（每次登录请求先过此关，含成功请求）
     *
     * @throws BusinessException 10003 操作频繁
     */
    public void checkRateLimit(String ip) {
        long now = System.currentTimeMillis();
        cacheStore.windowAdd(SecurityConst.LOGIN_RATE_KEY + ip, now, RATE_WINDOW);
        long count = cacheStore.windowCount(SecurityConst.LOGIN_RATE_KEY + ip, now, RATE_WINDOW);
        if (count > RATE_LIMIT) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    /**
     * 验证码接口限流：同一 IP 每分钟最多 10 次（防止恶意刷验证码占用缓存）
     *
     * @throws BusinessException 10003 操作频繁
     */
    public void checkCaptchaRate(String ip) {
        long now = System.currentTimeMillis();
        cacheStore.windowAdd(SecurityConst.CAPTCHA_RATE_KEY + ip, now, RATE_WINDOW);
        long count = cacheStore.windowCount(SecurityConst.CAPTCHA_RATE_KEY + ip, now, RATE_WINDOW);
        if (count > CAPTCHA_RATE_LIMIT) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    /**
     * 级别 3：锁定检查（锁定期间直接拒绝，不校验密码）
     *
     * @throws BusinessException 20005 账号已锁定
     */
    public void checkLocked(String ip) {
        String unlockAt = cacheStore.get(SecurityConst.LOGIN_LOCK_KEY + ip);
        if (unlockAt != null && Long.parseLong(unlockAt) > System.currentTimeMillis()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    /**
     * 级别 2：是否要求图形验证码（失败累计 ≥ 3 次）
     */
    public boolean requireCaptcha(String ip) {
        String failCount = cacheStore.get(SecurityConst.LOGIN_FAIL_KEY + ip);
        return failCount != null && Integer.parseInt(failCount) >= CAPTCHA_THRESHOLD;
    }

    /**
     * 记录一次密码校验失败；达到锁定阈值时立即锁定并清零失败计数
     *
     * @throws BusinessException 20005（本次失败恰好触发锁定）
     */
    public void recordFailure(String ip) {
        long failCount = cacheStore.increment(SecurityConst.LOGIN_FAIL_KEY + ip, FAIL_TTL);
        if (failCount < LOCK_THRESHOLD) {
            return;
        }
        // 触发锁定：锁定时长指数翻倍（首次 15min，上限 24h）
        long nextLockSeconds = nextLockDuration(ip);
        cacheStore.set(SecurityConst.LOGIN_LOCK_DURATION_KEY + ip, String.valueOf(nextLockSeconds),
                Duration.ofSeconds(nextLockSeconds + 3600));
        cacheStore.set(SecurityConst.LOGIN_LOCK_KEY + ip,
                String.valueOf(System.currentTimeMillis() + nextLockSeconds * 1000),
                Duration.ofSeconds(nextLockSeconds));
        // 锁定后失败计数清零：解锁后重新累计（3 次验证码 / 10 次锁定）
        cacheStore.delete(SecurityConst.LOGIN_FAIL_KEY + ip);
        throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
    }

    /**
     * 登录成功：重置失败计数、锁定与退避时长
     */
    public void recordSuccess(String ip) {
        cacheStore.delete(SecurityConst.LOGIN_FAIL_KEY + ip);
        cacheStore.delete(SecurityConst.LOGIN_LOCK_KEY + ip);
        cacheStore.delete(SecurityConst.LOGIN_LOCK_DURATION_KEY + ip);
    }

    /**
     * 计算下一次锁定时长：首次锁定 15 分钟；解锁后再次触发锁定按历史时长翻倍
     * （15min→30min→60min→…），上限 24 小时
     */
    private long nextLockDuration(String ip) {
        String last = cacheStore.get(SecurityConst.LOGIN_LOCK_DURATION_KEY + ip);
        if (last == null) {
            return LOCK_BASE_SECONDS;
        }
        return Math.min(Long.parseLong(last) * 2, LOCK_MAX_SECONDS);
    }
}
