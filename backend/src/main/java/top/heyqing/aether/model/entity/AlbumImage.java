package top.heyqing.aether.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * 图集图片实体（BackEnd-Plan §6.2 album_image 表）
 *
 * <p>图片文件本体在 storage_file（宽高/大小等参数随文件元数据），
 * 本表仅维护图集内的展示信息（标题/介绍/排序）。</p>
 */
@Entity
@Table(name = "album_image", indexes = @Index(name = "idx_album", columnList = "album_id"))
public class AlbumImage extends BaseEntity {

    /** 所属图集 ID */
    @Column(name = "album_id", nullable = false)
    private Long albumId;

    /** 图片文件 ID（宽高大小等参数存 storage_file） */
    @Column(name = "file_id", nullable = false)
    private Long fileId;

    /** 图片标题（可选） */
    @Column(name = "title", length = 200)
    private String title;

    /** 图片介绍（可选） */
    @Column(name = "intro", length = 500)
    private String intro;

    /** 排序号 */
    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
