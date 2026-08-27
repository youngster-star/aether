package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.VideoChapter;

/**
 * 视频关键时间节点数据访问（BackEnd-Plan §6.2 video_chapter 表）
 */
public interface VideoChapterRepository extends JpaRepository<VideoChapter, Long> {

    /**
     * 视频节点列表（排序号升序，同号先入先出）
     */
    List<VideoChapter> findByVideoIdOrderBySortAscIdAsc(Long videoId);

    /**
     * 批量查询视频节点（管理端列表一次取齐，避免 N+1）
     */
    List<VideoChapter> findByVideoIdInOrderByVideoIdAscSortAscIdAsc(List<Long> videoIds);

    /**
     * 删除视频全部节点（视频删除时级联清理）
     */
    void deleteByVideoId(Long videoId);
}
