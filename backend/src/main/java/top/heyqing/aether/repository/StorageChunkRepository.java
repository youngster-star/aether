package top.heyqing.aether.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.StorageChunk;

/**
 * 分片上传进度表数据访问（BackEnd-Plan §6.2 storage_chunk）
 */
public interface StorageChunkRepository extends JpaRepository<StorageChunk, Long> {

    /**
     * 查询上传会话已传分片（断点续传依据，按序号升序）
     */
    List<StorageChunk> findByUploadIdOrderByChunkIndexAsc(String uploadId);

    /**
     * 查询指定分片记录（分片幂等覆盖用）
     */
    Optional<StorageChunk> findByUploadIdAndChunkIndex(String uploadId, Integer chunkIndex);

    /**
     * 统计会话已传分片数
     */
    long countByUploadIdAndStatus(String uploadId, Integer status);
}
