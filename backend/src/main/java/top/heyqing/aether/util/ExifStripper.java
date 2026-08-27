package top.heyqing.aether.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.zip.CRC32;

import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;

/**
 * 图片 EXIF 抹除工具（BackEnd-Plan §4.5：上传图片抹除定位信息）
 *
 * <p>策略按格式区分：
 * <ul>
 *   <li>JPEG：commons-imaging {@link ExifRewriter#removeExif} 无损剥离全部 EXIF 段
 *       （像素数据不重编码，画质不变，手机照片 GPS 定位信息随之清除）</li>
 *   <li>PNG：手写 chunk 过滤剥离 eXIf 辅助块（保留 ICC 等色彩块，CRC 重算）</li>
 *   <li>GIF/WebP：无标准定位元数据规范，跳过</li>
 * </ul>
 * 改写失败一律回退原文件内容（不阻断上传流程）。</p>
 */
public final class ExifStripper {

    /** PNG 签名（8 字节） */
    private static final byte[] PNG_SIG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    /** PNG eXIf 块类型（EXIF 元数据载体，见 PNG 规范 1.2+） */
    private static final byte[] EXIF_CHUNK = {'e', 'X', 'I', 'f'};

    private ExifStripper() {
    }

    /**
     * 抹除图片 EXIF：读取 source 写入 target
     *
     * @param source 原文件路径
     * @param target 输出路径（可由调用方删除的临时文件）
     * @param ext    小写扩展名（jpg/jpeg/png/gif/webp）
     * @return 是否发生了改写（false=原样复制或无需处理）
     */
    public static boolean strip(Path source, Path target, String ext) {
        try {
            return switch (ext) {
                case "jpg", "jpeg" -> stripJpeg(source, target);
                case "png" -> stripPng(source, target);
                default -> {
                    copy(source, target);
                    yield false;
                }
            };
        } catch (Exception e) {
            // 改写失败回退原样复制：EXIF 抹除为安全增强，不阻断上传
            try {
                copy(source, target);
            } catch (IOException ignored) {
                // 连复制都失败则原文件仍可用，交由调用方直接读取原文件
            }
            return false;
        }
    }

    /**
     * JPEG：ExifRewriter 无损剥离（无 EXIF 的图输出与原图一致的段流）
     */
    private static boolean stripJpeg(Path source, Path target) throws Exception {
        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(target)) {
            new ExifRewriter().removeExifMetadata(in, out);
        }
        return true;
    }

    /**
     * PNG：逐 chunk 过滤 eXIf（其他块原样透传，CRC 重新计算）
     *
     * @throws IOException 文件结构截断（调用方回退整文件复制）
     */
    private static boolean stripPng(Path source, Path target) throws IOException {
        try (InputStream in = Files.newInputStream(source)) {
            byte[] sig = in.readNBytes(8);
            if (!Arrays.equals(sig, PNG_SIG)) {
                copy(source, target);
                return false;
            }
            boolean stripped = false;
            try (OutputStream out = Files.newOutputStream(target)) {
                out.write(sig);
                while (true) {
                    byte[] lenBytes = in.readNBytes(4);
                    if (lenBytes.length == 0) {
                        break; // 正常结束
                    }
                    byte[] type = in.readNBytes(4);
                    if (lenBytes.length != 4 || type.length != 4) {
                        throw new IOException("PNG 块头截断");
                    }
                    int len = ByteBuffer.wrap(lenBytes).getInt();
                    if (len < 0 || len > 64 * 1024 * 1024) {
                        throw new IOException("PNG 块长度非法: " + len);
                    }
                    byte[] data = in.readNBytes(len);
                    byte[] crc = in.readNBytes(4);
                    if (data.length != len || crc.length != 4) {
                        throw new IOException("PNG 块数据截断");
                    }
                    if (Arrays.equals(type, EXIF_CHUNK)) {
                        stripped = true; // 跳过 eXIf 块
                        continue;
                    }
                    out.write(lenBytes);
                    out.write(type);
                    out.write(data);
                    // CRC 重算（原 CRC 覆盖 len+type+data，透传数据一致时可直接复用原值；
                    // 这里统一重算保证与写入内容严格一致）
                    out.write(crcOf(type, data));
                }
            }
            return stripped;
        }
    }

    private static byte[] crcOf(byte[] type, byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(data);
        return ByteBuffer.allocate(4).putInt((int) crc.getValue()).array();
    }

    private static void copy(Path source, Path target) throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
