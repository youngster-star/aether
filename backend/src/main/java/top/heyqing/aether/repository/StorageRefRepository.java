package top.heyqing.aether.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.StorageRef;

/**
 * 业务-文件引用表数据访问（BackEnd-Plan §6.2 storage_ref，删除一致性核心）
 */
public interface StorageRefRepository extends JpaRepository<StorageRef, Long> {

    /**
     * 查询某文件的所有引用（判断 ref_count 归零时是否物理删除）
     */
    List<StorageRef> findByFileId(Long fileId);

    /**
     * 查询单条引用（bind 幂等递增用）
     */
    Optional<StorageRef> findByFileIdAndBizTypeAndBizId(Long fileId, String bizType, Long bizId);

    /**
     * 查询某业务记录的全部引用（解绑时收集涉及文件）
     */
    List<StorageRef> findByBizTypeAndBizId(String bizType, Long bizId);

    /**
     * 删除某业务记录的全部引用（业务删除时调用）
     */
    void deleteByBizTypeAndBizId(String bizType, Long bizId);

    /**
     * 删除单条引用（业务更新换文件时调用）
     */
    void deleteByFileIdAndBizTypeAndBizId(Long fileId, String bizType, Long bizId);

    /**
     * 某文件剩余引用数（归零=可物理删除）
     */
    long countByFileId(Long fileId);
}
