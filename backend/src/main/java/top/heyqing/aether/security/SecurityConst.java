package top.heyqing.aether.security;

/**
 * 安全模块常量（CacheStore key 前缀、Cookie 名，BackEnd-Plan §4）
 */
public final class SecurityConst {

    private SecurityConst() {
    }

    /** 登录限流滑动窗口 key 前缀（+ip） */
    public static final String LOGIN_RATE_KEY = "login:rate:";

    /** 登录失败计数 key 前缀（+ip） */
    public static final String LOGIN_FAIL_KEY = "login:fail:";

    /** 登录锁定 key 前缀（+ip，值为解锁时间戳毫秒） */
    public static final String LOGIN_LOCK_KEY = "login:lock:";

    /** 锁定退避时长 key 前缀（+ip，值为当前锁定时长秒，指数翻倍用） */
    public static final String LOGIN_LOCK_DURATION_KEY = "login:lockduration:";

    /** 图形验证码 key 前缀（+captchaId） */
    public static final String CAPTCHA_KEY = "captcha:";

    /** Refresh Token 白名单 key 前缀（+jti） */
    public static final String REFRESH_KEY = "refresh:";

    /** 上传会话 key 前缀（+uploadId，BackEnd-Plan §8.2） */
    public static final String UPLOAD_SESSION_KEY = "upload:";

    /** Refresh Token Cookie 名 */
    public static final String REFRESH_COOKIE = "aether_refresh";

    /** 管理员角色 */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /** 上传会话 TTL（24 小时，BackEnd-Plan §8.2） */
    public static final java.time.Duration UPLOAD_SESSION_TTL = java.time.Duration.ofHours(24);
}
