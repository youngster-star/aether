package top.heyqing.aether;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;

import top.heyqing.aether.storage.OssStorageServiceImpl;
import top.heyqing.aether.storage.StoredFile;
import top.heyqing.aether.storage.UploadContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OSS 分片结构单测（BackEnd-Plan §8.1；阶段 9 凭据联调前的结构防回归）
 *
 * <p>用 Mockito 打桩 OSS 客户端，验证 multipart 生命周期关键契约：
 * initMultipart 生成 objectKey 并返回 uploadId；uploadChunk 携带正确 partNumber
 * 与 key；merge 以收集的 ETag 列表调用 CompleteMultipartUpload。</p>
 */
class OssUploadStructureTest {

    @Test
    @DisplayName("OSS multipart 生命周期：initMultipart 定 objectKey，uploadPart 传对参数，merge 提交 ETag")
    void ossMultipartLifecycle() {
        OSS oss = mock(OSS.class);
        InitiateMultipartUploadResult initResult = new InitiateMultipartUploadResult();
        initResult.setUploadId("oss-upload-1");
        when(oss.initiateMultipartUpload(any())).thenReturn(initResult);
        UploadPartResult partResult = new UploadPartResult();
        partResult.setETag("etag-abc");
        when(oss.uploadPart(any())).thenReturn(partResult);
        CompleteMultipartUploadResult completeResult = new CompleteMultipartUploadResult();
        completeResult.setKey("whatever.mp4");
        when(oss.completeMultipartUpload(any())).thenReturn(completeResult);

        OssStorageServiceImpl service = new OssStorageServiceImpl(oss, "bucket");
        UploadContext ctx = new UploadContext();
        ctx.setUploadId("upload-1");
        ctx.setExt("mp4");
        ctx.setChunkSize(8L * 1024 * 1024);
        ctx.setChunkTotal(2);
        ctx.setSize(10L * 1024 * 1024);

        // initMultipart：生成并固定 objectKey，返回 OSS uploadId
        String ossUploadId = service.initMultipart(ctx);
        assertEquals("oss-upload-1", ossUploadId);
        assertNotNull(ctx.getObjectKey(), "objectKey 应在会话创建阶段生成并持久化");

        // uploadChunk：partNumber = index+1，携带 key 与 uploadId，ETag 收集到 ctx
        ctx.setOssUploadId(ossUploadId);
        ctx.setChunkIndex(0);
        service.uploadChunk(ctx, new ByteArrayInputStream(new byte[0]));
        assertEquals("etag-abc", ctx.getPartEtags().get(1), "ETag 应按 partNumber 收集");
        ArgumentCaptor<UploadPartRequest> partCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
        verify(oss).uploadPart(partCaptor.capture());
        assertEquals(ctx.getObjectKey(), partCaptor.getValue().getKey());
        assertEquals("oss-upload-1", partCaptor.getValue().getUploadId());
        assertEquals(1, partCaptor.getValue().getPartNumber());

        // merge：以收集的 ETag 列表提交 CompleteMultipartUpload
        StoredFile stored = service.merge(ctx);
        assertEquals(ctx.getObjectKey(), stored.objectKey());
        ArgumentCaptor<CompleteMultipartUploadRequest> mergeCaptor =
                ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(oss).completeMultipartUpload(mergeCaptor.capture());
        assertEquals(1, mergeCaptor.getValue().getPartETags().size());
        assertEquals("etag-abc", mergeCaptor.getValue().getPartETags().get(0).getETag());
    }
}
