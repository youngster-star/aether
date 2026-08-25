package top.heyqing.aether.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import top.heyqing.aether.common.cache.CacheStore;
import top.heyqing.aether.common.cache.InMemoryCacheStore;
import top.heyqing.aether.common.cache.RedisCacheStore;

/**
 * 缓存实现装配（BackEnd-Plan §4.1）
 *
 * <p>aether.cache.store=redis 装配 Redis 实现（prod）；
 * =memory 装配内存实现（dev 回退，docker Redis 就绪前）；
 * 未配置时默认内存实现，保证无 Redis 环境可启动。</p>
 */
@Configuration
public class CacheStoreConfiguration {

    /**
     * 生产实现：Redis（原子 Lua 脚本）
     */
    @Bean
    @ConditionalOnProperty(name = "aether.cache.store", havingValue = "redis")
    public CacheStore redisCacheStore(StringRedisTemplate stringRedisTemplate) {
        return new RedisCacheStore(stringRedisTemplate);
    }

    /**
     * dev 回退实现：内存（单实例）
     */
    @Bean
    @ConditionalOnProperty(name = "aether.cache.store", havingValue = "memory", matchIfMissing = true)
    public CacheStore inMemoryCacheStore() {
        return new InMemoryCacheStore();
    }
}
