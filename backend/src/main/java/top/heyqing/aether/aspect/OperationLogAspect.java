package top.heyqing.aether.aspect;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import top.heyqing.aether.common.RequestContext;
import top.heyqing.aether.model.entity.OperationLog;
import top.heyqing.aether.repository.OperationLogRepository;

/**
 * 操作日志切面（BackEnd-Plan §10）
 *
 * <p>拦截标注 {@link OperationLog} 的 Controller 方法，记录模块/操作/路径/参数
 * （敏感字段脱敏）/结果/耗时/IP 到 operation_log 表。日志记录失败不阻断业务。</p>
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    /** 脱敏字段名（参数 JSON 中这些字段值替换为 ***） */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "captchaCode", "token", "accessToken", "refreshToken", "secret", "apiKey");

    /** 失败原因最大长度（与表字段 error_msg VARCHAR(500) 对齐） */
    private static final int MAX_ERROR_MSG_LENGTH = 500;

    private final OperationLogRepository operationLogRepository;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(OperationLogRepository operationLogRepository, ObjectMapper objectMapper) {
        this.operationLogRepository = operationLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 环绕通知：无论业务成功或异常都落库
     *
     * <p>注意：注解类型用全限定名引用——本文件 import 了同名 entity
     * （model.entity.OperationLog），显式 import 优先级高于同包类型，
     * 不限定会解析成实体类。</p>
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint,
                         top.heyqing.aether.aspect.OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            save(joinPoint, operationLog, true, null, start);
            return result;
        } catch (Throwable throwable) {
            save(joinPoint, operationLog, false, throwable.getMessage(), start);
            throw throwable;
        }
    }

    private void save(ProceedingJoinPoint joinPoint, top.heyqing.aether.aspect.OperationLog operationLog,
                      boolean success, String errorMsg, long start) {
        try {
            OperationLog entity = new OperationLog();
            // 单管理员体系，操作人固定 cryptex（阶段 8 可扩展为登录用户）
            entity.setOperator("cryptex");
            entity.setModule(operationLog.module());
            entity.setAction(operationLog.action());
            entity.setMethod(joinPoint.getSignature().toShortString());
            entity.setPath(currentPath());
            entity.setParams(sanitize(joinPoint.getArgs()));
            entity.setResult(success ? 1 : 0);
            entity.setIp(RequestContext.getClientIp());
            entity.setCostMs((int) (System.currentTimeMillis() - start));
            entity.setErrorMsg(errorMsg == null ? null
                    : errorMsg.substring(0, Math.min(errorMsg.length(), MAX_ERROR_MSG_LENGTH)));
            operationLogRepository.save(entity);
        } catch (Exception e) {
            // 日志记录失败不影响业务主流程
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
    }

    /**
     * 序列化请求参数并脱敏敏感字段（序列化失败降级为空对象，如 MultipartFile 参数）
     */
    private String sanitize(Object[] args) {
        try {
            JsonNode node = objectMapper.valueToTree(args);
            maskSensitive(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 递归脱敏：对象字段名命中敏感集合则值替换为 ***
     */
    private void maskSensitive(JsonNode node) {
        if (node.isArray()) {
            node.forEach(this::maskSensitive);
        } else if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            // Jackson 3：fields() 已废弃，使用 properties()
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_FIELDS.contains(field.getKey()) && !field.getValue().isNull()) {
                    objectNode.put(field.getKey(), "***");
                } else {
                    maskSensitive(field.getValue());
                }
            }
        }
    }

    /**
     * 当前请求路径（非 Web 上下文返回 unknown）
     */
    private String currentPath() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRequestURI();
        }
        return "unknown";
    }
}
