package top.heyqing.aether.util;

/**
 * User-Agent 解析工具（BackEnd-Plan §4.6 游客日志：设备/浏览器/系统）
 *
 * <p>纯字符串匹配实现，不引第三方库；统计粒度到设备类型 + 浏览器大类 + 系统大类即可。</p>
 */
public final class UaParser {

    private UaParser() {
    }

    /**
     * 解析 UA
     *
     * @param userAgent 原始 User-Agent（可空）
     * @return 解析结果；空 UA 返回未知
     */
    public static UaInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new UaInfo("Unknown", "Unknown", "Unknown");
        }
        String ua = userAgent;
        return new UaInfo(deviceType(ua), browser(ua), os(ua));
    }

    /** 设备类型：Tablet 优先（iPad 的 UA 同时含 Mobile 与 iPad），再 Mobile，兜底 PC */
    private static String deviceType(String ua) {
        if (ua.contains("iPad") || ua.contains("Tablet")) {
            return "Tablet";
        }
        if (ua.contains("Mobile") || ua.contains("Android") || ua.contains("iPhone")
                || ua.contains("iPod") || ua.contains("Windows Phone")) {
            return "Mobile";
        }
        return "PC";
    }

    /** 浏览器大类（按主流识别顺序，避免 Chrome 前缀误判） */
    private static String browser(String ua) {
        if (ua.contains("MicroMessenger")) {
            return "WeChat";
        }
        if (ua.contains("QQBrowser")) {
            return "QQ Browser";
        }
        if (ua.contains("Edg/")) {
            return "Edge";
        }
        if (ua.contains("OPR/") || ua.contains("Opera")) {
            return "Opera";
        }
        if (ua.contains("Firefox/")) {
            return "Firefox";
        }
        // Safari 排在 Chrome 之后：Chrome 的 UA 也包含 "Safari" 字样
        if (ua.contains("Chrome/")) {
            return "Chrome";
        }
        if (ua.contains("Safari/")) {
            return "Safari";
        }
        if (ua.contains("360SE")) {
            return "360 Browser";
        }
        return "Unknown";
    }

    /** 操作系统大类 */
    private static String os(String ua) {
        if (ua.contains("Windows NT 10.0")) {
            return "Windows 10/11";
        }
        if (ua.contains("Windows NT 6.1")) {
            return "Windows 7";
        }
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("iPhone")) {
            return "iOS";
        }
        if (ua.contains("iPad")) {
            return "iPadOS";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "macOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return "Unknown";
    }

    /**
     * UA 解析结果
     */
    public record UaInfo(String deviceType, String browser, String os) {
    }
}
