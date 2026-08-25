package top.heyqing.aether.model.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 问卷选项字典表（BackEnd-Plan §6.2 survey_option，seed 全量生成）
 */
@Entity
@Table(name = "survey_option")
public class SurveyOption extends BaseEntity {

    /** 字段名：age_range/gender/occupation/interests */
    @Column(name = "field", nullable = false, length = 30)
    private String field;

    /** 选项文本 */
    @Column(name = "label", nullable = false, length = 50)
    private String label;

    /** 排序号 */
    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
