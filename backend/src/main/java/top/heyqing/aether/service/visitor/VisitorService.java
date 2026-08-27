package top.heyqing.aether.service.visitor;

import java.time.LocalDate;

/**
 * 游客访问统计服务（BackEnd-Plan §4.6）
 */
public interface VisitorService {

    /**
     * 记录游客访问
     *
     * <p>同一 IP 在去重窗口（30 分钟）内只记一次；解析 ip2region 地域与 UA
     * 后写入 visitor_log。统计链路异常静默降级，不阻断业务请求。</p>
     *
     * @param ip        客户端 IP（IpUtil 解析后的真实 IP）
     * @param userAgent 原始 User-Agent
     * @param visitPath 访问路径
     * @param referer   来源页面
     */
    void recordVisit(String ip, String userAgent, String visitPath, String referer);

    /**
     * 聚合指定日期的地域统计（日统计 job 调用）
     *
     * <p>按省份聚合 visitor_log 的访问量与独立 IP 数写入 visitor_daily_stat；
     * 幂等：重跑时先清理当日旧数据。</p>
     *
     * @param statDate 统计日期
     */
    void aggregateDaily(LocalDate statDate);
}
