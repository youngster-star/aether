package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.SurveyOption;

/**
 * 问卷选项字典表数据访问（BackEnd-Plan §6.2 survey_option）
 */
public interface SurveyOptionRepository extends JpaRepository<SurveyOption, Long> {

    /**
     * 按字段查询选项（排序号升序）
     */
    List<SurveyOption> findByFieldOrderBySortAsc(String field);

    /**
     * 统计（seed 幂等判断用）
     */
    long countByField(String field);
}
