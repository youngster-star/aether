package top.heyqing.aether.model.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 业务-文件引用表（删除一致性核心，BackEnd-Plan §6.2 storage_ref）
 */
@Entity
@Table(name = "storage_ref", uniqueConstraints = @UniqueConstraint(name = "uk_file_biz",
        columnNames = {"file_id", "biz_type", "biz_id"}))
public class StorageRef extends BaseEntity {

    /** 文件 ID */
    @Column(name = "file_id", nullable = false)
    private Long fileId;

    /** 业务域：article/album/video/music/book/announcement */
    @Column(name = "biz_type", nullable = false, length = 20)
    private String bizType;

    /** 业务记录 ID */
    @Column(name = "biz_id", nullable = false)
    private Long bizId;

    /** 引用次数（同一文件多处引用时递增） */
    @Column(name = "ref_count", nullable = false)
    private Integer refCount = 1;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public Integer getRefCount() {
        return refCount;
    }

    public void setRefCount(Integer refCount) {
        this.refCount = refCount;
    }
}
