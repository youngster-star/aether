package top.heyqing.aether.config;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;

/**
 * 存储配置（BackEnd-Plan §8）
 *
 * <p>前缀 aether.storage；生产通过环境变量覆盖（STORAGE_DIR/SIGN_SECRET）。</p>
 */
@ConfigurationProperties(prefix = "aether.storage")
public class StorageProperties {

    /** 本地存储根目录（LocalStorageServiceImpl 使用） */
    private String localBaseDir = "./data/storage";

    /** 分片大小（MB），默认 8 */
    private int chunkSizeMb = 8;

    /** 签名 URL 密钥（HMAC-SHA256，生产必须环境变量注入） */
    private String signSecret;

    /** 签名 URL 有效期（秒），默认 600（10 分钟，BackEnd-Plan §4.4） */
    private long signExpireSeconds = 600;

    /**
     * fail-fast 校验：签名密钥未配置或过短时启动失败（与 JwtService 同策略，
     * 防止生产误配置导致签名访问 500 而非明确报错）
     */
    @PostConstruct
    void validate() {
        if (signSecret == null || signSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("签名密钥未配置或长度不足 32 字节，请检查 SIGN_SECRET 环境变量");
        }
    }

    public String getLocalBaseDir() {
        return localBaseDir;
    }

    public void setLocalBaseDir(String localBaseDir) {
        this.localBaseDir = localBaseDir;
    }

    public int getChunkSizeMb() {
        return chunkSizeMb;
    }

    public void setChunkSizeMb(int chunkSizeMb) {
        this.chunkSizeMb = chunkSizeMb;
    }

    public String getSignSecret() {
        return signSecret;
    }

    public void setSignSecret(String signSecret) {
        this.signSecret = signSecret;
    }

    public long getSignExpireSeconds() {
        return signExpireSeconds;
    }

    public void setSignExpireSeconds(long signExpireSeconds) {
        this.signExpireSeconds = signExpireSeconds;
    }

    /** 分片大小（字节） */
    public long chunkSizeBytes() {
        return (long) chunkSizeMb * 1024 * 1024;
    }
}
