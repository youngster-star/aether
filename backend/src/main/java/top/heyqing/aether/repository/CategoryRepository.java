package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.Category;

/**
 * 分类表数据访问（全站共用字典，BackEnd-Plan §6.2 category）
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 按业务域查询分类（排序号升序）
     */
    List<Category> findByBizTypeOrderBySortAsc(String bizType);

    /**
     * 按业务域统计（seed 幂等判断用）
     */
    long countByBizType(String bizType);
}
