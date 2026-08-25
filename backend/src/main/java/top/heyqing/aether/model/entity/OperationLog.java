package top.heyqing.aether.model.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 管理员操作日志表（BackEnd-Plan §6.2 operation_log / §10 日志模块）
 */
@Entity
@Table(name = "operation_log")
public class OperationLog extends BaseEntity {

    /** 操作人 */
    @Column(name = "operator", length = 50)
    private String operator;

    /** 模块 */
    @Column(name = "module", length = 50)
    private String module;

    /** 操作类型（新增/修改/删除/登录/上传/生成…） */
    @Column(name = "action", length = 50)
    private String action;

    /** 请求方法 */
    @Column(name = "method", length = 200)
    private String method;

    /** 请求路径 */
    @Column(name = "path")
    private String path;

    /** 请求参数（脱敏后：password/token 等字段替换为 ***，JSON） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params")
    private String params;

    /** 结果：1 成功 0 失败 */
    @Column(name = "result")
    private Integer result;

    /** 操作 IP */
    @Column(name = "ip", length = 45)
    private String ip;

    /** 耗时（毫秒） */
    @Column(name = "cost_ms")
    private Integer costMs;

    /** 失败原因 */
    @Column(name = "error_msg", length = 500)
    private String errorMsg;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getCostMs() {
        return costMs;
    }

    public void setCostMs(Integer costMs) {
        this.costMs = costMs;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
