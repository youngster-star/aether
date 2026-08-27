package top.heyqing.aether.service.video;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.model.dto.VideoChapterBatchRequest;
import top.heyqing.aether.model.dto.VideoSaveRequest;
import top.heyqing.aether.model.vo.VideoAdminVO;

/**
 * 视频管理服务（BackEnd-Plan §5.2 /admin/videos）
 *
 * <p>视频文件与封面经 storage_ref 登记引用（§8.3）；时长默认取
 * storage_file 探测值（ffprobe/内置解析），探测失败时管理端可手工补录。</p>
 */
public interface VideoAdminService {

    /**
     * 管理端分页列表（keyword 搜索标题/介绍）
     */
    PageResult<VideoAdminVO> list(int page, int size, String keyword);

    /**
     * 新建视频
     */
    Long create(VideoSaveRequest request);

    /**
     * 编辑视频（文件/封面更换时旧引用自动解绑清理）
     */
    void update(Long id, VideoSaveRequest request);

    /**
     * 删除视频（含关键时间节点；引用归零文件进入物理清理链路）
     */
    void delete(Long id);

    /**
     * 整体保存关键时间节点（提交列表即全集，空列表=清空）
     */
    void saveChapters(Long id, VideoChapterBatchRequest request);
}
