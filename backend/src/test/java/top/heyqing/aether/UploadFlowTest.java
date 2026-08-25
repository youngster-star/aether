package top.heyqing.aether;

import java.io.ByteArrayInputStream;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.StorageInitRequest;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.StorageInitVO;
import top.heyqing.aether.model.vo.StorageMergeVO;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.util.DigestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分片上传 - 合并 - 秒传全流程测试（BackEnd-Plan §8.2，阶段 1 完成标准）
 *
 * <p>覆盖：断点续传信息返回、分片缺失拒绝合并（30501）、整体 SHA-256 校验失败（30503）、
 * 秒传命中、合并产物可读且内容一致。存储目录重定向到 target/ 下，不污染工作区。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = "aether.storage.local-base-dir=target/test-storage")
class UploadFlowTest {

    /** 测试内容 20MB = 3 片（8MB + 8MB + 4MB） */
    private static final int CONTENT_SIZE = 20 * 1024 * 1024;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private StorageFileRepository storageFileRepository;

    @Test
    @DisplayName("分片上传-合并-秒传全流程：init 返回断点信息，merge 校验落库，重复 init 秒传命中")
    void fullUploadFlow() {
        // 构造 20MB 内容（PNG 魔数头 + 随机数据，通过魔数校验）并计算 SHA-256
        byte[] content = pngLikeContent(CONTENT_SIZE);
        String md5 = DigestUtil.sha256Hex(new ByteArrayInputStream(content));

        // 1. init：返回上传会话与断点信息
        StorageInitVO init = fileUploadService.init(
                new StorageInitRequest(md5, (long) content.length, "测试图片.png", 1));
        assertNotNull(init.uploadId(), "首次上传应创建会话");
        assertEquals(3, init.chunkTotal(), "20MB 按 8MB 分片应为 3 片");
        assertTrue(init.uploadedChunks().isEmpty(), "新会话应无已传分片");

        // 2. 逐片上传
        int chunkSize = (int) (8 * 1024 * 1024);
        for (int i = 0; i < init.chunkTotal(); i++) {
            int from = i * chunkSize;
            int to = Math.min(from + chunkSize, content.length);
            byte[] part = java.util.Arrays.copyOfRange(content, from, to);
            MultipartFile file = new MockMultipartFile("file", "chunk-" + i, "application/octet-stream", part);
            fileUploadService.chunk(init.uploadId(), i, file);
        }

        // 3. merge：落库 + 签名 URL
        StorageMergeVO merge = fileUploadService.merge(init.uploadId());
        assertNotNull(merge.fileId());
        assertTrue(merge.url().contains("expires=") && merge.url().contains("sign="), "签名 URL 应含 expires 与 sign");

        StorageFile storageFile = storageFileRepository.findById(merge.fileId()).orElseThrow();
        assertEquals(md5, storageFile.getFileMd5());
        assertEquals(content.length, storageFile.getSize());
        assertEquals("png", storageFile.getExt());

        // 4. 合并产物内容可读且 SHA-256 一致（StorageResource 签名访问的数据源）
        String readMd5 = DigestUtil.sha256Hex(fileUploadService.openFile(storageFile));
        assertEquals(md5, readMd5, "合并产物内容应与上传内容一致");

        // 5. 秒传：同 md5 再次 init 直接返回 fileId，无需重新上传
        StorageInitVO second = fileUploadService.init(
                new StorageInitRequest(md5, (long) content.length, "另一个名字.png", 1));
        assertEquals(merge.fileId(), second.fileId(), "同 SHA-256 应秒传命中同一文件");
        assertTrue(second.uploadId() == null, "秒传命中不应创建上传会话");
    }

    @Test
    @DisplayName("分片缺失时合并被拒（30501）")
    void mergeRejectedWhenChunksMissing() {
        byte[] content = randomBytes(CONTENT_SIZE);
        String md5 = DigestUtil.sha256Hex(new ByteArrayInputStream(content));
        StorageInitVO init = fileUploadService.init(
                new StorageInitRequest(md5, (long) content.length, "视频.mp4", 1));
        // 只上传第 0 片就合并
        MultipartFile file = new MockMultipartFile("file", "chunk-0", "application/octet-stream",
                java.util.Arrays.copyOfRange(content, 0, 8 * 1024 * 1024));
        fileUploadService.chunk(init.uploadId(), 0, file);

        BusinessException e = assertThrows(BusinessException.class, () -> fileUploadService.merge(init.uploadId()));
        assertEquals(ErrorCode.CHUNK_MISSING.getCode(), e.getErrorCode().getCode());
    }

    @Test
    @DisplayName("整体 SHA-256 与 init 不一致时合并被拒并清理物理文件（30503）")
    void mergeRejectedWhenMd5Mismatch() {
        byte[] content = randomBytes(CONTENT_SIZE);
        // 伪造 md5：与真实内容不符
        String fakeMd5 = "a".repeat(64);
        StorageInitVO init = fileUploadService.init(
                new StorageInitRequest(fakeMd5, (long) content.length, "视频.mp4", 1));
        int chunkSize = 8 * 1024 * 1024;
        for (int i = 0; i < init.chunkTotal(); i++) {
            int from = i * chunkSize;
            int to = Math.min(from + chunkSize, content.length);
            MultipartFile file = new MockMultipartFile("file", "chunk-" + i, "application/octet-stream",
                    java.util.Arrays.copyOfRange(content, from, to));
            fileUploadService.chunk(init.uploadId(), i, file);
        }
        BusinessException e = assertThrows(BusinessException.class, () -> fileUploadService.merge(init.uploadId()));
        assertEquals(ErrorCode.FILE_VERIFY_FAILED.getCode(), e.getErrorCode().getCode());
        // 校验失败不应落库
        assertTrue(storageFileRepository.findByFileMd5AndStatus(fakeMd5, 1).isEmpty());
    }

    @Test
    @DisplayName("非法扩展名与超限大小被拒（10005/10006）")
    void typeAndSizeRejected() {
        byte[] content = randomBytes(1024);
        String md5 = DigestUtil.sha256Hex(new ByteArrayInputStream(content));
        // 白名单外扩展名
        BusinessException e1 = assertThrows(BusinessException.class, () -> fileUploadService.init(
                new StorageInitRequest(md5, 1024L, "病毒.exe", 1)));
        assertEquals(ErrorCode.FILE_TYPE_NOT_SUPPORTED.getCode(), e1.getErrorCode().getCode());
        // 图片超 50MB
        BusinessException e2 = assertThrows(BusinessException.class, () -> fileUploadService.init(
                new StorageInitRequest(md5, 51L * 1024 * 1024, "大图.png", 1)));
        assertEquals(ErrorCode.FILE_SIZE_EXCEEDED.getCode(), e2.getErrorCode().getCode());
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new Random(42).nextBytes(bytes);
        return bytes;
    }

    /**
     * 生成带 PNG 魔数头的内容（89 50 4E 47 0D 0A 1A 0A），其余为随机数据，
     * 用于通过魔数校验；ImageIO 无法解析随机尾部，宽高探测返回 null 属预期。
     */
    private static byte[] pngLikeContent(int size) {
        byte[] bytes = new byte[size];
        // 先随机填充，再覆写头部为 PNG 魔数（保证魔数校验通过）
        new Random(42).nextBytes(bytes);
        bytes[0] = (byte) 0x89;
        bytes[1] = 0x50;
        bytes[2] = 0x4E;
        bytes[3] = 0x47;
        bytes[4] = 0x0D;
        bytes[5] = 0x0A;
        bytes[6] = 0x1A;
        bytes[7] = 0x0A;
        return bytes;
    }
}
