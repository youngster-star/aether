package top.heyqing.aether.service.impl.storage;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.service.storage.StorageCleanService;
import top.heyqing.aether.storage.StorageRouter;
import top.heyqing.aether.storage.StorageService;

/**
 * 物理文件清理实现（BackEnd-Plan §8.3）
 *
 * <p>status=0（引用归零）的文件执行物理删除；status!=0 说明已被
 * uk_md5 复活（删除后同内容重新上传），跳过不删。</p>
 */
@Service
public class StorageCleanServiceImpl implements StorageCleanService {

    private static final Logger log = LoggerFactory.getLogger(StorageCleanServiceImpl.class);

    private final StorageFileRepository storageFileRepository;
    private final StorageRouter storageRouter;
    private final Executor executor;

    public StorageCleanServiceImpl(StorageFileRepository storageFileRepository, StorageRouter storageRouter,
                                   @Qualifier("storageCleanExecutor") Executor executor) {
        this.storageFileRepository = storageFileRepository;
        this.storageRouter = storageRouter;
        this.executor = executor;
    }

    @Override
    public void deletePhysicalAsync(Long fileId) {
        executor.execute(() -> deletePhysical(fileId));
    }

    @Override
    public void deletePhysical(Long fileId) {
        storageFileRepository.findById(fileId).ifPresent(file -> {
            if (file.getStatus() != 0) {
                log.info("文件状态已恢复（复活），跳过物理删除: fileId={}", fileId);
                return;
            }
            StorageService storage = storageRouter.select(file.getStorageType());
            try {
                if (storage.exists(file.getObjectKey())) {
                    storage.delete(file.getObjectKey());
                }
                log.info("物理文件删除完成: fileId={}, objectKey={}", fileId, file.getObjectKey());
            } catch (Exception e) {
                // 失败不抛：补偿 job 每 10 分钟重试（最多 3 天，BackEnd-Plan §8.3）
                log.warn("物理文件删除失败，等待补偿 job 重试: fileId={}, objectKey={}, 原因={}",
                        fileId, file.getObjectKey(), e.getMessage());
            }
        });
    }
}
