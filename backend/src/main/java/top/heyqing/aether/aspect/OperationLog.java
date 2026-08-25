package top.heyqing.aether.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（BackEnd-Plan §10）
 *
 * <p>标注在管理端写操作 Controller 方法上，由 {@link OperationLogAspect} 切面
 * 记录到 operation_log 表（参数脱敏）。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 模块：认证/文章/图集/视频/音乐/书籍/公告/订阅/存储/AI/系统 */
    String module();

    /** 操作类型：登录/登出/新增/修改/删除/上传/生成/发布/下线… */
    String action();
}
