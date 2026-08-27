package top.heyqing.aether.service.storage;

/**
 * 物理文件清理（BackEnd-Plan §8.3）
 *
 * <p>仅处理 storage_file.status=0（引用归零待清理）的文件：
 * 异步路径用于事务提交后即时清理；同步路径供补偿 job 重试。</p>
 */
public interface StorageCleanService {

    /**
     * 异步物理删除（事务提交后调用，不阻塞业务响应）
     */
    void deletePhysicalAsync(Long fileId);

    /**
     * 同步物理删除（补偿 job 重试用；status!=0 视为已复活，跳过）
     */
    void deletePhysical(Long fileId);
}
