package top.heyqing.aether.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 离线 IP 定位服务（ip2region，BackEnd-Plan §4.6）
 *
 * <p>启动时把 xdb 数据文件整体加载进内存（BufferCache 模式，约 11MB），
 * 查询微秒级；解析结果为纯统计用途，初始化失败不阻断业务（定位降级为未知）。</p>
 */
@Component
public class IpRegionService {

    private static final Logger log = LoggerFactory.getLogger(IpRegionService.class);

    /** classpath 下的数据文件（v2 格式，2025-09 版，来源见 BackEnd-Plan §1） */
    private static final String XDB_CLASSPATH = "ip2region/ip2region.xdb";

    /** 省级行政区划代码（简称 -> 代码，ECharts 地图匹配用） */
    private static final Map<String, String> PROVINCE_CODES = Map.ofEntries(
            Map.entry("北京", "110000"), Map.entry("天津", "120000"), Map.entry("河北", "130000"),
            Map.entry("山西", "140000"), Map.entry("内蒙古", "150000"), Map.entry("辽宁", "210000"),
            Map.entry("吉林", "220000"), Map.entry("黑龙江", "230000"), Map.entry("上海", "310000"),
            Map.entry("江苏", "320000"), Map.entry("浙江", "330000"), Map.entry("安徽", "340000"),
            Map.entry("福建", "350000"), Map.entry("江西", "360000"), Map.entry("山东", "370000"),
            Map.entry("河南", "410000"), Map.entry("湖北", "420000"), Map.entry("湖南", "430000"),
            Map.entry("广东", "440000"), Map.entry("广西", "450000"), Map.entry("海南", "460000"),
            Map.entry("重庆", "500000"), Map.entry("四川", "510000"), Map.entry("贵州", "520000"),
            Map.entry("云南", "530000"), Map.entry("西藏", "540000"), Map.entry("陕西", "610000"),
            Map.entry("甘肃", "620000"), Map.entry("青海", "630000"), Map.entry("宁夏", "640000"),
            Map.entry("新疆", "650000"), Map.entry("香港", "810000"), Map.entry("澳门", "820000"),
            Map.entry("台湾", "710000"));

    private volatile Searcher searcher;

    /**
     * 启动加载数据文件（BufferCache 全量内存模式）
     */
    @PostConstruct
    void init() {
        try (InputStream in = new ClassPathResource(XDB_CLASSPATH).getInputStream()) {
            byte[] cBuff = in.readAllBytes();
            // newWithBuffer 全量内存模式：Searcher 内部自动切分 vector index 与内容区
            searcher = Searcher.newWithBuffer(cBuff);
            log.info("ip2region 数据文件加载完成（{} 字节）", cBuff.length);
        } catch (IOException | RuntimeException e) {
            log.error("ip2region 数据文件加载失败，IP 定位降级为未知（不阻断业务）：{}", e.getMessage());
        }
    }

    /**
     * 解析 IP 地域
     *
     * @param ip 客户端 IP（IPv4）
     * @return 地域信息；解析失败返回 {@link IpRegion#UNKNOWN}
     */
    public IpRegion locate(String ip) {
        Searcher current = searcher;
        if (current == null || ip == null || ip.isBlank() || ip.contains(":")) {
            // v2 数据文件不含 IPv6 段，IPv6 直接降级
            return IpRegion.UNKNOWN;
        }
        try {
            // xdb 返回格式：中国|0|陕西|西安|电信（5 段）
            String[] parts = current.search(ip).split("\\|");
            if (parts.length < 4 || parts[0].isBlank()) {
                return IpRegion.UNKNOWN;
            }
            String country = parts[0];
            String province = parts[2];
            String city = parts[3];
            return new IpRegion(country, province, city, buildRegion(country, province, city),
                    PROVINCE_CODES.getOrDefault(province, null));
        } catch (Exception e) {
            // search(String) 抛受检 Exception（非法 IP 等），定位失败不阻断业务
            log.debug("IP 定位失败（ip={}）：{}", ip, e.getMessage());
            return IpRegion.UNKNOWN;
        }
    }

    /**
     * 省份名 -> 省级行政区划代码（ECharts 地图匹配；日统计 job 聚合时调用）
     *
     * @param province 省份简称（如"陕西"）
     * @return 区划代码；不在映射表内返回 null
     */
    public String provinceCode(String province) {
        return province == null ? null : PROVINCE_CODES.get(province);
    }

    /**
     * 拼接完整地域串（BackEnd-Plan §4.6 降级规则）
     *
     * <p>城市未知（空或与省份相同，直辖市数据如此）时降级为两级：中国·陕西。</p>
     */
    private String buildRegion(String country, String province, String city) {
        StringBuilder region = new StringBuilder(country);
        if (province != null && !province.isBlank() && !"0".equals(province)) {
            region.append('·').append(province);
            if (city != null && !city.isBlank() && !"0".equals(city) && !city.equals(province)) {
                region.append('·').append(city);
            }
        }
        return region.toString();
    }

    /**
     * IP 地域信息
     *
     * @param country      国家
     * @param province     省份（ip2region 简称，如"陕西"；海外为 null）
     * @param city         城市（未知为 null）
     * @param region       完整地域串（中国·陕西·西安）
     * @param provinceCode 省级行政区划代码（ECharts 地图匹配；无法解析为 null）
     */
    public record IpRegion(String country, String province, String city, String region, String provinceCode) {

        /** 未知地域 */
        public static final IpRegion UNKNOWN = new IpRegion(null, null, null, null, null);
    }
}
