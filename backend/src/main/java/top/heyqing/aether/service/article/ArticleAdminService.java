package top.heyqing.aether.service.article;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.model.dto.ArticleHotRequest;
import top.heyqing.aether.model.dto.ArticleSaveRequest;
import top.heyqing.aether.model.vo.ArticleAdminVO;

/**
 * 文章管理服务（BackEnd-Plan §5.2 /admin/articles，需 Access Token）
 */
public interface ArticleAdminService {

    /**
     * 管理端分页列表（含草稿，可按发布状态/关键词过滤）
     */
    PageResult<ArticleAdminVO> list(int page, int size, String keyword, Integer isPublished);

    /**
     * 管理端详情（含草稿与编辑回显字段）
     */
    ArticleAdminVO get(Long id);

    /**
     * 新建文章（默认草稿；内容入库前 jsoup sanitize）
     *
     * @return 新文章 ID
     */
    Long create(ArticleSaveRequest request);

    /**
     * 编辑文章（重建分类标签关联）
     */
    void update(Long id, ArticleSaveRequest request);

    /**
     * 删除文章（物理删 + 关联清理；封面文件暂不物理清理，storage_ref 引用机制阶段 3+ 接入）
     */
    void delete(Long id);

    /**
     * 发布/下线（首次发布记录 publishTime）
     */
    void publish(Long id, Integer isPublished);

    /**
     * 设置热度（isHot/hotOrder）
     */
    void setHot(Long id, ArticleHotRequest request);

    /**
     * 绑定独立样式（styleId 传 null 解绑回默认样式）
     */
    void bindStyle(Long id, Long styleId);
}
