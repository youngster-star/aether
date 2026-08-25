package top.heyqing.aether.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import top.heyqing.aether.storage.LocalStorageServiceImpl;
import top.heyqing.aether.storage.OssStorageServiceImpl;
import top.heyqing.aether.storage.StorageService;

/**
 * 存储后端装配（BackEnd-Plan §8.1）
 *
 * <p>本地实现始终装配；OSS 实现仅在凭据环境变量齐全时装配
 * （未装配时 StorageRouter 对 storageType=2 给出"OSS 未配置"业务错误）。</p>
 */
@Configuration
public class StorageServiceConfiguration {

    /**
     * 本地磁盘实现（默认通道，dev 与无 OSS 凭据环境可用）
     */
    @Bean
    public StorageService localStorageService(StorageProperties properties) {
        return new LocalStorageServiceImpl(properties);
    }

    /**
     * 阿里云 OSS 实现（OSS_ENDPOINT 配置后才装配；阶段 9 部署环境凭据联调）
     */
    @Bean
    @ConditionalOnProperty(name = "OSS_ENDPOINT")
    public StorageService ossStorageService(
            @Value("${OSS_ENDPOINT}") String endpoint,
            @Value("${OSS_ACCESS_KEY:}") String accessKeyId,
            @Value("${OSS_SECRET:}") String accessKeySecret,
            @Value("${OSS_BUCKET:}") String bucket) {
        OssStorageServiceImpl.checkAvailable(endpoint, accessKeyId, accessKeySecret, bucket);
        return new OssStorageServiceImpl(endpoint, accessKeyId, accessKeySecret, bucket);
    }
}
