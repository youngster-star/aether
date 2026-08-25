package top.heyqing.aether.common.cache;

import java.time.Duration;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内存缓存实现：dev 环境 Redis 未就绪时的回退方案（BackEnd-Plan §4.1）
 *
 * <p>仅限单实例 dev 环境使用，不保证分布式一致性；生产环境必须使用
 * {@link RedisCacheStore}（aether.cache.store=redis）。</p>
 */
public class InMemoryCacheStore implements CacheStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCacheStore.class);

    /** KV 存储：key -> (value, 过期时间戳，0 表示不过期) */
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /** 滑动窗口存储：key -> 时间戳队列（按时间升序） */
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner;

    public InMemoryCacheStore() {
        // 后台清理线程：每 5 分钟惰性清理过期 KV 与空窗口队列，防止内存泄漏
        cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
        log.info("InMemoryCacheStore 已启用（dev 回退实现，生产请使用 Redis）");
    }

    private record Entry(String value, long expireAt) {
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        store.put(key, new Entry(value, now() + ttl.toMillis()));
    }

    @Override
    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (isExpired(entry)) {
            store.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    @Override
    public String getAndDelete(String key) {
        Entry entry = store.remove(key);
        if (entry == null || isExpired(entry)) {
            return null;
        }
        return entry.value();
    }

    @Override
    public void delete(String key) {
        store.remove(key);
        windows.remove(key);
    }

    @Override
    public boolean exists(String key) {
        return get(key) != null;
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Entry entry = new Entry(value, now() + ttl.toMillis());
        return store.putIfAbsent(key, entry) == null;
    }

    @Override
    public long increment(String key, Duration ttl) {
        Entry newEntry = store.compute(key, (k, old) -> {
            if (old == null || isExpired(old)) {
                return new Entry("1", now() + ttl.toMillis());
            }
            // 自增不重置过期时间
            return new Entry(String.valueOf(Long.parseLong(old.value()) + 1), old.expireAt());
        });
        return Long.parseLong(newEntry.value());
    }

    @Override
    public void windowAdd(String key, long timestamp, Duration window) {
        Deque<Long> deque = windows.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
            deque.addLast(timestamp);
            long cutoff = timestamp - window.toMillis();
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.pollFirst();
            }
        }
    }

    @Override
    public long windowCount(String key, long now, Duration window) {
        Deque<Long> deque = windows.get(key);
        if (deque == null) {
            return 0;
        }
        synchronized (deque) {
            long cutoff = now - window.toMillis();
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.pollFirst();
            }
            return deque.size();
        }
    }

    private static boolean isExpired(Entry entry) {
        return entry.expireAt() != 0 && entry.expireAt() <= now();
    }

    private void cleanup() {
        long now = now();
        store.entrySet().removeIf(e -> e.getValue().expireAt() != 0 && e.getValue().expireAt() <= now);
        windows.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
}
