package top.heyqing.aether.service.impl.storage;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import top.heyqing.aether.model.entity.StorageRef;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.repository.StorageRefRepository;
import top.heyqing.aether.service.storage.StorageCleanService;
import top.heyqing.aether.service.storage.StorageRefService;

/**
 * 业务-文件引用管理实现（BackEnd-Plan §8.3）
 *
 * <p>解绑后引用归零的文件：status=0 标记随调用方事务一起提交/回滚
 * （回滚时标记自动撤销，不会误删仍被引用的文件）；物理删除注册在
 * afterCommit（事务未提交不执行，避免删除后事务回滚出现悬空引用）。</p>
 */
@Service
public class StorageRefServiceImpl implements StorageRefService {

    private static final Logger log = LoggerFactory.getLogger(StorageRefServiceImpl.class);

    private final StorageRefRepository storageRefRepository;
    private final StorageFileRepository storageFileRepository;
    private final StorageCleanService storageCleanService;

    public StorageRefServiceImpl(StorageRefRepository storageRefRepository,
                                 StorageFileRepository storageFileRepository,
                                 StorageCleanService storageCleanService) {
        this.storageRefRepository = storageRefRepository;
        this.storageFileRepository = storageFileRepository;
        this.storageCleanService = storageCleanService;
    }

    @Override
    public void bind(Long fileId, String bizType, Long bizId) {
        if (fileId == null) {
            return;
        }
        storageRefRepository.findByFileIdAndBizTypeAndBizId(fileId, bizType, bizId)
                .ifPresentOrElse(ref -> {
                    ref.setRefCount(ref.getRefCount() + 1);
                    storageRefRepository.save(ref);
                }, () -> {
                    StorageRef ref = new StorageRef();
                    ref.setFileId(fileId);
                    ref.setBizType(bizType);
                    ref.setBizId(bizId);
                    ref.setRefCount(1);
                    storageRefRepository.save(ref);
                });
    }

    @Override
    public void unbindFile(Long fileId, String bizType, Long bizId) {
        if (fileId == null) {
            return;
        }
        storageRefRepository.deleteByFileIdAndBizTypeAndBizId(fileId, bizType, bizId);
        markOrphan(fileId);
    }

    @Override
    public void unbindBiz(String bizType, Long bizId) {
        // 先取引用涉及的文件清单，再整体删除引用行（逐条解绑的批量优化）
        List<StorageRef> refs = storageRefRepository.findByBizTypeAndBizId(bizType, bizId);
        storageRefRepository.deleteByBizTypeAndBizId(bizType, bizId);
        refs.stream().map(StorageRef::getFileId).distinct().forEach(this::markOrphan);
    }

    /**
     * 引用归零的文件：status=0 标记（签名访问即刻 10002）+ 提交后异步物理删除
     */
    private void markOrphan(Long fileId) {
        if (storageRefRepository.countByFileId(fileId) > 0) {
            return; // 仍有其他业务引用，只减计数不动文件
        }
        storageFileRepository.findById(fileId)
                .filter(file -> file.getStatus() == 1)
                .ifPresent(file -> {
                    file.setStatus(0);
                    storageFileRepository.save(file);
                    // 事务提交后物理删除；无活动事务（如直接调用）时立即异步执行
                    if (TransactionSynchronizationManager.isSynchronizationActive()) {
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                storageCleanService.deletePhysicalAsync(fileId);
                            }
                        });
                    } else {
                        storageCleanService.deletePhysicalAsync(fileId);
                    }
                    log.info("文件引用归零，标记待清理: fileId={}, objectKey={}", fileId, file.getObjectKey());
                });
    }
}
