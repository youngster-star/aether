package top.heyqing.aether.repository;

import java.util.List;

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
}
