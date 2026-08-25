package top.heyqing.aether.model.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 文件元数据表（本地与 OSS 统一记录，BackEnd-Plan §6.2 storage_file）
 *
 * <p>注意：列名 file_md5 按文档保留，实际存 SHA-256 十六进制（64 位），秒传依据。</p>
 */
@Entity
@Table(name = "storage_file")
public class StorageFile extends BaseEntity {

    /** 原始文件名 */
    @Column(name = "original_name", nullable = false)
    private String originalName;

    /** 存储类型：1 本地 2 OSS */
    @Column(name = "storage_type", nullable = false)
    private Integer storageType;

    /** 存储对象键（本地=相对路径，OSS=objectKey） */
    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    /** 文件大小（字节） */
    @Column(name = "size", nullable = false)
    private Long size;

    /** 文件 SHA-256 哈希（秒传依据） */
    @Column(name = "file_md5", nullable = false, unique = true, length = 64)
    private String fileMd5;

    /** MIME 类型 */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /** 扩展名 */
    @Column(name = "ext", length = 20)
    private String ext;

    /** 图片宽度（像素） */
    @Column(name = "width")
    private Integer width;

    /** 图片高度（像素） */
    @Column(name = "height")
    private Integer height;

    /** 音视频时长（秒，阶段 3/4 上传后探测） */
    @Column(name = "duration")
    private Integer duration;

    /** 其他元数据（编码/帧率/色域等，JSON） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta")
    private String meta;

    /** 状态：1 正常 0 已删除（延迟清理） */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public Integer getStorageType() {
        return storageType;
    }

    public void setStorageType(Integer storageType) {
        this.storageType = storageType;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
