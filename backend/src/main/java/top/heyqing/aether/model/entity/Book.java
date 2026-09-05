package top.heyqing.aether.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 书籍实体（BackEnd-Plan §6.2 book 表，含出版图书与本人作品）
 *
 * <p>ownership_type 决定阅读器第二页版权声明文案（UI-Plan §6.7）；
 * source_file_id 指向 txt 源文件（分章依据，§7.2）；
 * total_chapters 仅统计 level=1 章（节挂章不计数）。</p>
 */
@Entity
@Table(name = "book")
public class Book extends BaseEntity {

    /** 书名 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 作者 */
    @Column(name = "author", length = 100)
    private String author;

    /** 封面文件 ID（storage_file，可空） */
    @Column(name = "cover_file_id")
    private Long coverFileId;

    /** 简介 */
    @Column(name = "intro", length = 1000)
    private String intro;

    /** 版权归属：1 本人 2 他人出版 */
    @Column(name = "ownership_type", nullable = false)
    private Integer ownershipType = 1;

    /** 源文件 ID（txt，分章依据，可空=手动创建章节） */
    @Column(name = "source_file_id")
    private Long sourceFileId;

    /** 总章节数（level=1 章） */
    @Column(name = "total_chapters", nullable = false)
    private Integer totalChapters = 0;

    /** 是否推荐：1 是 0 否 */
    @Column(name = "is_recommend", nullable = false)
    private Integer isRecommend = 0;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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

    public Integer getOwnershipType() {
        return ownershipType;
    }

    public void setOwnershipType(Integer ownershipType) {
        this.ownershipType = ownershipType;
    }

    public Long getSourceFileId() {
        return sourceFileId;
    }

    public void setSourceFileId(Long sourceFileId) {
        this.sourceFileId = sourceFileId;
    }

    public Integer getTotalChapters() {
        return totalChapters;
    }

    public void setTotalChapters(Integer totalChapters) {
        this.totalChapters = totalChapters;
    }

    public Integer getIsRecommend() {
        return isRecommend;
    }

    public void setIsRecommend(Integer isRecommend) {
        this.isRecommend = isRecommend;
    }
}
