package top.heyqing.aether.storage;

/**
 * 分片合并结果（BackEnd-Plan §8.1）
 *
 * @param objectKey 存储对象键（本地=相对路径，OSS=objectKey）
 * @param size      文件大小（字节）
 * @param mimeType  MIME 类型
 * @param ext       扩展名（不含点，小写）
 */
public record StoredFile(String objectKey, long size, String mimeType, String ext) {
}
