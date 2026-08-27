package top.heyqing.aether.service.album;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.model.dto.AlbumImageBatchRequest;
import top.heyqing.aether.model.dto.AlbumSaveRequest;
import top.heyqing.aether.model.vo.AlbumAdminVO;

/**
 * 图集管理服务（BackEnd-Plan §5.2 /admin/albums）
 *
 * <p>封面与图片文件经 storage_ref 登记引用（§8.3）：删除图集时引用归零的
 * 文件自动标记待清理，事务提交后异步物理删除。</p>
 */
public interface AlbumAdminService {

    /**
     * 管理端分页列表（keyword 搜索标题/介绍）
     */
    PageResult<AlbumAdminVO> list(int page, int size, String keyword);

    /**
     * 新建图集
     */
    Long create(AlbumSaveRequest request);

    /**
     * 编辑图集（封面更换时旧封面引用自动解绑清理）
     */
    void update(Long id, AlbumSaveRequest request);

    /**
     * 删除图集（含全部图片；引用归零文件进入物理清理链路）
     */
    void delete(Long id);

    /**
     * 批量添加图片（含排序/标题/介绍）
     */
    void addImages(Long albumId, AlbumImageBatchRequest request);

    /**
     * 删除单张图片（文件引用解绑，归零则物理清理）
     */
    void deleteImage(Long albumId, Long imageId);
}
