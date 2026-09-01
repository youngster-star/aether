package top.heyqing.aether.service.impl.storage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import top.heyqing.aether.service.storage.MediaProbeService;
import top.heyqing.aether.storage.StorageService;
import top.heyqing.aether.util.Mp3DurationParser;
import top.heyqing.aether.util.Mp4DurationParser;

/**
 * 时长探测实现（BackEnd-Plan §8.2）
 *
 * <p>探测链路：ffprobe 外部二进制（路径 aether.media.ffprobe-path，60s 超时）
 * → MP4/M4A 内置 mvhd 解析回退（零依赖，webm 等无回退则放弃）。
 * OSS 对象先溢出到临时文件再探测（远端对象无法直接给 ffprobe 路径）。</p>
 */
@Service
public class MediaProbeServiceImpl implements MediaProbeService {

    private static final Logger log = LoggerFactory.getLogger(MediaProbeServiceImpl.class);

    /** 参与时长探测的扩展名 */
    private static final Set<String> PROBE_EXTS = Set.of("mp4", "webm", "mp3", "flac", "wav", "aac", "m4a");

    /** 可走内置 mvhd 解析回退的扩展名 */
    private static final Set<String> MP4_EXTS = Set.of("mp4", "m4a");

    /** 可走内置 MP3 帧结构解析回退的扩展名 */
    private static final Set<String> MP3_EXTS = Set.of("mp3");

    /** ffprobe 单次探测超时 */
    private static final long FFPROBE_TIMEOUT_SECONDS = 60;

    private final String ffprobePath;

    public MediaProbeServiceImpl(@Value("${aether.media.ffprobe-path:ffprobe}") String ffprobePath) {
        this.ffprobePath = ffprobePath;
    }

    @Override
    public Integer probeDurationSeconds(String ext, StorageService storage, String objectKey) {
        String normalized = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        if (!PROBE_EXTS.contains(normalized)) {
            return null;
        }
        Path localPath = null;
        boolean spilled = false;
        try {
            localPath = storage.localPathOf(objectKey);
            if (localPath == null) {
                // 远端存储：对象流溢出临时文件（探测完成后删除）
                localPath = Files.createTempFile("aether-probe-", "." + normalized);
                try (InputStream in = storage.open(objectKey)) {
                    Files.copy(in, localPath, StandardCopyOption.REPLACE_EXISTING);
                }
                spilled = true;
            }
            Integer duration = probeWithFfprobe(localPath);
            if (duration == null && MP4_EXTS.contains(normalized)) {
                duration = Mp4DurationParser.parseSeconds(localPath);
            }
            if (duration == null && MP3_EXTS.contains(normalized)) {
                duration = Mp3DurationParser.parseSeconds(localPath);
            }
            if (duration == null) {
                log.warn("时长探测失败（ffprobe 不可用且无内置回退）: ext={}, objectKey={}", normalized, objectKey);
            }
            return duration;
        } catch (Exception e) {
            log.warn("时长探测异常: ext={}, objectKey={}, 原因={}", normalized, objectKey, e.getMessage());
            return null;
        } finally {
            if (spilled && localPath != null) {
                try {
                    Files.deleteIfExists(localPath);
                } catch (Exception ignored) {
                    // 临时文件残留由系统临时目录自清理
                }
            }
        }
    }

    /**
     * 调用 ffprobe 读取时长（-of csv 输出纯秒值）
     *
     * @return 时长（秒）；二进制不存在/超时/非零退出返回 null（回退内置解析）
     */
    private Integer probeWithFfprobe(Path file) {
        try {
            Process process = new ProcessBuilder(ffprobePath, "-v", "error",
                    "-show_entries", "format=duration", "-of", "csv=p=0", file.toString())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(FFPROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("ffprobe 探测超时（{}s），放弃等待", FFPROBE_TIMEOUT_SECONDS);
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (output.isBlank() || "N/A".equals(output)) {
                return null;
            }
            double seconds = Double.parseDouble(output.split(",")[0].trim());
            return seconds < 0 ? null : (int) Math.round(seconds);
        } catch (Exception e) {
            return null; // ffprobe 不可用：静默回退内置解析
        }
    }
}
