package top.heyqing.aether.service.article;

import java.util.List;

import top.heyqing.aether.model.dto.ArticleStyleRequest;
import top.heyqing.aether.model.vo.ArticleStyleVO;

/**
 * 文章样式管理服务（BackEnd-Plan §5.2 /admin/article-styles）
 *
 * <p>样式可被多篇文章复用；is_default=1 全站仅一个，设置新默认时自动取消旧默认；
 * 默认样式与被引用样式禁止删除（防呆）。</p>
 */
public interface ArticleStyleService {

    /**
     * 样式列表
     */
    List<ArticleStyleVO> list();

    /**
     * 新建样式
     *
     * @return 新样式 ID
     */
    Long create(ArticleStyleRequest request);

    /**
     * 修改样式
     */
    void update(Long id, ArticleStyleRequest request);

    /**
     * 删除样式（默认样式/被文章引用时拒绝）
     */
    void delete(Long id);
}
