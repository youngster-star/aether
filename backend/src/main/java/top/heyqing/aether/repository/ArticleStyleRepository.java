package top.heyqing.aether.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.ArticleStyle;

/**
 * 文章样式仓库（BackEnd-Plan §6.2 article_style）
 */
public interface ArticleStyleRepository extends JpaRepository<ArticleStyle, Long> {

    Optional<ArticleStyle> findByIsDefault(Integer isDefault);
}
