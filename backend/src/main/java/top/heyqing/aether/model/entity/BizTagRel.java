package top.heyqing.aether.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 业务-标签关联表（BackEnd-Plan §6.2 biz_tag_rel，多对多）
 *
 * <p>仅关联无时间戳，独立主键实体。</p>
 */
@Entity
@Table(name = "biz_tag_rel",
        uniqueConstraints = @UniqueConstraint(name = "uk_biz_tag_rel",
                columnNames = {"biz_type", "biz_id", "tag_id"}))
public class BizTagRel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 业务域 */
    @Column(name = "biz_type", nullable = false, length = 20)
    private String bizType;

    /** 业务记录 ID */
    @Column(name = "biz_id", nullable = false)
    private Long bizId;

    /** 标签 ID */
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
