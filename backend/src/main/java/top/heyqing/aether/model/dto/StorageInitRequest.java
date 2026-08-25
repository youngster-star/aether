package top.heyqing.aether.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * 上传初始化请求（BackEnd-Plan §5.2 存储 storage）
 *
 * @param md5          文件 SHA-256（前端 Web Worker 计算，秒传依据）
 * @param size         文件大小（字节）
 * @param originalName 原始文件名
 * @param storageType  存储类型：1 本地 2 OSS
 */
public record StorageInitRequest(
        @NotBlank(message = "文件哈希不能为空")
        @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "文件哈希必须为 64 位十六进制 SHA-256")
        String md5,
        @NotNull(message = "文件大小不能为空") @Positive(message = "文件大小必须大于 0")
        Long size,
        @NotBlank(message = "文件名不能为空")
        String originalName,
        @NotNull(message = "存储类型不能为空")
        Integer storageType) {
}
