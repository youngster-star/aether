package top.heyqing.aether.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.BizCategoryRel;

/**
 * 业务-分类关联仓库（BackEnd-Plan §6.2 biz_category_rel）
 */
public interface BizCategoryRelRepository extends JpaRepository<BizCategoryRel, Long> {

    List<BizCategoryRel> findByBizTypeAndBizId(String bizType, Long bizId);

    List<BizCategoryRel> findByBizTypeAndBizIdIn(String bizType, Collection<Long> bizIds);

    void deleteByBizTypeAndBizId(String bizType, Long bizId);

    boolean existsByBizTypeAndCategoryId(String bizType, Long categoryId);
}
