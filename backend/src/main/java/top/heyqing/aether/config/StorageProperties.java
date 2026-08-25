package top.heyqing.aether.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
