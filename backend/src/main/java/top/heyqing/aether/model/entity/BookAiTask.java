package top.heyqing.aether.model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 书籍 AI 分章任务实体（BackEnd-Plan §6.2 book_ai_task 表，状态机）
 *
 * <p>状态流转：1 解析中 → 2 待确认 →（确认）3 已完成；任意非 3 状态失败 → 4（可重试）。
 * ai_result 为分章建议 JSON：{"chapters":[{title,level,paragraphs[]}],"source":"agent|heuristic"}；
 * 每本书同时至多一个进行中任务（trigger 幂等控制，§7.2）。</p>
 */
@Entity
@Table(name = "book_ai_task")
public class BookAiTask extends BaseEntity {

    /** 状态：解析中 */
    public static final int STATUS_PARSING = 1;

    /** 状态：待确认 */
    public static final int STATUS_PENDING_CONFIRM = 2;

    /** 状态：已完成 */
    public static final int STATUS_CONFIRMED = 3;

    /** 状态：失败 */
    public static final int STATUS_FAILED = 4;

    /** 所属书籍 ID */
    @Column(name = "book_id", nullable = false)
    private Long bookId;

    /** 状态：1 解析中 2 待确认 3 已完成 4 失败 */
    @Column(name = "status", nullable = false)
    private Integer status = STATUS_PARSING;

    /** AI 分章建议 JSON（章节标题+层级+段落，§7.2） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_result")
    private String aiResult;

    /** 失败原因（status=4 时） */
    @Column(name = "fail_reason", length = 500)
    private String failReason;

    /** 用户确认时间（status=3 时） */
    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAiResult() {
        return aiResult;
    }

    public void setAiResult(String aiResult) {
        this.aiResult = aiResult;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public LocalDateTime getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }
}
