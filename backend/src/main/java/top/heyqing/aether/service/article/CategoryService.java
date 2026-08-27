package top.heyqing.aether.service.article;

import java.util.List;

import top.heyqing.aether.model.dto.CategorySaveRequest;
import top.heyqing.aether.model.dto.TagSaveRequest;
import top.heyqing.aether.model.vo.CategoryVO;
import top.heyqing.aether.model.vo.TagVO;

/**
 * 分类/标签字典管理服务（全站共用，bizType 区分业务域，BackEnd-Plan §5.2）
 *
 * <p>公开列表走 ArticleService（本服务负责管理端 CRUD）；删除被业务引用的
 * 分类/标签被拒绝（防呆，避免孤儿关联）。</p>
 */
public interface CategoryService {

    /**
     * 新建分类（业务域内 slug 唯一）
     *
     * @return 新分类 ID
     */
    Long createCategory(CategorySaveRequest request);

    /**
     * 修改分类
     */
    void updateCategory(Long id, CategorySaveRequest request);

    /**
     * 删除分类（被业务引用时拒绝）
     */
    void deleteCategory(Long id);

    /**
     * 新建标签
     *
     * @return 新标签 ID
     */
    Long createTag(TagSaveRequest request);

    /**
     * 修改标签
     */
    void updateTag(Long id, TagSaveRequest request);

    /**
     * 删除标签（被业务引用时拒绝）
     */
    void deleteTag(Long id);

    /**
     * 管理端分类列表（bizType 过滤）
     */
    List<CategoryVO> listCategories(String bizType);

    /**
     * 管理端标签列表（bizType 过滤）
     */
    List<TagVO> listTags(String bizType);
}
