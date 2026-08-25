package top.heyqing.aether.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.OperationLog;

/**
 * 管理员操作日志表数据访问（BackEnd-Plan §6.2 operation_log）
 *
 * <p>阶段 8 管理端日志页需按模块/操作人/时间过滤查询，届时扩展 Specification。</p>
 */
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
}
