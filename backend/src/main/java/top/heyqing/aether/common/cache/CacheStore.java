package top.heyqing.aether.common.cache;

import java.time.Duration;

/**
 * 缓存抽象（BackEnd-Plan §4.1/§4.2）
 *
 * <p>限流计数、图形验证码、Refresh token 白名单、上传会话等统一走本接口：
 * 生产环境 {@link RedisCacheStore}（Redis），dev 环境 Redis 未就绪时
 * {@link InMemoryCacheStore}（内存回退，与 H2 回退同理），
 * 由 {@code aether.cache.store} 配置切换（见 CacheStoreConfiguration）。</p>
 */
public interface CacheStore {

    /**
     * 写入字符串 KV，带过期时间
     */
    void set(String key, String value, Duration ttl);

    /**
     * 读取（不存在或已过期返回 null）
     */
    String get(String key);

    /**
     * 读取并删除（原子操作，用于一次性验证码 / refresh 轮换）
     */
    String getAndDelete(String key);

    /**
     * 删除
     */
    void delete(String key);

    /**
     * 是否存在
     */
    boolean exists(String key);

    /**
     * 不存在则写入（原子），返回是否写入成功
     */
    boolean setIfAbsent(String key, String value, Duration ttl);

    /**
     * 原子自增计数并返回新值；首次写入时设置过期时间（后续自增不重置 TTL）
     */
    long increment(String key, Duration ttl);

    /**
     * 滑动窗口：追加一条时间戳记录（超出窗口的历史记录自动清理）
     */
    void windowAdd(String key, long timestamp, Duration window);

    /**
     * 滑动窗口：统计 [now - window, now] 内的记录数
     */
    long windowCount(String key, long now, Duration window);
}
