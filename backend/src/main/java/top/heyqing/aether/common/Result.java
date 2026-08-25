package top.heyqing.aether.common;

/**
 * 统一返回体（BackEnd-Plan §3.1）
 *
 * <p>约定：HTTP 状态码统一 200，业务结果以 {@code code} 区分（0 成功，非 0 失败），
 * 前端按 code 处理，便于全局拦截器统一提示。</p>
 *
 * @param <T> 数据类型
 */
public record Result<T>(int code, String message, T data, String requestId, long timestamp) {

    /**
     * 成功（无数据）
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 成功（带数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data,
                RequestContext.getRequestId(), System.currentTimeMillis());
    }

    /**
     * 失败（错误码 + 默认消息）
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null,
                RequestContext.getRequestId(), System.currentTimeMillis());
    }

    /**
     * 失败（错误码 + 自定义消息，用于需要补充上下文的场景）
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null,
                RequestContext.getRequestId(), System.currentTimeMillis());
    }

    /**
     * 失败（错误码默认消息 + 详情数据，如参数校验的字段级错误清单）
     */
    public static <T> Result<T> failWithData(ErrorCode errorCode, T data) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), data,
                RequestContext.getRequestId(), System.currentTimeMillis());
    }
}
