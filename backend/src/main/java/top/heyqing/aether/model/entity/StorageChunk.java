package top.heyqing.aether.model.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 分片上传进度表（断点续传依据，BackEnd-Plan §6.2 storage_chunk）
 */
@Entity
@Table(name = "storage_chunk", uniqueConstraints = @UniqueConstraint(name = "uk_upload_chunk",
        columnNames = {"upload_id", "chunk_index"}))
public class StorageChunk extends BaseEntity {

    /** 上传会话 ID（UUID） */
    @Column(name = "upload_id", nullable = false, length = 64)
    private String uploadId;

    /** 文件 SHA-256 */
    @Column(name = "file_md5", nullable = false, length = 64)
    private String fileMd5;

    /** 分片序号（从 0） */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /** 总分片数 */
    @Column(name = "chunk_total", nullable = false)
    private Integer chunkTotal;

    /** 分片大小（字节） */
    @Column(name = "size", nullable = false)
    private Long size;

    /** 存储类型（与上传时选择一致） */
    @Column(name = "storage_type", nullable = false)
    private Integer storageType;

    /** 状态：1 已上传 2 已合并 0 失效 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Integer getChunkTotal() {
        return chunkTotal;
    }

    public void setChunkTotal(Integer chunkTotal) {
        this.chunkTotal = chunkTotal;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Integer getStorageType() {
        return storageType;
    }

    public void setStorageType(Integer storageType) {
        this.storageType = storageType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
