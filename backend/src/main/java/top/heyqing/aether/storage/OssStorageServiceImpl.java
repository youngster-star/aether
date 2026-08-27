package top.heyqing.aether.storage;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;

/**
 * 阿里云 OSS 存储实现（BackEnd-Plan §8.1）
 *
 * <p>私有 Bucket；分片上传走 OSS 原生 MultipartUpload（UploadPart + CompleteMultipartUpload，
 * ETag 校验）；媒体访问经签名 URL（见 §4.4）。凭据由环境变量注入
 * （OSS_ENDPOINT/OSS_ACCESS_KEY/OSS_SECRET/OSS_BUCKET），未配置时懒加载抛业务异常。
 * 真实凭据联调留待阶段 9 部署环境（BackEnd-Plan 附录 B）。</p>
 */
public class OssStorageServiceImpl implements StorageService {

    private final OSS client;
    private final String bucket;

    /**
     * @param client OSS 客户端（由 StorageServiceConfiguration 按环境变量构建；单测可注入 mock）
     * @param bucket 私有 Bucket 名
     */
    public OssStorageServiceImpl(OSS client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public String initMultipart(UploadContext ctx) {
        // 对象键在分片上传开始前确定（后续分片均指向同一 key）
        String key = UUID.randomUUID().toString().replace("-", "") + "." + ctx.getExt();
        ctx.setObjectKey(key);
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, key);
        InitiateMultipartUploadResult result = client.initiateMultipartUpload(request);
        return result.getUploadId();
    }

    @Override
    public void uploadChunk(UploadContext ctx, InputStream in) {
        // partNumber 从 1 开始；ETag 收集到 ctx，merge 时 CompleteMultipartUpload 校验
        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(bucket);
        request.setKey(ctx.getObjectKey());
        request.setUploadId(ctx.getOssUploadId());
        request.setPartNumber(ctx.getChunkIndex() + 1);
        request.setInputStream(in);
        // 除最后一片外大小固定为分片大小（合并前完整性由上层分片记录保证）
        request.setPartSize(ctx.getChunkSize());
        UploadPartResult result = client.uploadPart(request);
        ctx.getPartEtags().put(ctx.getChunkIndex() + 1, result.getETag());
    }

    @Override
    public StoredFile merge(UploadContext ctx) {
        List<PartETag> partEtags = ctx.getPartEtags().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PartETag(entry.getKey(), entry.getValue()))
                .toList();
        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
                bucket, ctx.getObjectKey(), ctx.getOssUploadId(), partEtags);
        client.completeMultipartUpload(request);
        return new StoredFile(ctx.getObjectKey(), ctx.getSize(), null, ctx.getExt());
    }

    @Override
    public void delete(String objectKey) {
        client.deleteObject(bucket, objectKey);
    }

    @Override
    public InputStream open(String objectKey) {
        return client.getObject(bucket, objectKey).getObjectContent();
    }

    @Override
    public String getSignedUrl(String objectKey, long expiresSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expiresSeconds * 1000);
        return client.generatePresignedUrl(bucket, objectKey, expiration).toString();
    }

    @Override
    public boolean exists(String objectKey) {
        return client.doesObjectExist(bucket, objectKey);
    }

    @Override
    public void overwrite(String objectKey, InputStream in) {
        try (in) {
            client.putObject(bucket, objectKey, in);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "OSS 对象覆盖写失败");
        }
    }

    @Override
    public int storageType() {
        return 2;
    }

    /**
     * 校验 OSS 配置是否可用（凭据为空时明确报错，避免误走 OSS 通道）
     */
    public static void checkAvailable(String endpoint, String accessKeyId, String accessKeySecret, String bucket) {
        if (endpoint == null || endpoint.isBlank() || accessKeyId == null || accessKeyId.isBlank()
                || accessKeySecret == null || accessKeySecret.isBlank() || bucket == null || bucket.isBlank()) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "OSS 未配置（OSS_ENDPOINT/OSS_ACCESS_KEY/OSS_SECRET/OSS_BUCKET）");
        }
    }
}
