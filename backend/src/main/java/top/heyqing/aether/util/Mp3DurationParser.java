package top.heyqing.aether.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * MP3 时长解析（BackEnd-Plan §8.2 时长探测的 ffprobe 回退路径）
 *
 * <p>纯 Java 解析 MPEG 音频帧结构：跳过 ID3v2 标签 → 定位首个帧同步字 →
 * 优先读 Xing/Info/VBRI 头里的总帧数（VBR 精确值），缺失时按 CBR 码率估算。
 * 零依赖；解析失败返回 null（duration=0 落库，不阻断上传）。</p>
 */
public final class Mp3DurationParser {

    /** MPEG1 Layer I/II/III 每帧采样数 */
    private static final int[] SAMPLES_PER_FRAME_MPEG1 = {384, 1152, 1152};

    /** MPEG2/2.5 Layer I/II/III 每帧采样数（Layer III 减半为 576） */
    private static final int[] SAMPLES_PER_FRAME_MPEG2 = {384, 1152, 576};

    /** 码率表（kbps）：行=MPEG1 Layer I/II/III 与 MPEG2 Layer I/II/III，列=码率索引 1-14 */
    private static final int[][] BITRATE_KBPS = {
            {32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448},
            {32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384},
            {32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320},
            {32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448},
            {8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160},
            {8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160}};

    /** 采样率表（Hz）：行=MPEG1/MPEG2/MPEG2.5，列=索引 0-2（3=保留） */
    private static final int[][] SAMPLE_RATES = {{44100, 48000, 32000}, {22050, 24000, 16000}, {11025, 12000, 8000}};

    private Mp3DurationParser() {
    }

    /**
     * 解析 MP3 时长（秒）
     *
     * @param file 本地文件路径
     * @return 时长（秒，四舍五入）；文件不合法或无有效帧返回 null
     */
    public static Integer parseSeconds(Path file) {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long length = raf.length();
            // ID3v2 标签跳过（"ID3" + 4 字节 synchsafe 大小）
            long offset = skipId3v2(raf, length);
            // 定位首个有效帧同步字（最多扫 64KB，容忍标签与帧间的垃圾填充）
            FrameHeader header = null;
            long scanEnd = Math.min(offset + 64 * 1024, length - 4);
            for (long pos = offset; pos < scanEnd; pos++) {
                raf.seek(pos);
                int b0 = raf.read();
                if (b0 != 0xFF) {
                    continue;
                }
                int b1 = raf.read();
                int b2 = raf.read();
                header = parseHeader(b0, b1, b2);
                if (header != null) {
                    offset = pos;
                    break;
                }
            }
            if (header == null) {
                return null;
            }
            // Xing/Info（VBR）总帧数优先，其次 VBRI，最后 CBR 码率估算
            Integer frames = readXingFrames(raf, offset, header);
            if (frames == null) {
                frames = readVbriFrames(raf, offset);
            }
            if (frames != null) {
                double seconds = (double) frames * header.samplesPerFrame / header.sampleRate;
                return (int) Math.round(seconds);
            }
            // CBR 估算：有效音频字节 = 总长 - ID3v2 - 末尾 ID3v1（128 字节）
            long audioBytes = length - offset;
            if (hasId3v1(raf, length)) {
                audioBytes -= 128;
            }
            double seconds = audioBytes * 8.0 / (header.bitrateKbps * 1000.0);
            return seconds < 0 ? null : (int) Math.round(seconds);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 跳过文件头 ID3v2 标签
     *
     * @return 音频数据起始偏移（无标签返回 0）
     */
    private static long skipId3v2(RandomAccessFile raf, long length) throws IOException {
        if (length < 10) {
            return 0;
        }
        raf.seek(0);
        byte[] head = new byte[10];
        if (raf.read(head) != 10 || head[0] != 'I' || head[1] != 'D' || head[2] != '3') {
            return 0;
        }
        // synchsafe 整数：每字节仅低 7 位有效
        long size = ((head[6] & 0x7FL) << 21) | ((head[7] & 0x7FL) << 14)
                | ((head[8] & 0x7FL) << 7) | (head[9] & 0x7FL);
        return Math.min(10 + size, length);
    }

    /**
     * 解析 4 字节帧头（b0=0xFF 前提下）
     *
     * @return 有效帧头；版本/层/码率/采样率任一非法返回 null（继续扫描下一个同步字）
     */
    private static FrameHeader parseHeader(int b0, int b1, int b2) {
        int versionBits = (b1 >> 3) & 0x3;   // 0=MPEG2.5 2=MPEG2 3=MPEG1 1=保留
        int layerBits = (b1 >> 1) & 0x3;     // 1=Layer III 2=Layer II 3=Layer I 0=保留
        int bitrateIndex = (b2 >> 4) & 0xF;  // 0=free 15=bad
        int sampleIndex = (b2 >> 2) & 0x3;   // 3=保留
        if (versionBits == 1 || layerBits == 0 || bitrateIndex == 0 || bitrateIndex == 15 || sampleIndex == 3) {
            return null;
        }
        boolean mpeg1 = versionBits == 3;
        // layerBits 1/2/3 = Layer III/II/I → 码率行与采样表下标 2/1/0（数组按 L1/L2/L3 排列）
        int layer = 3 - layerBits;
        int rateRow = versionBits == 3 ? 0 : versionBits == 2 ? 1 : 2;
        int bitrateRow = (mpeg1 ? 0 : 3) + layer;
        int bitrateKbps = BITRATE_KBPS[bitrateRow][bitrateIndex - 1];
        int sampleRate = SAMPLE_RATES[rateRow][sampleIndex];
        int samplesPerFrame = (mpeg1 ? SAMPLES_PER_FRAME_MPEG1 : SAMPLES_PER_FRAME_MPEG2)[layer];
        boolean mono = ((b2 >> 6) & 0x3) == 3;
        return new FrameHeader(mpeg1, layer, bitrateKbps, sampleRate, samplesPerFrame, mono);
    }

    /**
     * 读 Xing/Info 头的总帧数（VBR 精确；CBR 编码器通常也写 Info 头）
     */
    private static Integer readXingFrames(RandomAccessFile raf, long frameOffset, FrameHeader header) throws IOException {
        // Xing/Info 位于帧头 + 侧信息之后：MPEG1 立体声 32 / 单声道 17，MPEG2 减半 17/9
        int sideInfo = header.mpeg1 ? (header.mono ? 17 : 32) : (header.mono ? 9 : 17);
        raf.seek(frameOffset + 4 + sideInfo);
        byte[] tag = new byte[4];
        if (raf.read(tag) != 4) {
            return null;
        }
        String marker = new String(tag, StandardCharsets.US_ASCII);
        if (!"Xing".equals(marker) && !"Info".equals(marker)) {
            return null;
        }
        int flags = raf.read();
        flags = (flags << 8) | raf.read();
        flags = (flags << 8) | raf.read();
        flags = (flags << 8) | raf.read();
        if ((flags & 0x1) == 0) {
            return null; // 未写 FRAMES 字段
        }
        int frames = 0;
        for (int i = 0; i < 4; i++) {
            frames = (frames << 8) | raf.read();
        }
        return frames > 0 ? frames : null;
    }

    /**
     * 读 VBRI 头的总帧数（Fraunhofer 编码器，位于帧头 +32 字节处）
     */
    private static Integer readVbriFrames(RandomAccessFile raf, long frameOffset) throws IOException {
        raf.seek(frameOffset + 4 + 32);
        byte[] tag = new byte[4];
        if (raf.read(tag) != 4 || !"VBRI".equals(new String(tag, StandardCharsets.US_ASCII))) {
            return null;
        }
        raf.skipBytes(6); // version(2) + delay(2) + quality(2)
        int frames = 0;
        for (int i = 0; i < 4; i++) {
            frames = (frames << 8) | raf.read();
        }
        return frames > 0 ? frames : null;
    }

    /**
     * 文件末尾是否有 ID3v1 标签（128 字节，"TAG" 起始）
     */
    private static boolean hasId3v1(RandomAccessFile raf, long length) throws IOException {
        if (length < 128) {
            return false;
        }
        raf.seek(length - 128);
        byte[] tag = new byte[3];
        return raf.read(tag) == 3 && tag[0] == 'T' && tag[1] == 'A' && tag[2] == 'G';
    }

    /**
     * 帧头关键字段
     */
    private record FrameHeader(boolean mpeg1, int layer, int bitrateKbps,
                               int sampleRate, int samplesPerFrame, boolean mono) {
    }
}
