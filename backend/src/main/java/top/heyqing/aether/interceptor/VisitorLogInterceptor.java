package top.heyqing.aether.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.heyqing.aether.service.visitor.VisitorService;
import top.heyqing.aether.util.IpUtil;

/**
 * 游客访问记录拦截器（BackEnd-Plan §4.6）
 *
 * <p>仅记录公开内容的 GET 浏览（页面/接口），跳过：
 * 管理端请求、认证接口、媒体文件加载（页面内嵌会放大 PV）、健康检查与调试端点。
 * 统计失败静默降级，绝不影响业务请求。</p>
 */
@Component
public class VisitorLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(VisitorLogInterceptor.class);

    private final VisitorService visitorService;

    public VisitorLogInterceptor(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (skip(path)) {
            return true;
        }
        try {
            visitorService.recordVisit(IpUtil.clientIp(request), request.getHeader("User-Agent"),
                    path, request.getHeader("Referer"));
        } catch (RuntimeException e) {
            log.debug("游客记录拦截器异常（已忽略）：{}", e.getMessage());
        }
        return true;
    }

    /**
     * 不记录的路径：管理端/认证/媒体文件（页面内嵌请求不重复计 PV）/调试端点/静态资源
     */
    private boolean skip(String path) {
        return path.startsWith("/v1/admin")
                || path.startsWith("/v1/auth")
                || path.startsWith("/v1/storage")
                || path.equals("/health")
                || path.contains("h2-console")
                || path.contains("swagger")
                || path.contains("api-docs")
                || path.startsWith("/actuator")
                || path.contains(".");
    }
}
