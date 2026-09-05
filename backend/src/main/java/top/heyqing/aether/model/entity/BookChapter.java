package top.heyqing.aether.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * 书籍章节实体（BackEnd-Plan §6.2 book_chapter 表，章-节两级结构）
 *
 * <p>level=1 章（parent_id=null）/ level=2 节（parent_id=所属章 ID）；
 * order_no 为全书线性序（阅读器上一章/下一章按此导航）；
 * content 为分段排版 HTML：段落 &lt;p class="indent"&gt;（段首空两格）+
 * 段间半行距（前端 .book-content p+p 样式，§7.2）。</p>
 */
@Entity
@Table(name = "book_chapter")
public class BookChapter extends BaseEntity {

    /** 所属书籍 ID */
    @Column(name = "book_id", nullable = false)
    private Long bookId;

    /** 章节标题 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 层级：1 章 2 节 */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** 父章节 ID（节挂章，章为 null） */
    @Column(name = "parent_id")
    private Long parentId;

    /** 排序号（全书顺序，从 1 递增） */
    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    /** 章节内容（分段排版 HTML） */
    @Lob
    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    /** 字数（段落字符总数，不含 HTML 标签） */
    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }
}
