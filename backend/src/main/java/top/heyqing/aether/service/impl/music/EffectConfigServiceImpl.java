package top.heyqing.aether.service.impl.music;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.service.music.EffectConfigService;
import top.heyqing.aether.service.storage.FileUploadService;
import top.heyqing.aether.util.EffectConfigValidator;

/**
 * EffectConfig 生成实现（BackEnd-Plan §7.3）
 *
 * <p>调色板：封面缩略（24×24）像素 k-means 聚 3 类取主色，失败回退羊皮卷默认色；
 * 图层固定覆盖三类音频绑定（粒子=响度 / 波形=频段 / 圆环=节拍），节奏参数由
 * 时长粗估 BPM 驱动；产物过 {@link EffectConfigValidator} 后返回。</p>
 */
@Service
public class EffectConfigServiceImpl implements EffectConfigService {

    /** 默认调色板（羊皮卷基调，封面缺失/解析失败回退） */
    private static final List<String> DEFAULT_PALETTE = List.of("#E8C94A", "#1A1B1F", "#8B6F47");

    /** 调色板提取采样尺寸 */
    private static final int SAMPLE_SIZE = 24;

    /** k-means 迭代次数（小样本足够收敛） */
    private static final int KMEANS_ITERATIONS = 6;

    private final StorageFileRepository storageFileRepository;
    private final FileUploadService fileUploadService;

    public EffectConfigServiceImpl(StorageFileRepository storageFileRepository, FileUploadService fileUploadService) {
        this.storageFileRepository = storageFileRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public String generate(Long coverFileId, Integer duration) {
        List<String> palette = extractPalette(coverFileId);
        // 节奏粗估：长曲（>240s）按慢歌处理，其余按中快歌；驱动粒子数与速度
        int bpm = duration != null && duration > 240 ? 90 : 110;
        int particleCount = bpm == 90 ? 100 : 130;
        double speed = bpm / 100.0;
        String config = """
                {"version":1,
                 "palette":%s,
                 "layers":[
                   {"type":"particles","bind":"amplitude","count":%d,"sizeRange":[1,6],"speed":%s,"opacity":0.7},
                   {"type":"wave","bind":"freqBand","band":[0,0.3],"amplitude":40,"color":"%s"},
                   {"type":"ring","bind":"beat","amplitude":30,"color":"%s"}],
                 "background":{"type":"gradient","from":"#0F1013","to":"#1A1B1F"},
                 "transition":{"duration":800,"easing":"easeOutCubic"},
                 "sandboxCode":null}"""
                .formatted(jsonArray(palette), particleCount, speed, palette.get(0), palette.get(2));
        // 防御性校验：生成器自身产物必须合法，否则视为生成失败（30802）
        String error = EffectConfigValidator.validate(config);
        if (error != null) {
            throw new BusinessException(ErrorCode.AI_GENERATE_FAILED, "生成的特效配置未通过 Schema 校验: " + error);
        }
        return config;
    }

    /**
     * 封面主色提取：缩略采样 + k-means 聚 3 类（按簇规模降序）
     *
     * <p>封面缺失/读取失败一律回退默认调色板（探测式失败不阻断生成）。</p>
     */
    private List<String> extractPalette(Long coverFileId) {
        if (coverFileId == null) {
            return DEFAULT_PALETTE;
        }
        try {
            StorageFile file = storageFileRepository.findById(coverFileId)
                    .filter(item -> item.getStatus() == 1)
                    .orElse(null);
            if (file == null) {
                return DEFAULT_PALETTE;
            }
            BufferedImage source;
            try (InputStream in = fileUploadService.openFile(file)) {
                source = ImageIO.read(in);
            }
            if (source == null) {
                return DEFAULT_PALETTE;
            }
            // 缩略到采样尺寸（平均化像素，降低 k-means 输入量）
            BufferedImage sampled = new BufferedImage(SAMPLE_SIZE, SAMPLE_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = sampled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE, null);
            graphics.dispose();
            List<int[]> pixels = new ArrayList<>(SAMPLE_SIZE * SAMPLE_SIZE);
            for (int y = 0; y < SAMPLE_SIZE; y++) {
                for (int x = 0; x < SAMPLE_SIZE; x++) {
                    int rgb = sampled.getRGB(x, y);
                    pixels.add(new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF});
                }
            }
            // 初始中心取首/中/尾采样点（确定性，无随机数）
            int[][] centers = {
                    pixels.get(0).clone(),
                    pixels.get(pixels.size() / 2).clone(),
                    pixels.get(pixels.size() - 1).clone()};
            int[] counts = new int[3];
            for (int iter = 0; iter < KMEANS_ITERATIONS; iter++) {
                int[][] sums = new int[3][3];
                counts = new int[3];
                for (int[] pixel : pixels) {
                    int nearest = 0;
                    int bestDistance = Integer.MAX_VALUE;
                    for (int k = 0; k < 3; k++) {
                        int distance = square(pixel[0] - centers[k][0])
                                + square(pixel[1] - centers[k][1])
                                + square(pixel[2] - centers[k][2]);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            nearest = k;
                        }
                    }
                    sums[nearest][0] += pixel[0];
                    sums[nearest][1] += pixel[1];
                    sums[nearest][2] += pixel[2];
                    counts[nearest]++;
                }
                for (int k = 0; k < 3; k++) {
                    if (counts[k] > 0) {
                        centers[k] = new int[]{sums[k][0] / counts[k], sums[k][1] / counts[k], sums[k][2] / counts[k]};
                    }
                }
            }
            // 按最后一轮簇规模降序输出（主色在前）
            record Cluster(int[] center, int count) {
            }
            int[] finalCounts = counts;
            return java.util.stream.IntStream.range(0, 3)
                    .mapToObj(k -> new Cluster(centers[k], finalCounts[k]))
                    .sorted(Comparator.comparingInt(Cluster::count).reversed())
                    .map(cluster -> String.format(Locale.ROOT, "#%02X%02X%02X",
                            cluster.center()[0], cluster.center()[1], cluster.center()[2]))
                    .toList();
        } catch (Exception e) {
            return DEFAULT_PALETTE;
        }
    }

    private static int square(int value) {
        return value * value;
    }

    /**
     * 颜色列表 → JSON 数组字面量（值已过十六进制格式化，无需转义）
     */
    private static String jsonArray(List<String> colors) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < colors.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(colors.get(i)).append('"');
        }
        return builder.append(']').toString();
    }
}
