package top.heyqing.aether.model.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 文章独立样式表（BackEnd-Plan §6.2 article_style）
 *
 * <p>样式 JSON 存 CSS 变量集 + 排版参数（结构见 BackEnd-Plan §6.3），
 * 可被多篇文章复用；is_default=1 的样式为全站默认样式。</p>
 */
@Entity
@Table(name = "article_style")
public class ArticleStyle extends BaseEntity {

    /** 样式名称 */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 样式配置 JSON（字体/字号/行高/段间距/首行缩进/主题色等） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "style_json", nullable = false)
    private String styleJson;

    /** 是否默认样式：1 是 0 否 */
    @Column(name = "is_default", nullable = false)
    private Integer isDefault = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStyleJson() {
        return styleJson;
    }

    public void setStyleJson(String styleJson) {
        this.styleJson = styleJson;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }
}
