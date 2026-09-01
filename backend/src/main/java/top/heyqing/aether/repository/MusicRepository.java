package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import top.heyqing.aether.model.entity.Music;

/**
 * 音乐单曲仓储（BackEnd-Plan §6.2 music）
 *
 * <p>公开搜索（keyword/albumId）走 Specification 动态条件；
 * 合集曲目数统计用 group by 批量聚合（避免列表页 N+1）。</p>
 */
public interface MusicRepository extends JpaRepository<Music, Long>, JpaSpecificationExecutor<Music> {

    /**
     * 合集详情曲目列表（按 id 升序=入库顺序）
     */
    List<Music> findByAlbumIdOrderByIdAsc(Long albumId);

    /**
     * 批量统计各合集曲目数（合集列表页 trackCount 用）
     *
     * @param albumIds 合集 ID 集合
     * @return [albumId, trackCount] 行集
     */
    @Query("select m.albumId, count(m.id) from Music m where m.albumId in :albumIds group by m.albumId")
    List<Object[]> countGroupByAlbumId(@Param("albumIds") List<Long> albumIds);

    /**
     * 合集内曲目数（删除合集前防呆校验）
     */
    long countByAlbumId(Long albumId);
}
