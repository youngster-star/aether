package top.heyqing.aether.model.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 游客日统计表（BackEnd-Plan §6.2 visitor_daily_stat）
 *
 * <p>每日 01:30 由统计 job 从 visitor_log 聚合生成，按省级行政区划代码聚合
 * （ECharts 中国地图数据源），独立主键实体。</p>
 */
@Entity
@Table(name = "visitor_daily_stat",
        uniqueConstraints = @UniqueConstraint(name = "uk_visitor_date_region",
                columnNames = {"stat_date", "region_code"}))
public class VisitorDailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 统计日期 */
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /** 行政区划代码（省级，ECharts 地图匹配用） */
    @Column(name = "region_code", nullable = false, length = 20)
    private String regionCode;

    /** 地区名称 */
    @Column(name = "region_name", length = 100)
    private String regionName;

    /** 访问次数 */
    @Column(name = "visit_count", nullable = false)
    private Integer visitCount = 0;

    /** 独立 IP 数 */
    @Column(name = "unique_ip", nullable = false)
    private Integer uniqueIp = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public Integer getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(Integer visitCount) {
        this.visitCount = visitCount;
    }

    public Integer getUniqueIp() {
        return uniqueIp;
    }

    public void setUniqueIp(Integer uniqueIp) {
        this.uniqueIp = uniqueIp;
    }
}
