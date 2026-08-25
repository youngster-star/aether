package top.heyqing.aether.model.vo;

/**
 * 分片合并返回体（BackEnd-Plan §5.2 存储 storage）
 *
 * @param fileId 文件 ID（业务记录关联用）
 * @param url    签名访问 URL（相对路径，含 expires 与 sign，见 §4.4）
 */
public record StorageMergeVO(Long fileId, String url) {
}
