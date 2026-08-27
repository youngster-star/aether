package top.heyqing.aether.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.VisitorDailyStat;

/**
 * 游客日统计仓库（BackEnd-Plan §4.6，ECharts 地图数据源）
 */
public interface VisitorDailyStatRepository extends JpaRepository<VisitorDailyStat, Long> {

    boolean existsByStatDate(LocalDate statDate);

    List<VisitorDailyStat> findByStatDateBetweenOrderByStatDate(LocalDate start, LocalDate end);

    /** 重跑统计时清理旧数据（job 幂等） */
    void deleteByStatDate(LocalDate statDate);
}
