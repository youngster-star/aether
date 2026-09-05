package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import top.heyqing.aether.model.entity.Book;

/**
 * 书籍仓储（BackEnd-Plan §6.2 book）
 *
 * <p>公开/管理列表的 categoryId/tagId/keyword 过滤走 Specification 动态条件
 * （分类标签经 biz_category_rel/biz_tag_rel 关联）。</p>
 */
public interface BookRepository extends JpaRepository<Book, Long>,
        JpaSpecificationExecutor<Book> {

    /**
     * 推荐书籍（is_recommend=1，id 降序）
     */
    List<Book> findByIsRecommend(Integer isRecommend, Pageable pageable);
}
