package top.heyqing.aether.model.entity;

import java.math.BigDecimal;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * AI 配置表（管理端可切换厂商/模型，BackEnd-Plan §6.2 ai_config）
 */
@Entity
@Table(name = "ai_config")
public class AiConfig extends BaseEntity {

    /** 场景：chat/effect/agent/classify */
    @Column(name = "scene", nullable = false, unique = true, length = 30)
    private String scene;

    /** 厂商：local（Ollama）/cloud（DeepSeek） */
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    /** 模型名称 */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    /** 接口地址（local 默认 http://ollama:11434） */
    @Column(name = "base_url")
    private String baseUrl;

    /** API 密钥（阶段 7 实现加密存储） */
    @Column(name = "api_key", length = 500)
    private String apiKey;

    /** 采样温度 */
    @Column(name = "temperature", nullable = false, precision = 3, scale = 2)
    private BigDecimal temperature = new BigDecimal("0.70");

    /** 是否启用：1 是 0 否 */
    @Column(name = "is_active", nullable = false)
    private Integer isActive = 1;

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }
}
