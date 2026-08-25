package top.heyqing.aether.model.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 分类表（全站共用，biz_type 区分业务域，BackEnd-Plan §6.2 category）
 */
@Entity
@Table(name = "category", uniqueConstraints = @UniqueConstraint(name = "uk_category_biz_slug",
        columnNames = {"biz_type", "slug"}))
public class Category extends BaseEntity {

    /** 分类名称 */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 唯一标识（URL 用） */
    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    /** 业务域：article/book/music/album/video/announcement */
    @Column(name = "biz_type", nullable = false, length = 20)
    private String bizType;

    /** 排序号 */
    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
