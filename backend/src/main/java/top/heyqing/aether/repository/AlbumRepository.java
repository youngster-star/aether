package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import top.heyqing.aether.model.entity.Album;

/**
 * 图集数据访问（BackEnd-Plan §6.2 album 表）
 */
public interface AlbumRepository extends JpaRepository<Album, Long>, JpaSpecificationExecutor<Album> {

    /**
     * 推荐图集（is_recommend=1，推荐排序号大者优先，同号新者优先）
     */
    List<Album> findByIsRecommendOrderBySortDescIdDesc(int isRecommend, Pageable pageable);
}
