package top.heyqing.aether.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.config.StorageProperties;
import top.heyqing.aether.exception.BusinessException;

/**
 * 本地磁盘存储实现（BackEnd-Plan §8.1）
 *
 * <p>分片临时目录：{base}/chunks/{uploadId}/{index}.part；
 * 合并用 NIO FileChannel.transferTo 零拷贝拼接（保持原始字节序），
 * 合并后原子 rename 至 {base}/files/{uuid}.{ext} 并清理分片目录。</p>
 */
public class LocalStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageServiceImpl.class);

    private final Path baseDir;

    public LocalStorageServiceImpl(StorageProperties properties) {
        this.baseDir = Path.of(properties.getLocalBaseDir()).toAbsolutePath().normalize();
    }

    @Override
    public void uploadChunk(UploadContext ctx, InputStream in) {
        Path chunkPath = chunkPath(ctx.getUploadId(), ctx.getChunkIndex());
        try {
            Files.createDirectories(chunkPath.getParent());
            // COPY_REPLACE_EXISTING：断点续传重传场景幂等覆盖
            Files.copy(in, chunkPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "分片写入失败");
        }
    }

    @Override
    public StoredFile merge(UploadContext ctx) {
        Path chunkDir = chunkDir(ctx.getUploadId());
        Path filesDir = baseDir.resolve("files");
        // 存储名一律 UUID（防路径穿越，原始文件名仅存元数据）
        String objectKey = "files/" + UUID.randomUUID().toString().replace("-", "") + "." + ctx.getExt();
        Path target = baseDir.resolve(objectKey);
        try {
            Files.createDirectories(filesDir);
            // 先写 .tmp 再原子 rename：避免并发读取到半成品
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            try (FileChannel out = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                for (int index = 0; index < ctx.getChunkTotal(); index++) {
                    Path part = chunkDir.resolve(index + ".part");
                    try (FileChannel in = FileChannel.open(part, StandardOpenOption.READ)) {
                        // 零拷贝拼接：内核态直接传输，保持原始字节序
                        long remaining = in.size();
                        while (remaining > 0) {
                            long transferred = in.transferTo(in.size() - remaining, remaining, out);
                            if (transferred <= 0) {
                                throw new IOException("分片零拷贝传输中断: index=" + index);
                            }
                            remaining -= transferred;
                        }
                    }
                }
            }
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            // 清理分片临时目录（清理失败不阻断主流程）
            try (var paths = Files.walk(chunkDir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // 由后续过期清理 job 兜底
                    }
                });
            }
            log.info("本地分片合并完成: uploadId={}, objectKey={}, size={}", ctx.getUploadId(), objectKey, ctx.getSize());
            // mimeType 由上层按扩展名推断（此处传 null）
            return new StoredFile(objectKey, ctx.getSize(), null, ctx.getExt());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "分片合并失败");
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(baseDir.resolve(objectKey));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "文件删除失败");
        }
    }

    @Override
    public InputStream open(String objectKey) {
        try {
            return Files.newInputStream(baseDir.resolve(objectKey));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "文件读取失败");
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.exists(baseDir.resolve(objectKey));
    }

    @Override
    public void overwrite(String objectKey, InputStream in) {
        try (in) {
            Files.copy(in, baseDir.resolve(objectKey), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "文件覆盖写失败");
        }
    }

    @Override
    public Path localPathOf(String objectKey) {
        return baseDir.resolve(objectKey);
    }

    private Path chunkDir(String uploadId) {
        return baseDir.resolve("chunks").resolve(uploadId);
    }

    private Path chunkPath(String uploadId, int index) {
        return chunkDir(uploadId).resolve(index + ".part");
    }
}
