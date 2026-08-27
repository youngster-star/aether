package top.heyqing.aether.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * 文章表（BackEnd-Plan §6.2 article）
 *
 * <p>博客主体：内容 HTML+MD 双存（编辑回显用 MD，展示用 sanitize 后 HTML）、
 * 独立样式（article_style_id 可空=默认样式）、自定义热度（hot_order）。</p>
 */
@Entity
@Table(name = "article")
public class Article extends BaseEntity {

    /** 文章标题 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 封面文件 ID（storage_file.id） */
    @Column(name = "cover_file_id")
    private Long coverFileId;

    /** 文章简介 */
    @Column(name = "summary", length = 500)
    private String summary;

    /** HTML 内容（jsoup sanitize 后） */
    @Lob
    @Column(name = "content_html", nullable = false)
    private String contentHtml;

    /** Markdown 源内容（管理端编辑回显用） */
    @Lob
    @Column(name = "content_md", nullable = false)
    private String contentMd;

    /** 字数 */
    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    /** 阅读次数（IP 24 小时去重） */
    @Column(name = "reading_count", nullable = false)
    private Integer readingCount = 0;

    /** 是否热门：1 是 0 否 */
    @Column(name = "is_hot", nullable = false)
    private Integer isHot = 0;

    /** 热度排序（越大越靠前） */
    @Column(name = "hot_order", nullable = false)
    private Integer hotOrder = 0;

    /** 文章样式 ID（NULL=默认样式） */
    @Column(name = "article_style_id")
    private Long articleStyleId;

    /** 是否发布：1 已发布 0 草稿 */
    @Column(name = "is_published", nullable = false)
    private Integer isPublished = 0;

    /** 发布时间 */
    @Column(name = "publish_time")
    private LocalDateTime publishTime;

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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public String getContentMd() {
        return contentMd;
    }

    public void setContentMd(String contentMd) {
        this.contentMd = contentMd;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public Integer getReadingCount() {
        return readingCount;
    }

    public void setReadingCount(Integer readingCount) {
        this.readingCount = readingCount;
    }

    public Integer getIsHot() {
        return isHot;
    }

    public void setIsHot(Integer isHot) {
        this.isHot = isHot;
    }

    public Integer getHotOrder() {
        return hotOrder;
    }

    public void setHotOrder(Integer hotOrder) {
        this.hotOrder = hotOrder;
    }

    public Long getArticleStyleId() {
        return articleStyleId;
    }

    public void setArticleStyleId(Long articleStyleId) {
        this.articleStyleId = articleStyleId;
    }

    public Integer getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Integer isPublished) {
        this.isPublished = isPublished;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }
}
