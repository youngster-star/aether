package top.heyqing.aether.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * 视频关键时间节点实体（BackEnd-Plan §6.2 video_chapter 表）
 *
 * <p>播放器右侧章节列表：点击跳转到 time_offset 秒处（ArtPlayer 集成点）。</p>
 */
@Entity
@Table(name = "video_chapter", indexes = @Index(name = "idx_video", columnList = "video_id"))
public class VideoChapter extends BaseEntity {

    /** 所属视频 ID */
    @Column(name = "video_id", nullable = false)
    private Long videoId;

    /** 节点标题 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 时间偏移（秒） */
    @Column(name = "time_offset", nullable = false)
    private Integer timeOffset;

    /** 排序号 */
    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTimeOffset() {
        return timeOffset;
    }

    public void setTimeOffset(Integer timeOffset) {
        this.timeOffset = timeOffset;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
