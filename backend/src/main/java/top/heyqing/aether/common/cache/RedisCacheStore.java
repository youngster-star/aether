package top.heyqing.aether.common.cache;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis 缓存实现（生产环境，BackEnd-Plan §4.1）
 *
 * <p>计数、滑动窗口、一次性取值均通过 Lua 脚本保证原子性。</p>
 */
public class RedisCacheStore implements CacheStore {

    private final StringRedisTemplate redis;

    /** 自增且首次设置 TTL（原子） */
    private static final DefaultRedisScript<Long> INCR_TTL = new DefaultRedisScript<>(
            "local v = redis.call('INCR', KEYS[1]); if v == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; return v",
            Long.class);

    /** 滑动窗口追加：ZADD + 清理窗口外 + 续期（原子） */
    private static final DefaultRedisScript<Long> WINDOW_ADD = new DefaultRedisScript<>(
            "redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2]); "
                    + "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[3]); "
                    + "redis.call('EXPIRE', KEYS[1], ARGV[4]); return 1",
            Long.class);

    /** 滑动窗口计数：清理窗口外 + ZCARD（原子） */
    private static final DefaultRedisScript<Long> WINDOW_COUNT = new DefaultRedisScript<>(
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1]); return redis.call('ZCARD', KEYS[1])",
            Long.class);

    public RedisCacheStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public String getAndDelete(String key) {
        return redis.opsForValue().getAndDelete(key);
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, ttl));
    }

    @Override
    public long increment(String key, Duration ttl) {
        Long value = redis.execute(INCR_TTL, List.of(key), String.valueOf(ttl.toSeconds()));
        return value == null ? 0 : value;
    }

    @Override
    public void windowAdd(String key, long timestamp, Duration window) {
        // member 需全局唯一（同一毫秒多次请求不互相覆盖），score 为时间戳
        String member = timestamp + "-" + UUID.randomUUID();
        redis.execute(WINDOW_ADD, List.of(key),
                String.valueOf(timestamp), member,
                String.valueOf(timestamp - window.toMillis()),
                String.valueOf(window.toSeconds() + 60));
    }

    @Override
    public long windowCount(String key, long now, Duration window) {
        Long count = redis.execute(WINDOW_COUNT, List.of(key), String.valueOf(now - window.toMillis()));
        return count == null ? 0 : count;
    }
}
