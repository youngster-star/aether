package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import top.heyqing.aether.model.entity.AlbumImage;

/**
 * 图集图片数据访问（BackEnd-Plan §6.2 album_image 表）
 */
public interface AlbumImageRepository extends JpaRepository<AlbumImage, Long> {

    /**
     * 图集图片列表（排序号升序，同号先入先出）
     */
    List<AlbumImage> findByAlbumIdOrderBySortAscIdAsc(Long albumId);

    /**
     * 批量统计图集图片数（列表 VO 的 imageCount 一次查询取齐，避免 N+1）
     *
     * @return 每行 [albumId, count]
     */
    @Query("select a.albumId, count(a) from AlbumImage a where a.albumId in :albumIds group by a.albumId")
    List<Object[]> countGroupByAlbumId(@Param("albumIds") List<Long> albumIds);

    /**
     * 删除图集全部图片（图集删除时级联清理）
     */
    void deleteByAlbumId(Long albumId);
}
