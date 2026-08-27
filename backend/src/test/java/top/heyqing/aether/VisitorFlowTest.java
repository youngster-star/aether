package top.heyqing.aether;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import top.heyqing.aether.model.entity.VisitorLog;
import top.heyqing.aether.repository.VisitorDailyStatRepository;
import top.heyqing.aether.repository.VisitorLogRepository;
import top.heyqing.aether.service.visitor.VisitorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 游客访问分析功能测试（阶段 2：BackEnd-Plan §4.6）
 *
 * <p>验证：拦截器记录访问（IP/路径/UA 解析落库）、同 IP 30 分钟窗口去重、
 * 日统计聚合（省份 → 区划代码 → visitor_daily_stat）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class VisitorFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VisitorLogRepository visitorLogRepository;

    @Autowired
    private VisitorDailyStatRepository visitorDailyStatRepository;

    @Autowired
    private VisitorService visitorService;

    @Test
    @DisplayName("拦截器记录访问：IP/路径/UA 解析落库，同 IP 窗口内不重复记录")
    void visitorRecordedWithDedup() throws Exception {
        long before = visitorLogRepository.count();

        // 第一次访问：记录
        mockMvc.perform(get("/v1/articles")
                        .header("X-Forwarded-For", "8.8.8.8")
                        .header("User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                        + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"))
                .andExpect(status().isOk());
        assertEquals(before + 1, visitorLogRepository.count(), "首次访问应新增 1 条记录");

        VisitorLog log = visitorLogRepository.findAll().stream()
                .max(Comparator.comparing(VisitorLog::getId)).orElseThrow();
        assertEquals("8.8.8.8", log.getIp());
        assertEquals("/v1/articles", log.getVisitPath());
        assertEquals("PC", log.getDeviceType());
        assertEquals("Chrome", log.getBrowser());
        assertEquals("Windows 10/11", log.getOs());
        // 8.8.8.8 为美国 IP：region 应含国家名（不参与国内省份聚合）
        assertTrue(log.getRegion() != null && log.getRegion().contains("美国"),
                "美国 IP 地域串应含国家名，实际: " + log.getRegion());

        // 同 IP 30 分钟窗口内第二次访问：不重复记录
        mockMvc.perform(get("/v1/tags").header("X-Forwarded-For", "8.8.8.8"))
                .andExpect(status().isOk());
        assertEquals(before + 1, visitorLogRepository.count(), "同 IP 窗口内不应重复记录");

        // 不同 IP：正常记录
        mockMvc.perform(get("/v1/categories").header("X-Forwarded-For", "1.1.1.1"))
                .andExpect(status().isOk());
        assertEquals(before + 2, visitorLogRepository.count(), "不同 IP 应正常记录");
    }

    @Test
    @DisplayName("日统计聚合：省份 → 区划代码 → visitor_daily_stat（幂等重跑）")
    void aggregateDailyByProvince() {
        // 构造两省访问数据（陕西 2 条含 1 重复 IP、广东 1 条）
        saveLog("10.0.1.1", "陕西", "西安");
        saveLog("10.0.1.1", "陕西", "西安");
        saveLog("10.0.2.1", "广东", "深圳");

        visitorService.aggregateDaily(LocalDate.now());

        var stats = visitorDailyStatRepository.findByStatDateBetweenOrderByStatDate(
                LocalDate.now(), LocalDate.now());
        assertEquals(2, stats.size(), "应聚合出 2 个省份");

        var shaanxi = stats.stream().filter(s -> s.getRegionName().equals("陕西")).findFirst().orElseThrow();
        assertEquals("610000", shaanxi.getRegionCode(), "陕西应映射区划代码 610000");
        assertEquals(2, shaanxi.getVisitCount(), "陕西访问次数应为 2（同一 IP 两次）");
        assertEquals(1, shaanxi.getUniqueIp(), "陕西独立 IP 应为 1（10.0.1.1 去重）");

        var guangdong = stats.stream().filter(s -> s.getRegionName().equals("广东")).findFirst().orElseThrow();
        assertEquals("440000", guangdong.getRegionCode());

        // 幂等重跑：结果不翻倍
        visitorService.aggregateDaily(LocalDate.now());
        assertEquals(2, visitorDailyStatRepository.findByStatDateBetweenOrderByStatDate(
                LocalDate.now(), LocalDate.now()).size(), "重跑聚合不应产生重复数据");
    }

    /**
     * 直接落一条带省份的访问记录（绕过 ip2region，数据可控）
     */
    private void saveLog(String ip, String province, String city) {
        VisitorLog log = new VisitorLog();
        log.setIp(ip);
        log.setCountry("中国");
        log.setProvince(province);
        log.setCity(city);
        log.setRegion("中国·" + province + "·" + city);
        log.setVisitPath("/v1/articles");
        log.setVisitTime(LocalDateTime.now());
        visitorLogRepository.save(log);
    }
}
