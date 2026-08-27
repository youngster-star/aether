package top.heyqing.aether.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 游客访问日志表（BackEnd-Plan §6.2 visitor_log）
 *
 * <p>仅写入不更新，无 create_time/update_time，独立主键实体（不继承 BaseEntity）。
 * ip2region 解析结果与 UA 解析结果在拦截器写入。</p>
 */
@Entity
@Table(name = "visitor_log")
public class VisitorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 访问 IP（IPv4/IPv6） */
    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    /** 国家（ip2region 解析） */
    @Column(name = "country", length = 50)
    private String country;

    /** 省份（ip2region 解析） */
    @Column(name = "province", length = 50)
    private String province;

    /** 城市（ip2region 解析） */
    @Column(name = "city", length = 50)
    private String city;

    /** 完整地域串，如：中国·陕西·西安 */
    @Column(name = "region", length = 100)
    private String region;

    /** 原始 User-Agent */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** 设备类型：PC/Mobile/Tablet */
    @Column(name = "device_type", length = 20)
    private String deviceType;

    /** 浏览器名称 */
    @Column(name = "browser", length = 50)
    private String browser;

    /** 操作系统 */
    @Column(name = "os", length = 50)
    private String os;

    /** 访问路径 */
    @Column(name = "visit_path", length = 255)
    private String visitPath;

    /** 来源页面 */
    @Column(name = "referer", length = 500)
    private String referer;

    /** 访问时间 */
    @Column(name = "visit_time", nullable = false)
    private LocalDateTime visitTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getVisitPath() {
        return visitPath;
    }

    public void setVisitPath(String visitPath) {
        this.visitPath = visitPath;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public LocalDateTime getVisitTime() {
        return visitTime;
    }

    public void setVisitTime(LocalDateTime visitTime) {
        this.visitTime = visitTime;
    }
}
