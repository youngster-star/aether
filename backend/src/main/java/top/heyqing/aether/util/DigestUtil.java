package top.heyqing.aether.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;

/**
 * 摘要与签名工具（SHA-256 / HMAC-SHA256，BackEnd-Plan §4.4/§8.2）
 */
public final class DigestUtil {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private DigestUtil() {
    }

    /**
     * 流式计算 SHA-256（不一次性读入内存，适合大文件）
     */
    public static String sha256Hex(InputStream in) {
        try (in) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // JDK 保证存在 SHA-256，此处不可达
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "文件读取失败");
        }
    }

    /**
     * 计算文件 SHA-256
     */
    public static String sha256Hex(Path file) {
        try {
            return sha256Hex(Files.newInputStream(file));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "文件读取失败");
        }
    }

    /**
     * HMAC-SHA256 签名（签名 URL 防伪造，BackEnd-Plan §4.4）
     *
     * @return 小写十六进制签名
     */
    public static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    /**
     * 恒时比较（防签名时序攻击）
     */
    public static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            out[i * 2] = HEX[(bytes[i] >> 4) & 0xF];
            out[i * 2 + 1] = HEX[bytes[i] & 0xF];
        }
        return new String(out);
    }
}
