package top.heyqing.aether.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分片上传上下文（BackEnd-Plan §8.1/§8.2）
 *
 * <p>每次请求由 FileUploadServiceImpl 从会话 JSON（CacheStore）恢复构建：
 * objectKey/ossUploadId 等跨请求状态在会话创建（initMultipart）时产生并持久化，
 * 本对象仅作单次请求内的传递载体；partEtags 在单请求内收集（OSS 分片上传后
 * 立即由上层持久化到 CacheStore，merge 时恢复填充）。</p>
 */
public class UploadContext {

    /** 上传会话 ID（UUID） */
    private String uploadId;

    /** 文件 SHA-256（init 时前端计算传入，merge 后整体校验） */
    private String md5;

    /** 文件总大小（字节） */
    private long size;

    /** 原始文件名 */
    private String originalName;

    /** 扩展名（不含点，小写） */
    private String ext;

    /** 存储类型：1 本地 2 OSS */
    private int storageType;

    /** 分片大小（字节） */
    private long chunkSize;

    /** 总分片数 */
    private int chunkTotal;

    /** 当前分片序号（从 0，uploadChunk 调用前设置） */
    private int chunkIndex;

    /** 最终对象键（OSS 在 initMultipart 时生成；本地在 merge 时生成） */
    private String objectKey;

    /** OSS 分片会话 ID（initMultipart 返回，本地实现为 null） */
    private String ossUploadId;

    /** OSS 分片 ETag 集合（partNumber -> eTag，completeMultipartUpload 用；线程安全） */
    private final Map<Integer, String> partEtags = new ConcurrentHashMap<>();

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public int getStorageType() {
        return storageType;
    }

    public void setStorageType(int storageType) {
        this.storageType = storageType;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkTotal() {
        return chunkTotal;
    }

    public void setChunkTotal(int chunkTotal) {
        this.chunkTotal = chunkTotal;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getOssUploadId() {
        return ossUploadId;
    }

    public void setOssUploadId(String ossUploadId) {
        this.ossUploadId = ossUploadId;
    }

    public Map<Integer, String> getPartEtags() {
        return partEtags;
    }
}
