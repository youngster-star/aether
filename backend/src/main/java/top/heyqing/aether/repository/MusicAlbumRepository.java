package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import top.heyqing.aether.model.entity.MusicAlbum;

/**
 * 音乐合集仓储（BackEnd-Plan §6.2 music_album）
 *
 * <p>公开/管理列表的 type 过滤与 keyword 搜索走 Specification 动态条件。</p>
 */
public interface MusicAlbumRepository extends JpaRepository<MusicAlbum, Long>,
        JpaSpecificationExecutor<MusicAlbum> {

    /**
     * 按类型分页（1 自定义合集 2 固定合集；type 为空时不走本方法，公开列表用 Specification）
     */
    Page<MusicAlbum> findByType(Integer type, Pageable pageable);

    /**
     * 推荐合集（is_recommend=1，id 降序由 Pageable 传入）
     */
    List<MusicAlbum> findByIsRecommend(Integer isRecommend, Pageable pageable);
}
