package top.heyqing.aether.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置（BackEnd-Plan §4.2）
 *
 * <p>前缀 aether.jwt；secret 由环境变量 JWT_SECRET 注入（生产必填，dev 默认值见 application-dev.yml）。</p>
 */
@ConfigurationProperties(prefix = "aether.jwt")
public class JwtProperties {

    /** 签名密钥（HMAC-SHA256，至少 32 字节；生产必须环境变量注入） */
    private String secret;

    /** Access Token 有效期（分钟），默认 30 */
    private int accessExpireMinutes = 30;

    /** Refresh Token 有效期（天），默认 7 */
    private int refreshExpireDays = 7;

    /** Refresh Cookie 是否 Secure（https 环境开启；dev http 环境关闭） */
    private boolean refreshSecure = true;

    /** Refresh Cookie 路径（与前端 basePath 一致） */
    private String refreshCookiePath = "/aether";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getAccessExpireMinutes() {
        return accessExpireMinutes;
    }

    public void setAccessExpireMinutes(int accessExpireMinutes) {
        this.accessExpireMinutes = accessExpireMinutes;
    }

    public int getRefreshExpireDays() {
        return refreshExpireDays;
    }

    public void setRefreshExpireDays(int refreshExpireDays) {
        this.refreshExpireDays = refreshExpireDays;
    }

    public boolean isRefreshSecure() {
        return refreshSecure;
    }

    public void setRefreshSecure(boolean refreshSecure) {
        this.refreshSecure = refreshSecure;
    }

    public String getRefreshCookiePath() {
        return refreshCookiePath;
    }

    public void setRefreshCookiePath(String refreshCookiePath) {
        this.refreshCookiePath = refreshCookiePath;
    }
}
