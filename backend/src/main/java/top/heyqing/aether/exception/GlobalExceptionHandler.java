package top.heyqing.aether.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.RequestContext;
import top.heyqing.aether.common.Result;

/**
 * 全局异常处理器（BackEnd-Plan §3.3）
 *
 * <p>统一捕获业务异常、参数校验异常与兜底异常，转换为 {@link Result}。
 * 对外不泄露内部细节（堆栈仅记日志），HTTP 状态码统一 200、业务结果看 body.code。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：携带明确错误码
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.fail(e.getErrorCode(), e.getMessage());
    }

    /**
     * DTO 参数校验失败（Jakarta Validation），返回 10001 + 字段级错误详情
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("参数校验失败: {}", fieldErrors);
        return Result.failWithData(ErrorCode.PARAM_ERROR, fieldErrors);
    }

    /**
     * 缺少必填请求参数 / 请求体不可读（JSON 格式错误）
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("请求参数错误: {}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_ERROR);
    }

    /**
     * 请求体超过上传大小限制（nginx/Spring 均有限制）
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        log.warn("上传大小超限: {}", e.getMessage());
        return Result.fail(ErrorCode.FILE_SIZE_EXCEEDED);
    }

    /**
     * 资源不存在（404）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.fail(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 兜底异常：记录完整堆栈，对外只返回 50001
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("系统异常: requestId={}", RequestContext.getRequestId(), e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
