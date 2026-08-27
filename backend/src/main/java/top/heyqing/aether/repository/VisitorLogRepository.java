package top.heyqing.aether.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import top.heyqing.aether.model.entity.VisitorLog;

/**
 * 游客访问日志仓库（BackEnd-Plan §4.6）
 */
public interface VisitorLogRepository extends JpaRepository<VisitorLog, Long> {

    /**
     * 按省份聚合指定时间段的访问量与独立 IP 数（日统计 job 数据源）
     *
     * <p>参数绑定防注入；province 为 null 的条目（海外/解析失败）不参与省份聚合。</p>
     */
    @Query("""
            SELECT v.province AS province,
                   COUNT(v) AS visitCount,
                   COUNT(DISTINCT v.ip) AS uniqueIp
            FROM VisitorLog v
            WHERE v.visitTime >= :start AND v.visitTime < :end AND v.province IS NOT NULL
            GROUP BY v.province
            """)
    List<ProvinceAgg> aggregateByProvince(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    /**
     * 省份聚合投影（GROUP BY province 结果行）
     */
    interface ProvinceAgg {
        String getProvince();

        Long getVisitCount();

        Long getUniqueIp();
    }
}
