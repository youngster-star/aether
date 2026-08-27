package top.heyqing.aether.job;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import top.heyqing.aether.service.visitor.VisitorService;

/**
 * 游客日统计定时任务（BackEnd-Plan §4.6）
 *
 * <p>每天 01:30 聚合前一日 visitor_log 数据到 visitor_daily_stat，
 * 供管理端 ECharts 地域分布地图查询。</p>
 */
@Component
public class VisitorStatJob {

    private static final Logger log = LoggerFactory.getLogger(VisitorStatJob.class);

    private final VisitorService visitorService;

    public VisitorStatJob(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    /**
     * 每日 01:30 执行（Spring cron：秒 分 时 日 月 周）
     */
    @Scheduled(cron = "0 30 1 * * *")
    public void aggregateYesterday() {
        log.info("游客日统计 job 启动");
        visitorService.aggregateDaily(LocalDate.now().minusDays(1));
    }
}
