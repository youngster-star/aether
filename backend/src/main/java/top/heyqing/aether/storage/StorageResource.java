package top.heyqing.aether.storage;

import java.io.InputStream;

import org.springframework.core.io.AbstractResource;

/**
 * 存储文件资源：包装存储层读取流并提供显式文件长度
 *
 * <p>Spring 6+ 的 InputStreamResource 无法提供 contentLength（Range 支持失效），
 * 自定义 Resource 使 ResourceHttpMessageConverter 正常返回 206 分段响应
 * （视频拖动进度条、音频 seek 依赖 Range，BackEnd-Plan §4.4）。</p>
 */
public class StorageResource extends AbstractResource {

    private final InputStream inputStream;
    private final long length;

    public StorageResource(InputStream inputStream, long length) {
        this.inputStream = inputStream;
        this.length = length;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public long contentLength() {
        return length;
    }

    @Override
    public String getDescription() {
        return "storage-file";
    }
}
