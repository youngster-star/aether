package top.heyqing.aether.exception;

import top.heyqing.aether.common.ErrorCode;

/**
 * 业务异常：携带错误码，由全局异常处理器统一转换为 Result（BackEnd-Plan §3.3）
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义消息覆盖错误码默认消息（错误码不变，仅提示文案不同）
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
