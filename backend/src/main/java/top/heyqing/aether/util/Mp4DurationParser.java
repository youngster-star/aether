package top.heyqing.aether.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * MP4/M4A 时长解析（BackEnd-Plan §8.2 时长探测的 ffprobe 回退路径）
 *
 * <p>纯 Java 解析 ISO BMFF 原子结构：顶层遍历 box 找到 moov，再进 moov 找 mvhd，
 * 读取 timescale/duration 换算秒数。零依赖，不要求 moov 前置（faststart），
 * 大文件 mdat 按 size 跳过不读入内存。</p>
 */
public final class Mp4DurationParser {

    private Mp4DurationParser() {
    }

    /**
     * 解析视频/音频时长（秒）
     *
     * @param file 本地文件路径
     * @return 时长（秒，向下取整）；文件不合法或找不到 mvhd 返回 null
     */
    public static Integer parseSeconds(Path file) {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long length = raf.length();
            long offset = 0;
            while (offset + 8 <= length) {
                raf.seek(offset);
                Box box = readBoxHeader(raf);
                if (box == null || box.size < box.headerSize || offset + box.size > length) {
                    return null;
                }
                if ("moov".equals(box.type)) {
                    return parseMvhd(raf, offset + box.headerSize, offset + box.size);
                }
                offset += box.size;
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 在 moov 内查找 mvhd 并计算时长
     */
    private static Integer parseMvhd(RandomAccessFile raf, long start, long end) throws IOException {
        long offset = start;
        while (offset + 8 <= end) {
            raf.seek(offset);
            Box box = readBoxHeader(raf);
            if (box == null || box.size < box.headerSize || offset + box.size > end) {
                return null;
            }
            if ("mvhd".equals(box.type)) {
                return durationFromMvhd(raf, offset + box.headerSize);
            }
            offset += box.size;
        }
        return null;
    }

    /**
     * mvhd 布局：box 头之后 version(1)+flags(3)；
     * v0：creation_time(4)+modification_time(4)+timescale(4)+duration(4)
     * v1：creation_time(8)+modification_time(8)+timescale(4)+duration(8)
     */
    private static Integer durationFromMvhd(RandomAccessFile raf, long payloadStart) throws IOException {
        raf.seek(payloadStart);
        int version = raf.readUnsignedByte();
        if (version != 0 && version != 1) {
            return null;
        }
        raf.seek(payloadStart + 4 + (version == 1 ? 16 : 8));
        long timescale = Integer.toUnsignedLong(raf.readInt());
        long duration = version == 1 ? raf.readLong() : Integer.toUnsignedLong(raf.readInt());
        if (timescale <= 0 || duration < 0) {
            return null;
        }
        return (int) Math.round((double) duration / timescale);
    }

    /**
     * 读取 box 头（支持 64 位 large size 与 size=0 到文件尾）
     */
    private static Box readBoxHeader(RandomAccessFile raf) throws IOException {
        long size = Integer.toUnsignedLong(raf.readInt());
        byte[] typeBytes = new byte[4];
        raf.readFully(typeBytes);
        String type = new String(typeBytes, StandardCharsets.US_ASCII);
        long headerSize = 8;
        if (size == 1) {
            size = raf.readLong();
            headerSize = 16;
        }
        return new Box(type, size, headerSize);
    }

    /** box 头信息 */
    private record Box(String type, long size, long headerSize) {
    }
}
