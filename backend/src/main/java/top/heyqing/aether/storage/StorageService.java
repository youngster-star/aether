package top.heyqing.aether.storage;

import java.io.InputStream;

/**
 * 存储抽象（BackEnd-Plan §8.1）：本地磁盘与阿里云 OSS 双实现
 *
 * <p>上层 FileUploadService 按 storageType 选择实现，业务代码不感知具体存储后端。</p>
 */
public interface StorageService {

    /**
     * 上传单个分片（分片已按序号校验，实现方按 index 落盘/上传）
     */
    void uploadChunk(UploadContext ctx, InputStream in);

    /**
     * 合并全部分片为最终文件（本地零拷贝拼接 / OSS CompleteMultipartUpload）
     *
     * @return 合并后的文件元信息（objectKey 已生成）
     */
    StoredFile merge(UploadContext ctx);

    /**
     * 物理删除
     */
    void delete(String objectKey);

    /**
     * 打开读取流（本地 FileInputStream 支持 Range；OSS 对象流）
     */
    InputStream open(String objectKey);

    /**
     * 短时效签名 URL（OSS 专用；本地实现不支持，返回 null）
     */
    default String getSignedUrl(String objectKey, long expiresSeconds) {
        return null;
    }

    /**
     * 对象是否存在
     */
    boolean exists(String objectKey);

    /**
     * 初始化分片会话（OSS 返回 uploadId；本地无需，默认返回 null）
     */
    default String initMultipart(UploadContext ctx) {
        return null;
    }

    /**
     * 实现对应的存储类型（1 本地 / 2 OSS，StorageRouter 路由依据）
     */
    default int storageType() {
        return 1;
    }

    /**
     * 覆盖写对象（EXIF 抹除等合并后内容重写场景，BackEnd-Plan §4.5）
     *
     * <p>本地=文件覆盖（REPLACE_EXISTING）；OSS=putObject 覆盖同 key。</p>
     *
     * @param objectKey 对象键（与 merge 返回的一致）
     * @param in        新内容流（调用方负责关闭）
     */
    void overwrite(String objectKey, InputStream in);

    /**
     * 对象键对应的本地文件路径（本地实现返回绝对路径；OSS 等远端实现返回 null）
     *
     * <p>供需要按文件路径工作的工具使用（EXIF 抹除/ffprobe 时长探测）。</p>
     */
    default java.nio.file.Path localPathOf(String objectKey) {
        return null;
    }
}
