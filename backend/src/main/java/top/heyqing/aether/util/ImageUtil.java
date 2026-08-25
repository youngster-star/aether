package top.heyqing.aether.util;

import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * 图片工具（BackEnd-Plan §8.2：上传合并后探测宽高）
 */
public final class ImageUtil {

    private ImageUtil() {
    }

    /**
     * 探测图片宽高（像素）
     *
     * @return [width, height]；解析失败（如 webp 无内置 reader）返回 null
     */
    public static int[] probeSize(InputStream in) {
        try (in) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                return null;
            }
            return new int[]{image.getWidth(), image.getHeight()};
        } catch (IOException e) {
            return null;
        }
    }
}
