package top.heyqing.aether.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 业务-分类关联表（BackEnd-Plan §6.2 biz_category_rel，多对多）
 *
 * <p>仅关联无时间戳，独立主键实体。</p>
 */
@Entity
@Table(name = "biz_category_rel",
        uniqueConstraints = @UniqueConstraint(name = "uk_biz_category_rel",
                columnNames = {"biz_type", "biz_id", "category_id"}))
public class BizCategoryRel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 业务域（与 category.biz_type 对应） */
    @Column(name = "biz_type", nullable = false, length = 20)
    private String bizType;

    /** 业务记录 ID */
    @Column(name = "biz_id", nullable = false)
    private Long bizId;

    /** 分类 ID */
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
