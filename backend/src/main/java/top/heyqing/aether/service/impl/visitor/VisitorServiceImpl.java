package top.heyqing.aether.service.impl.visitor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import top.heyqing.aether.common.cache.CacheStore;
import top.heyqing.aether.model.entity.VisitorDailyStat;
import top.heyqing.aether.model.entity.VisitorLog;
import top.heyqing.aether.repository.VisitorDailyStatRepository;
import top.heyqing.aether.repository.VisitorLogRepository;
import top.heyqing.aether.service.visitor.VisitorService;
import top.heyqing.aether.util.IpRegionService;
import top.heyqing.aether.util.IpRegionService.IpRegion;
import top.heyqing.aether.util.UaParser;

/**
 * 游客访问统计实现（BackEnd-Plan §4.6）
 */
@Service
public class VisitorServiceImpl implements VisitorService {

    private static final Logger log = LoggerFactory.getLogger(VisitorServiceImpl.class);

    /** 同 IP 记录去重窗口（30 分钟，CacheStore key 前缀 +ip） */
    private static final String VISIT_WINDOW_KEY = "visitor:window:";

    private static final Duration VISIT_WINDOW = Duration.ofMinutes(30);

    /** 海外/解析失败的地域占位（visitor_log.province 存 null，不参与省份聚合） */
    private static final String UNKNOWN_REGION = "未知";

    private final VisitorLogRepository visitorLogRepository;
    private final VisitorDailyStatRepository visitorDailyStatRepository;
    private final IpRegionService ipRegionService;
    private final CacheStore cacheStore;

    public VisitorServiceImpl(VisitorLogRepository visitorLogRepository,
                              VisitorDailyStatRepository visitorDailyStatRepository,
                              IpRegionService ipRegionService, CacheStore cacheStore) {
        this.visitorLogRepository = visitorLogRepository;
        this.visitorDailyStatRepository = visitorDailyStatRepository;
        this.ipRegionService = ipRegionService;
        this.cacheStore = cacheStore;
    }

    @Override
    public void recordVisit(String ip, String userAgent, String visitPath, String referer) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        // 同 IP 窗口去重（setIfAbsent 原子；统计是弱一致场景，窗口略长可接受）
        if (!cacheStore.setIfAbsent(VISIT_WINDOW_KEY + ip, "1", VISIT_WINDOW)) {
            return;
        }
        try {
            IpRegion region = ipRegionService.locate(ip);
            UaParser.UaInfo ua = UaParser.parse(userAgent);

            VisitorLog logEntry = new VisitorLog();
            logEntry.setIp(ip);
            logEntry.setCountry(region.country());
            logEntry.setProvince(region.province());
            logEntry.setCity(region.city());
            logEntry.setRegion(region.region() == null ? UNKNOWN_REGION : region.region());
            logEntry.setUserAgent(truncate(userAgent, 500));
            logEntry.setDeviceType(ua.deviceType());
            logEntry.setBrowser(ua.browser());
            logEntry.setOs(ua.os());
            logEntry.setVisitPath(truncate(visitPath, 255));
            logEntry.setReferer(truncate(referer, 500));
            logEntry.setVisitTime(LocalDateTime.now());
            visitorLogRepository.save(logEntry);
        } catch (RuntimeException e) {
            // 统计链路失败仅记录日志，绝不影响业务请求
            log.warn("游客访问记录失败（ip={}）：{}", ip, e.getMessage());
        }
    }

    @Override
    public void aggregateDaily(LocalDate statDate) {
        LocalDateTime start = statDate.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        try {
            var aggregates = visitorLogRepository.aggregateByProvince(start, end);
            if (aggregates.isEmpty()) {
                log.info("日统计：{} 无访问数据，跳过聚合", statDate);
                return;
            }
            // 幂等：重跑前清理当日旧数据
            visitorDailyStatRepository.deleteByStatDate(statDate);
            for (var agg : aggregates) {
                String regionCode = ipRegionService.provinceCode(agg.getProvince());
                if (regionCode == null) {
                    continue;
                }
                VisitorDailyStat stat = new VisitorDailyStat();
                stat.setStatDate(statDate);
                stat.setRegionCode(regionCode);
                stat.setRegionName(agg.getProvince());
                stat.setVisitCount(agg.getVisitCount().intValue());
                stat.setUniqueIp(agg.getUniqueIp().intValue());
                visitorDailyStatRepository.save(stat);
            }
            log.info("日统计完成：{} 聚合 {} 个省份", statDate, aggregates.size());
        } catch (RuntimeException e) {
            log.error("日统计 job 失败（{}）：{}", statDate, e.getMessage());
        }
    }

    /** 超长字段截断（DB 列长保护，防呆） */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
