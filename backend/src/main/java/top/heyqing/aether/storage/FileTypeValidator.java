package top.heyqing.aether.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;

/**
 * 文件类型与大小白名单校验（BackEnd-Plan §4.5/§8.4）
 *
 * <p>扩展名白名单 + 文件魔数双重校验；大小上限按类型区分。</p>
 */
public final class FileTypeValidator {

    /** 文件类别与大小上限（字节） */
    public enum FileCategory {
        /** 图片 50MB */
        IMAGE(50L * 1024 * 1024),
        /** 视频 2GB */
        VIDEO(2L * 1024 * 1024 * 1024),
        /** 音频 200MB */
        AUDIO(200L * 1024 * 1024),
        /** 文本 100MB */
        TEXT(100L * 1024 * 1024);

        final long maxSize;

        FileCategory(long maxSize) {
            this.maxSize = maxSize;
        }
    }

    /** 扩展名白名单（小写 -> 类别） */
    private static final Map<String, FileCategory> EXT_WHITELIST = Map.ofEntries(
            Map.entry("jpg", FileCategory.IMAGE), Map.entry("jpeg", FileCategory.IMAGE),
            Map.entry("png", FileCategory.IMAGE), Map.entry("gif", FileCategory.IMAGE),
            Map.entry("webp", FileCategory.IMAGE),
            Map.entry("mp4", FileCategory.VIDEO), Map.entry("webm", FileCategory.VIDEO),
            Map.entry("mp3", FileCategory.AUDIO), Map.entry("flac", FileCategory.AUDIO),
            Map.entry("wav", FileCategory.AUDIO), Map.entry("aac", FileCategory.AUDIO),
            Map.entry("m4a", FileCategory.AUDIO),
            Map.entry("txt", FileCategory.TEXT), Map.entry("md", FileCategory.TEXT));

    /** 文本类文件无固定魔数，跳过魔数校验 */
    private static final Set<String> SKIP_MAGIC_EXTS = Set.of("txt", "md");

    /** 图片扩展名集合（merge 后探测宽高用） */
    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 音视频扩展名集合（merge 后时长探测用，BackEnd-Plan §8.2） */
    private static final Set<String> AV_EXTS = Set.of("mp4", "webm", "mp3", "flac", "wav", "aac", "m4a");

    private FileTypeValidator() {
    }

    /**
     * 扩展名白名单校验（init 阶段调用）
     *
     * @param originalName 原始文件名
     * @param size         文件大小（字节）
     * @return 小写扩展名（不含点）
     * @throws BusinessException 10005 类型不支持 / 10006 大小超限
     */
    public static String validate(String originalName, long size) {
        String ext = extractExt(originalName);
        FileCategory category = EXT_WHITELIST.get(ext);
        if (category == null) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
        if (size <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小必须大于 0");
        }
        if (size > category.maxSize) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        return ext;
    }

    /**
     * 文件魔数校验（merge 后调用，读取文件头比对，BackEnd-Plan §8.4）
     *
     * @throws BusinessException 10005 魔数与扩展名不符（伪造类型）
     */
    public static void verifyMagic(String ext, InputStream in) {
        if (SKIP_MAGIC_EXTS.contains(ext)) {
            return;
        }
        byte[] head = new byte[12];
        try (in) {
            int read = in.readNBytes(head, 0, 12);
            if (read < 8) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "文件内容过短，无法识别类型");
            }
            boolean match = switch (ext) {
                case "jpg", "jpeg" -> isJpeg(head);
                case "png" -> startsWith(head, 0x89, 0x50, 0x4E, 0x47);
                case "gif" -> head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8';
                case "webp" -> head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                        && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
                case "mp4", "m4a" -> head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p';
                case "webm" -> head[0] == 0x1A && head[1] == 0x45 && head[2] == (byte) 0xDF && head[3] == (byte) 0xA3;
                case "mp3" -> isMp3(head);
                case "flac" -> head[0] == 'f' && head[1] == 'L' && head[2] == 'a' && head[3] == 'C';
                case "wav" -> head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                        && head[8] == 'W' && head[9] == 'A' && head[10] == 'V' && head[11] == 'E';
                case "aac" -> startsWith(head, 'A', 'D', 'I', 'F') || startsWith(head, 0xFF, 0xF1)
                        || startsWith(head, 0xFF, 0xF9);
                default -> false;
            };
            if (!match) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "文件内容与扩展名不符");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "文件读取失败");
        }
    }

    /**
     * 是否图片（merge 后探测宽高判断用）
     */
    public static boolean isImage(String ext) {
        return IMAGE_EXTS.contains(ext);
    }

    /**
     * 是否音视频（merge 后时长探测判断用）
     */
    public static boolean isVideoOrAudio(String ext) {
        return AV_EXTS.contains(ext);
    }

    /**
     * 是否视频（管理端视频模块校验用，仅 mp4/webm）
     */
    public static boolean isVideo(String ext) {
        return "mp4".equals(ext) || "webm".equals(ext);
    }

    /**
     * 是否音频（管理端音乐模块校验用，§8.4 音频白名单）
     */
    public static boolean isAudio(String ext) {
        return "mp3".equals(ext) || "flac".equals(ext) || "wav".equals(ext)
                || "aac".equals(ext) || "m4a".equals(ext);
    }

    /**
     * 按扩展名推断 MIME 类型（存储元数据用）
     */
    public static String mimeTypeOf(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mp3" -> "audio/mpeg";
            case "flac" -> "audio/flac";
            case "wav" -> "audio/wav";
            case "aac" -> "audio/aac";
            case "m4a" -> "audio/mp4";
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            default -> "application/octet-stream";
        };
    }

    /**
     * 提取小写扩展名（不含点）
     */
    public static String extractExt(String originalName) {
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isJpeg(byte[] head) {
        return head[0] == (byte) 0xFF && head[1] == (byte) 0xD8 && head[2] == (byte) 0xFF;
    }

    private static boolean isMp3(byte[] head) {
        // ID3 标签头或 MPEG 帧同步字（FF Ex / FF Fx）
        if (head[0] == 'I' && head[1] == 'D' && head[2] == '3') {
            return true;
        }
        return head[0] == (byte) 0xFF && (head[1] & 0xE0) == 0xE0;
    }

    private static boolean startsWith(byte[] head, int... bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (head[i] != (byte) bytes[i]) {
                return false;
            }
        }
        return true;
    }
}
