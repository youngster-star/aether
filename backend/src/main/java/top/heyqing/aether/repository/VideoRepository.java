package top.heyqing.aether.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import top.heyqing.aether.model.entity.Video;

/**
 * 视频数据访问（BackEnd-Plan §6.2 video 表）
 */
public interface VideoRepository extends JpaRepository<Video, Long>, JpaSpecificationExecutor<Video> {
}
