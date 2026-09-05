package top.heyqing.aether.common;

/**
 * 统一错误码（BackEnd-Plan §3.2）
 *
 * <p>分段规则：0 成功；1xxxx 通用错误；2xxxx 认证错误；3xxxx 业务错误；5xxxx 系统错误。
 * 新增错误码必须同步更新 BackEnd-Plan §3.2 错误码表。</p>
 */
public enum ErrorCode {

    // ===== 0 成功 =====
    SUCCESS(0, "success"),

    // ===== 1xxxx 通用错误 =====
    PARAM_ERROR(10001, "参数错误"),
    RESOURCE_NOT_FOUND(10002, "资源不存在"),
    TOO_MANY_REQUESTS(10003, "操作频繁，请稍后再试"),
    VALIDATION_FAILED(10004, "数据校验失败"),
    FILE_TYPE_NOT_SUPPORTED(10005, "文件类型不支持"),
    FILE_SIZE_EXCEEDED(10006, "文件大小超限"),

    // ===== 2xxxx 认证错误 =====
    UNAUTHORIZED(20001, "未认证或登录已失效"),
    TOKEN_EXPIRED(20002, "token 已过期"),
    PASSWORD_ERROR(20003, "密码错误"),
    CAPTCHA_ERROR(20004, "验证码错误或已过期"),
    ACCOUNT_LOCKED(20005, "登录尝试过于频繁，账号已锁定"),
    REFRESH_TOKEN_INVALID(20006, "refresh token 无效"),

    // ===== 3xxxx 业务错误 =====
    // 300xx 文章
    ARTICLE_NOT_FOUND(30001, "文章不存在"),
    CATEGORY_NOT_FOUND(30002, "分类不存在"),
    TAG_NOT_FOUND(30003, "标签不存在"),
    STYLE_NOT_FOUND(30004, "样式不存在"),
    // 301xx 图集
    ALBUM_NOT_FOUND(30101, "图集不存在"),
    // 302xx 视频
    VIDEO_NOT_FOUND(30201, "视频不存在"),
    // 303xx 音乐
    MUSIC_NOT_FOUND(30301, "音乐不存在"),
    MUSIC_ALBUM_NOT_FOUND(30302, "音乐合集不存在"),
    // 304xx 书籍
    SPLIT_TASK_NOT_FOUND(30401, "分章任务不存在"),
    BOOK_NOT_FOUND(30402, "书籍不存在"),
    BOOK_CHAPTER_NOT_FOUND(30403, "章节不存在"),
    SPLIT_TASK_STATE_INVALID(30404, "分章任务状态不符，请刷新后重试"),
    BOOK_SOURCE_MISSING(30405, "书籍尚未上传 txt 源文件，无法分章"),
    // 305xx 存储
    CHUNK_MISSING(30501, "分片缺失，请先上传缺失分片"),
    UPLOAD_SESSION_NOT_FOUND(30502, "上传会话不存在或已过期"),
    FILE_VERIFY_FAILED(30503, "文件校验失败，请重新上传"),
    // 306xx 公告
    ANNOUNCEMENT_NOT_FOUND(30601, "公告不存在"),
    // 307xx 订阅
    EMAIL_FORMAT_ERROR(30701, "邮箱格式错误"),
    SURVEY_SUBMITTED(30702, "该 IP 问卷已提交"),
    SURVEY_UPDATE_LIMIT(30703, "问卷修改次数已用完"),
    // 308xx AI
    AI_UNAVAILABLE(30801, "AI 服务不可用"),
    AI_GENERATE_FAILED(30802, "AI 生成失败"),
    AI_DAILY_LIMIT(30803, "当日使用次数已达上限"),

    // ===== 5xxxx 系统错误 =====
    SYSTEM_ERROR(50001, "系统内部错误"),
    STORAGE_ERROR(50002, "存储服务异常"),
    AI_ERROR(50003, "AI 服务异常"),
    DB_ERROR(50004, "数据库异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 按错误码反查枚举（Security 过滤器等无法抛异常的链路使用）
     *
     * @param code 错误码
     * @return 对应枚举；未知错误码回退 50001
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }
}
