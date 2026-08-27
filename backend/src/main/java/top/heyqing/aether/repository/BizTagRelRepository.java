package top.heyqing.aether.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.BizTagRel;

/**
 * 业务-标签关联仓库（BackEnd-Plan §6.2 biz_tag_rel）
 */
public interface BizTagRelRepository extends JpaRepository<BizTagRel, Long> {

    List<BizTagRel> findByBizTypeAndBizId(String bizType, Long bizId);

    List<BizTagRel> findByBizTypeAndBizIdIn(String bizType, Collection<Long> bizIds);

    void deleteByBizTypeAndBizId(String bizType, Long bizId);

    boolean existsByBizTypeAndTagId(String bizType, Long tagId);
}
