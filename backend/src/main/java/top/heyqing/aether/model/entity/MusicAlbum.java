package top.heyqing.aether.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 音乐合集实体（BackEnd-Plan §6.2 music_album 表）
 *
 * <p>合集分两类：自定义合集（站长策划）与固定合集/专辑（type=2，认证信息必填）；
 * 曲目明细见 {@link Music}（album_id 关联，可为独立单曲）。</p>
 */
@Entity
@Table(name = "music_album")
public class MusicAlbum extends BaseEntity {

    /** 合集标题 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 封面文件 ID（storage_file，可空=无封面） */
    @Column(name = "cover_file_id")
    private Long coverFileId;

    /** 合集介绍 */
    @Column(name = "intro", length = 500)
    private String intro;

    /** 类型：1 自定义合集 2 固定合集（专辑） */
    @Column(name = "type", nullable = false)
    private Integer type = 1;

    /** 认证信息（固定合集必填，如发行方/认证编号） */
    @Column(name = "certification", length = 200)
    private String certification;

    /** 是否推荐：1 是 0 否 */
    @Column(name = "is_recommend", nullable = false)
    private Integer isRecommend = 0;

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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getCertification() {
        return certification;
    }

    public void setCertification(String certification) {
        this.certification = certification;
    }

    public Integer getIsRecommend() {
        return isRecommend;
    }

    public void setIsRecommend(Integer isRecommend) {
        this.isRecommend = isRecommend;
    }
}
