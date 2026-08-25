package top.heyqing.aether.common;

/**
 * 请求上下文（ThreadLocal）：贯穿当前请求的 requestId 与客户端 IP。
 *
 * <p>由 {@link RequestIdFilter} 在请求入口写入、请求结束清理；
 * 供 Result 填充 requestId、日志 MDC、@OperationLog 切面取操作 IP 使用。</p>
 */
public final class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CLIENT_IP = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(String requestId, String clientIp) {
        REQUEST_ID.set(requestId);
        CLIENT_IP.set(clientIp);
    }

    public static void clear() {
        REQUEST_ID.remove();
        CLIENT_IP.remove();
    }

    /**
     * 获取请求链路 ID（异常兜底返回 unknown，保证序列化不失败）
     */
    public static String getRequestId() {
        String id = REQUEST_ID.get();
        return id == null ? "unknown" : id;
    }

    /**
     * 获取客户端 IP（未初始化时返回 unknown）
     */
    public static String getClientIp() {
        String ip = CLIENT_IP.get();
        return ip == null ? "unknown" : ip;
    }
}
