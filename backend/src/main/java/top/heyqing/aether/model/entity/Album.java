package top.heyqing.aether.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 图集实体（BackEnd-Plan §6.2 album 表）
 *
 * <p>图集为图片集合的展示单元：封面 + 介绍 + 推荐位；图片明细见 {@link AlbumImage}。</p>
 */
@Entity
@Table(name = "album")
public class Album extends BaseEntity {

    /** 图集标题 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 封面文件 ID（storage_file，可空=无封面） */
    @Column(name = "cover_file_id")
    private Long coverFileId;

    /** 图集介绍 */
    @Column(name = "intro", length = 500)
    private String intro;

    /** 是否推荐：1 是 0 否 */
    @Column(name = "is_recommend", nullable = false)
    private Integer isRecommend = 0;

    /** 推荐排序号（值大优先） */
    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getCoverFileId() {
        return coverFileId;
    }

    public void setCoverFileId(Long coverFileId) {
        this.coverFileId = coverFileId;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public Integer getIsRecommend() {
        return isRecommend;
    }

    public void setIsRecommend(Integer isRecommend) {
        this.isRecommend = isRecommend;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
