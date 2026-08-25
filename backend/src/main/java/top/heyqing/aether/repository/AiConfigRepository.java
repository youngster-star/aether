package top.heyqing.aether.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.AiConfig;

/**
 * AI 配置表数据访问（BackEnd-Plan §6.2 ai_config）
 */
public interface AiConfigRepository extends JpaRepository<AiConfig, Long> {

    /**
     * 按场景查询配置（chat/effect/agent/classify）
     */
    Optional<AiConfig> findByScene(String scene);
}
