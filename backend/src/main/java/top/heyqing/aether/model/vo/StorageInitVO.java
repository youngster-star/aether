package top.heyqing.aether.model.vo;

import java.util.List;

/**
 * 上传初始化返回体（BackEnd-Plan §5.2 存储 storage）
 *
 * @param uploadId       上传会话 ID（分片上传与合并时回传）
 * @param chunkSize      分片大小（字节）
 * @param chunkTotal     总分片数
 * @param uploadedChunks 已上传分片序号列表（断点续传跳过用）
 * @param fileId         秒传命中时的文件 ID（非秒传为 null）
 */
public record StorageInitVO(String uploadId, long chunkSize, int chunkTotal,
                            List<Integer> uploadedChunks, Long fileId) {

    /**
     * 秒传命中（文件已存在，无需上传）
     */
    public static StorageInitVO instantUpload(Long fileId) {
        return new StorageInitVO(null, 0, 0, List.of(), fileId);
    }
}
