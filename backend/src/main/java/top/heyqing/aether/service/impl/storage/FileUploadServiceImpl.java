package top.heyqing.aether.service.impl.storage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.cache.CacheStore;
import top.heyqing.aether.config.StorageProperties;
import top.heyqing.aether.constant.ApiConst;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.StorageInitRequest;
import top.heyqing.aether.model.entity.StorageChunk;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.vo.StorageInitVO;
import top.heyqing.aether.model.vo.StorageMergeVO;
import top.heyqing.aether.repository.StorageChunkRepository;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.security.SecurityConst;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.service.storage.MediaProbeService;
import top.heyqing.aether.storage.FileTypeValidator;
import top.heyqing.aether.storage.StorageRouter;
import top.heyqing.aether.storage.StorageService;
import top.heyqing.aether.storage.StoredFile;
import top.heyqing.aether.storage.UploadContext;
import top.heyqing.aether.util.DigestUtil;
import top.heyqing.aether.util.ExifStripper;
import top.heyqing.aether.util.ImageUtil;

/**
 * 文件上传业务编排实现（BackEnd-Plan §8.2）
 *
 * <p>跨请求状态设计：会话（md5/size/分片信息/OSS objectKey/uploadId）存 CacheStore JSON
 * （TTL 24h），OSS 每片 ETag 单独缓存 key；分片进度落 storage_chunk 表。
 * 断点续传：init 按 md5 反查未完成会话复用 uploadId，返回已传分片索引。</p>
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);

    /** merge 幂等锁 TTL（大文件 SHA-256 校验耗时，锁需覆盖整个合并周期） */
    private static final Duration MERGE_LOCK_TTL = Duration.ofMinutes(30);

    /** md5 → 进行中 uploadId 映射 key 前缀（断点续传会话复用） */
    private static final String MD5_SESSION_KEY = "upload:md5:";

    /** OSS 分片 ETag key 前缀（+uploadId:index，completeMultipartUpload 用） */
    private static final String ETAG_KEY = "upload:etag:";

    private final CacheStore cacheStore;
    private final StorageRouter storageRouter;
    private final StorageProperties storageProperties;
    private final StorageFileRepository storageFileRepository;
    private final StorageChunkRepository storageChunkRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final MediaProbeService mediaProbeService;

    public FileUploadServiceImpl(CacheStore cacheStore, StorageRouter storageRouter,
                                 StorageProperties storageProperties, StorageFileRepository storageFileRepository,
                                 StorageChunkRepository storageChunkRepository, ObjectMapper objectMapper,
                                 TransactionTemplate transactionTemplate, MediaProbeService mediaProbeService) {
        this.cacheStore = cacheStore;
        this.storageRouter = storageRouter;
        this.storageProperties = storageProperties;
        this.storageFileRepository = storageFileRepository;
        this.storageChunkRepository = storageChunkRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.mediaProbeService = mediaProbeService;
    }

    @Override
    public StorageInitVO init(StorageInitRequest request) {
        // 1. 扩展名白名单 + 大小上限校验（BackEnd-Plan §8.4）
        String ext = FileTypeValidator.validate(request.originalName(), request.size());
        String md5 = request.md5().toLowerCase(Locale.ROOT);
        int storageType = request.storageType();
        if (storageType != 1 && storageType != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存储类型只能为 1（本地）或 2（OSS）");
        }
        // 2. 秒传：同 SHA-256 已完成文件直接复用（BackEnd-Plan §8.2）
        return storageFileRepository.findByFileMd5AndStatus(md5, 1)
                .map(file -> {
                    log.info("秒传命中: md5={}, fileId={}", md5, file.getId());
                    return StorageInitVO.instantUpload(file.getId());
                })
                .orElseGet(() -> initOrResumeSession(request, ext, md5, storageType));
    }

    @Override
    @Transactional
    public void chunk(String uploadId, int index, MultipartFile file) {
        UploadSession session = loadSession(uploadId);
        if (index < 0 || index >= session.chunkTotal()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分片序号越界");
        }
        // 非最后一片必须完整 8MB（防中间缺数据），最后一片不超过剩余大小
        long expected = Math.min(storageProperties.chunkSizeBytes(), session.size() - (long) index * storageProperties.chunkSizeBytes());
        long actual = file.getSize();
        if (actual != expected) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    String.format("分片大小不符：期望 %d 字节，实际 %d 字节", expected, actual));
        }
        UploadContext ctx = buildContext(session, uploadId);
        ctx.setChunkIndex(index);
        StorageService storage = storageRouter.select(session.storageType());
        try (InputStream in = file.getInputStream()) {
            storage.uploadChunk(ctx, in);
        } catch (Exception e) {
            if (e instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "分片上传失败");
        }
        // OSS 分片 ETag 持久化（completeMultipartUpload 在 merge 时读取）
        if (session.storageType() == 2) {
            String etag = ctx.getPartEtags().get(index + 1);
            if (etag != null) {
                cacheStore.set(ETAG_KEY + uploadId + ":" + index, etag, SecurityConst.UPLOAD_SESSION_TTL);
            }
        }
        // 分片进度落库（断点续传依据；重复分片覆盖更新，幂等）
        StorageChunk chunk = storageChunkRepository.findByUploadIdAndChunkIndex(uploadId, index)
                .orElseGet(StorageChunk::new);
        chunk.setUploadId(uploadId);
        chunk.setFileMd5(session.md5());
        chunk.setChunkIndex(index);
        chunk.setChunkTotal(session.chunkTotal());
        chunk.setSize(actual);
        chunk.setStorageType(session.storageType());
        chunk.setStatus(1);
        storageChunkRepository.save(chunk);
    }

    @Override
    public StorageMergeVO merge(String uploadId) {
        UploadSession session = loadSession(uploadId);
        // 幂等锁：防止并发重复合并（同一会话只合并一次）
        if (!cacheStore.setIfAbsent("merge:lock:" + uploadId, "1", MERGE_LOCK_TTL)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "合并进行中，请勿重复提交");
        }
        try {
            // 分片完整性检查
            long uploaded = storageChunkRepository.countByUploadIdAndStatus(uploadId, 1);
            if (uploaded < session.chunkTotal()) {
                throw new BusinessException(ErrorCode.CHUNK_MISSING);
            }
            StorageService storage = storageRouter.select(session.storageType());
            UploadContext ctx = buildContext(session, uploadId);
            if (session.storageType() == 2) {
                // OSS：ETag 从缓存恢复后 CompleteMultipartUpload（分片上传阶段已持久化）
                for (int index = 0; index < session.chunkTotal(); index++) {
                    String etag = cacheStore.get(ETAG_KEY + uploadId + ":" + index);
                    if (etag == null) {
                        throw new BusinessException(ErrorCode.CHUNK_MISSING);
                    }
                    ctx.getPartEtags().put(index + 1, etag);
                }
            }
            StoredFile stored = storage.merge(ctx);
            // 合并后整体校验：SHA-256 与 init 传入值一致（BackEnd-Plan §8.2）
            String actualMd5 = DigestUtil.sha256Hex(storage.open(stored.objectKey()));
            if (!DigestUtil.constantTimeEquals(session.md5(), actualMd5)) {
                storage.delete(stored.objectKey());
                throw new BusinessException(ErrorCode.FILE_VERIFY_FAILED);
            }
            // 魔数校验（防伪造类型）；失败同样清理物理文件，避免孤儿文件
            try {
                FileTypeValidator.verifyMagic(session.ext(), storage.open(stored.objectKey()));
            } catch (BusinessException e) {
                storage.delete(stored.objectKey());
                throw e;
            }
            // 图片 EXIF 抹除（BackEnd-Plan §4.5）：GPS 定位信息不落存储
            if (FileTypeValidator.isImage(session.ext())) {
                stripExif(storage, stored.objectKey(), session.ext());
            }
            // 元数据探测：图片宽高 / 音视频时长（ffprobe 优先，MP4 内置解析回退，§8.2）
            Integer width = null;
            Integer height = null;
            Integer duration = null;
            if (FileTypeValidator.isImage(session.ext())) {
                int[] size = ImageUtil.probeSize(storage.open(stored.objectKey()));
                if (size != null) {
                    width = size[0];
                    height = size[1];
                }
            } else if (FileTypeValidator.isVideoOrAudio(session.ext())) {
                duration = mediaProbeService.probeDurationSeconds(session.ext(), storage, stored.objectKey());
            }
            // storage_file 落库（对象键 UUID 命名，原始文件名仅存元数据）
            StorageFile storageFile = new StorageFile();
            storageFile.setOriginalName(session.originalName());
            storageFile.setStorageType(session.storageType());
            storageFile.setObjectKey(stored.objectKey());
            storageFile.setSize(stored.size());
            storageFile.setFileMd5(session.md5());
            storageFile.setMimeType(FileTypeValidator.mimeTypeOf(session.ext()));
            storageFile.setExt(session.ext());
            storageFile.setWidth(width);
            storageFile.setHeight(height);
            storageFile.setDuration(duration);
            storageFile.setStatus(1);
            // 短事务仅覆盖元数据落库（文件拼接/校验等耗时操作在事务外，避免长事务占连接）
            List<StorageChunk> chunks = storageChunkRepository.findByUploadIdOrderByChunkIndexAsc(uploadId);
            StorageFile saved = transactionTemplate.execute(status -> {
                StorageFile savedFile = persistStorageFile(storage, stored.objectKey(), session.md5(), storageFile);
                chunks.forEach(c -> {
                    c.setStatus(2);
                    storageChunkRepository.save(c);
                });
                return savedFile;
            });
            // 事务提交后清理缓存会话（清理失败由 24h TTL 兜底自愈）
            cacheStore.delete(SecurityConst.UPLOAD_SESSION_KEY + uploadId);
            cacheStore.delete(MD5_SESSION_KEY + session.md5());
            for (int index = 0; index < session.chunkTotal(); index++) {
                cacheStore.delete(ETAG_KEY + uploadId + ":" + index);
            }
            log.info("上传合并完成: uploadId={}, fileId={}, md5={}", uploadId, saved.getId(), session.md5());
            return new StorageMergeVO(saved.getId(), signUrl(saved.getId()));
        } finally {
            cacheStore.delete("merge:lock:" + uploadId);
        }
    }

    @Override
    public StorageFile findPublishedFile(Long fileId) {
        return storageFileRepository.findById(fileId)
                .filter(file -> file.getStatus() == 1)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public InputStream openFile(StorageFile file) {
        return storageRouter.select(file.getStorageType()).open(file.getObjectKey());
    }

    /**
     * 图片 EXIF 抹除（BackEnd-Plan §4.5，merge 落库前执行）
     *
     * <p>本地文件直接按路径改写；OSS 对象先溢出临时文件再 overwrite 回写。
     * 抹除失败仅 warn 日志（安全增强项，不阻断上传），原文件保留。</p>
     */
    private void stripExif(StorageService storage, String objectKey, String ext) {
        Path source = storage.localPathOf(objectKey);
        Path spill = null;
        try {
            if (source == null) {
                // 远端存储：对象溢出临时文件（图片上限 50MB，§8.4）
                spill = Files.createTempFile("aether-exif-", "." + ext);
                try (InputStream in = storage.open(objectKey)) {
                    Files.copy(in, spill, StandardCopyOption.REPLACE_EXISTING);
                }
                source = spill;
            }
            Path stripped = Files.createTempFile("aether-exif-stripped-", "." + ext);
            try {
                if (ExifStripper.strip(source, stripped, ext)) {
                    try (InputStream in = Files.newInputStream(stripped)) {
                        storage.overwrite(objectKey, in);
                    }
                    log.info("图片 EXIF 已抹除: objectKey={}", objectKey);
                }
            } finally {
                Files.deleteIfExists(stripped);
            }
        } catch (Exception e) {
            log.warn("EXIF 抹除失败，保留原始文件: objectKey={}, 原因={}", objectKey, e.getMessage());
        } finally {
            if (spill != null) {
                try {
                    Files.deleteIfExists(spill);
                } catch (Exception ignored) {
                    // 临时文件残留由系统临时目录自清理
                }
            }
        }
    }

    /**
     * storage_file 落库（uk_md5 唯一约束处理，BackEnd-Plan §8.2/§8.3）
     *
     * <ul>
     *   <li>同 md5 的 status=0 行（删除后同内容重传）：复活——旧物理文件即时清理，
     *       行更新指向新 objectKey（EXIF 抹除后内容已确定）</li>
     *   <li>同 md5 的 status=1 行（并发同内容上传竞态）：丢弃本次合并物理文件复用现有行</li>
     *   <li>无同 md5 行：正常插入</li>
     * </ul>
     */
    private StorageFile persistStorageFile(StorageService storage, String newObjectKey, String md5,
                                           StorageFile storageFile) {
        StorageFile existing = storageFileRepository.findByFileMd5(md5).orElse(null);
        if (existing == null) {
            return storageFileRepository.save(storageFile);
        }
        if (existing.getStatus() == 0) {
            // 复活：旧 objectKey 物理文件可能仍存在（补偿 job 未执行），即时清理后指向新文件
            if (existing.getObjectKey() != null && !existing.getObjectKey().equals(newObjectKey)) {
                try {
                    storage.delete(existing.getObjectKey());
                } catch (Exception e) {
                    log.warn("复活清理旧物理文件失败（补偿 job 兜底）: objectKey={}, 原因={}",
                            existing.getObjectKey(), e.getMessage());
                }
            }
            existing.setOriginalName(storageFile.getOriginalName());
            existing.setObjectKey(newObjectKey);
            existing.setSize(storageFile.getSize());
            existing.setMimeType(storageFile.getMimeType());
            existing.setExt(storageFile.getExt());
            existing.setWidth(storageFile.getWidth());
            existing.setHeight(storageFile.getHeight());
            existing.setDuration(storageFile.getDuration());
            existing.setStatus(1);
            log.info("同内容文件复活: fileId={}, md5={}", existing.getId(), md5);
            return storageFileRepository.save(existing);
        }
        // status=1 并发竞态：内容相同（md5 一致），丢弃本次合并产物复用现有行
        try {
            storage.delete(newObjectKey);
        } catch (Exception e) {
            log.warn("竞态清理重复物理文件失败: objectKey={}, 原因={}", newObjectKey, e.getMessage());
        }
        return existing;
    }

    /**
     * 创建新会话或复用断点会话（BackEnd-Plan §8.2 断点续传）
     */
    private StorageInitVO initOrResumeSession(StorageInitRequest request, String ext, String md5, int storageType) {
        // 断点续传：同 md5 存在未完成会话则复用（分片进度存 storage_chunk，会话存 CacheStore）
        String existingUploadId = cacheStore.get(MD5_SESSION_KEY + md5);
        if (existingUploadId != null) {
            UploadSession existing = tryLoadSession(existingUploadId);
            if (existing != null && existing.storageType() == storageType) {
                List<Integer> uploadedChunks = storageChunkRepository.findByUploadIdOrderByChunkIndexAsc(existingUploadId).stream()
                        .map(StorageChunk::getChunkIndex)
                        .toList();
                log.info("断点续传命中: uploadId={}, 已传分片={}", existingUploadId, uploadedChunks);
                return new StorageInitVO(existingUploadId, storageProperties.chunkSizeBytes(),
                        existing.chunkTotal(), uploadedChunks, null);
            }
        }
        // 新建会话；OSS 通道在会话创建时初始化分片会话（objectKey/uploadId 随会话持久化）
        int chunkTotal = (int) Math.ceilDiv(request.size(), storageProperties.chunkSizeBytes());
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        UploadContext ctx = new UploadContext();
        ctx.setUploadId(uploadId);
        ctx.setMd5(md5);
        ctx.setSize(request.size());
        ctx.setOriginalName(request.originalName());
        ctx.setExt(ext);
        ctx.setStorageType(storageType);
        ctx.setChunkSize(storageProperties.chunkSizeBytes());
        ctx.setChunkTotal(chunkTotal);
        String ossUploadId = storageRouter.select(storageType).initMultipart(ctx);
        UploadSession session = new UploadSession(md5, request.size(), request.originalName(), ext,
                storageType, chunkTotal, ctx.getObjectKey(), ossUploadId);
        cacheStore.set(SecurityConst.UPLOAD_SESSION_KEY + uploadId, toJson(session), SecurityConst.UPLOAD_SESSION_TTL);
        cacheStore.set(MD5_SESSION_KEY + md5, uploadId, SecurityConst.UPLOAD_SESSION_TTL);
        log.info("创建上传会话: uploadId={}, 总分片={}, size={}, storageType={}", uploadId, chunkTotal, request.size(), storageType);
        return new StorageInitVO(uploadId, storageProperties.chunkSizeBytes(), chunkTotal, List.of(), null);
    }

    /**
     * 读取会话 JSON；不存在/过期返回 null（断点续传场景容忍）
     */
    private UploadSession tryLoadSession(String uploadId) {
        String json = cacheStore.get(SecurityConst.UPLOAD_SESSION_KEY + uploadId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, UploadSession.class);
        } catch (JacksonException e) {
            return null;
        }
    }

    private UploadSession loadSession(String uploadId) {
        if (uploadId == null || uploadId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传会话 ID 不能为空");
        }
        UploadSession session = tryLoadSession(uploadId);
        if (session == null) {
            throw new BusinessException(ErrorCode.UPLOAD_SESSION_NOT_FOUND);
        }
        return session;
    }

    /**
     * 由会话恢复构建分片上下文（objectKey/ossUploadId 等跨请求状态从会话 JSON 恢复）
     */
    private UploadContext buildContext(UploadSession session, String uploadId) {
        UploadContext ctx = new UploadContext();
        ctx.setUploadId(uploadId);
        ctx.setMd5(session.md5());
        ctx.setSize(session.size());
        ctx.setOriginalName(session.originalName());
        ctx.setExt(session.ext());
        ctx.setStorageType(session.storageType());
        ctx.setChunkSize(storageProperties.chunkSizeBytes());
        ctx.setChunkTotal(session.chunkTotal());
        ctx.setObjectKey(session.objectKey());
        ctx.setOssUploadId(session.ossUploadId());
        return ctx;
    }

    /**
     * 构建签名访问 URL（HMAC-SHA256(fileId:expires)，默认 10 分钟有效，BackEnd-Plan §4.4）
     */
    @Override
    public String signUrl(Long fileId) {
        long expires = System.currentTimeMillis() / 1000 + storageProperties.getSignExpireSeconds();
        String sign = DigestUtil.hmacSha256(fileId + ":" + expires, storageProperties.getSignSecret());
        return ApiConst.CONTEXT_PATH + ApiConst.API_V1 + "/storage/file/" + fileId
                + "?expires=" + expires + "&sign=" + sign;
    }

    private String toJson(UploadSession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传会话序列化失败");
        }
    }

    /**
     * 上传会话（CacheStore JSON 负载；objectKey/ossUploadId 为 OSS 通道跨请求状态，本地通道为 null）
     */
    private record UploadSession(String md5, long size, String originalName, String ext,
                                 int storageType, int chunkTotal, String objectKey, String ossUploadId) {
    }
}
