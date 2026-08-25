package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.Tag;

/**
 * 标签表数据访问（全站共用字典，BackEnd-Plan §6.2 tag）
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * 按业务域查询标签
     */
    List<Tag> findByBizType(String bizType);

    /**
     * 按业务域统计（seed 幂等判断用）
     */
    long countByBizType(String bizType);
}
