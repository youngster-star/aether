package top.heyqing.aether.model.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * 音乐单曲实体（BackEnd-Plan §6.2 music 表）
 *
 * <p>歌词支持 LRC 时间轴与纯文本两种（对齐策略见 §7.6，lyric_offset 全局偏移毫秒）；
 * effect_config 为 AI 特效配置（EffectConfig Schema v1，§7.3），落库前必须通过
 * Schema 校验（防 AI 幻觉字段）；时长在上传合并后自动探测（§8.2）。</p>
 */
@Entity
@Table(name = "music")
public class Music extends BaseEntity {

    /** 歌曲名称 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 歌手 */
    @Column(name = "artist", length = 100)
    private String artist;

    /** 所属合集 ID（可空=独立单曲） */
    @Column(name = "album_id")
    private Long albumId;

    /** 封面文件 ID（storage_file，可空；特效调色板提取源） */
    @Column(name = "cover_file_id")
    private Long coverFileId;

    /** 音频文件 ID（storage_file） */
    @Column(name = "file_id", nullable = false)
    private Long fileId;

    /** 歌词文本（LRC 带时间轴 / 纯文本） */
    @Lob
    @Column(name = "lyric_text", columnDefinition = "TEXT")
    private String lyricText;

    /** 歌词全局偏移（毫秒，正负可调，解决歌词整体快/慢） */
    @Column(name = "lyric_offset", nullable = false)
    private Integer lyricOffset = 0;

    /** 时长（秒，上传后自动探测；探测失败管理端手工补录） */
    @Column(name = "duration", nullable = false)
    private Integer duration = 0;

    /** AI 特效配置（EffectConfig JSON，Schema v1，§7.3） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "effect_config")
    private String effectConfig;

    /** 特效来源：1 生成（AI/规则） 2 手工调整 */
    @Column(name = "effect_source", nullable = false)
    private Integer effectSource = 1;

    /** 是否推荐：1 是 0 否 */
    @Column(name = "is_recommend", nullable = false)
    private Integer isRecommend = 0;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public Long getCoverFileId() {
        return coverFileId;
    }

    public void setCoverFileId(Long coverFileId) {
        this.coverFileId = coverFileId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getLyricText() {
        return lyricText;
    }

    public void setLyricText(String lyricText) {
        this.lyricText = lyricText;
    }

    public Integer getLyricOffset() {
        return lyricOffset;
    }

    public void setLyricOffset(Integer lyricOffset) {
        this.lyricOffset = lyricOffset;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getEffectConfig() {
        return effectConfig;
    }

    public void setEffectConfig(String effectConfig) {
        this.effectConfig = effectConfig;
    }

    public Integer getEffectSource() {
        return effectSource;
    }

    public void setEffectSource(Integer effectSource) {
        this.effectSource = effectSource;
    }

    public Integer getIsRecommend() {
        return isRecommend;
    }

    public void setIsRecommend(Integer isRecommend) {
        this.isRecommend = isRecommend;
    }
}
