package top.heyqing.aether.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.StorageFile;

/**
 * 文件元数据表数据访问（BackEnd-Plan §6.2 storage_file）
 */
public interface StorageFileRepository extends JpaRepository<StorageFile, Long> {

    /**
     * 按 SHA-256 查已存在文件（秒传依据，BackEnd-Plan §8.2）
     */
    Optional<StorageFile> findByFileMd5AndStatus(String fileMd5, Integer status);
}
