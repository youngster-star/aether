package top.heyqing.aether.repository;

import java.util.List;
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

    /**
     * 按 SHA-256 查任意状态文件（merge 落库冲突处理：uk_md5 唯一约束下复活 status=0 行）
     */
    Optional<StorageFile> findByFileMd5(String fileMd5);

    /**
     * 按状态查询（补偿 job 扫描 status=0 待物理清理文件）
     */
    List<StorageFile> findByStatus(Integer status);
}
