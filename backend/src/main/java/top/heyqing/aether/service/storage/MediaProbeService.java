package top.heyqing.aether.service.storage;

import top.heyqing.aether.storage.StorageService;

/**
 * 音视频时长探测（BackEnd-Plan §8.2）
 *
 * <p>上传合并后调用：首选外部 ffprobe，不可用时回退 MP4/M4A 内置 mvhd 解析；
 * 探测失败一律返回 null（duration=0 落库 + warn 日志），不阻断上传。</p>
 */
public interface MediaProbeService {

    /**
     * 探测音视频时长
     *
     * @param ext       小写扩展名（仅 mp4/webm/mp3/flac/wav/aac/m4a 探测）
     * @param storage   存储后端（本地走文件路径，OSS 溢出临时文件）
     * @param objectKey 对象键
     * @return 时长（秒）；无法探测返回 null
     */
    Integer probeDurationSeconds(String ext, StorageService storage, String objectKey);
}
