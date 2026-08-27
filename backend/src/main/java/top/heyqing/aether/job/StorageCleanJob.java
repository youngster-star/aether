package top.heyqing.aether.job;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.service.storage.StorageCleanService;

/**
 * 物理文件补偿清理 job（BackEnd-Plan §8.3）
 *
 * <p>引用归零文件的物理删除为事务提交后异步执行，失败时由本 job
 * 每 10 分钟重试；超过 3 天仍失败则放弃（记录 warn 日志，元数据仍
 * 保持 status=0 不可访问，避免无限重试）。</p>
 */
@Component
public class StorageCleanJob {

    private static final Logger log = LoggerFactory.getLogger(StorageCleanJob.class);

    /** 补偿重试窗口（天） */
    private static final int RETRY_WINDOW_DAYS = 3;

    private final StorageFileRepository storageFileRepository;
    private final StorageCleanService storageCleanService;

    public StorageCleanJob(StorageFileRepository storageFileRepository, StorageCleanService storageCleanService) {
        this.storageFileRepository = storageFileRepository;
        this.storageCleanService = storageCleanService;
    }

    /**
     * 每 10 分钟扫描 status=0（待物理清理）的文件并重试删除
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void retryPendingCleanup() {
        List<StorageFile> pending = storageFileRepository.findByStatus(0);
        if (pending.isEmpty()) {
            return;
        }
        LocalDateTime deadline = LocalDateTime.now().minusDays(RETRY_WINDOW_DAYS);
        log.info("物理文件补偿清理 job 启动：待清理 {} 个", pending.size());
        pending.forEach(file -> {
            if (file.getUpdateTime() != null && file.getUpdateTime().isBefore(deadline)) {
                log.warn("文件超出补偿重试窗口（{} 天），放弃物理删除（元数据保持不可访问）: fileId={}, objectKey={}",
                        RETRY_WINDOW_DAYS, file.getId(), file.getObjectKey());
                return;
            }
            storageCleanService.deletePhysical(file.getId());
        });
    }
}
