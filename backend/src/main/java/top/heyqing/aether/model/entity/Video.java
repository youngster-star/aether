package top.heyqing.aether.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 视频实体（BackEnd-Plan §6.2 video 表）
 *
 * <p>时长在上传合并后自动探测（ffprobe 优先、MP4 内置解析回退，见 §8.2），
 * 探测失败时管理端可手工补录；关键时间节点见 {@link VideoChapter}。</p>
 */
@Entity
@Table(name = "video")
public class Video extends BaseEntity {

    /** 视频标题 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 封面文件 ID（storage_file，可空） */
    @Column(name = "cover_file_id")
    private Long coverFileId;

    /** 视频介绍 */
    @Column(name = "intro", length = 500)
    private String intro;

    /** 视频文件 ID（storage_file） */
    @Column(name = "file_id", nullable = false)
    private Long fileId;

    /** 时长（秒，上传合并后自动探测） */
    @Column(name = "duration", nullable = false)
    private Integer duration = 0;

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

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getIsRecommend() {
        return isRecommend;
    }

    public void setIsRecommend(Integer isRecommend) {
        this.isRecommend = isRecommend;
    }
}
