package top.heyqing.aether.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * python-agent 分章服务配置（BackEnd-Plan §7.2）
 *
 * <p>base-url 仅内网可达（docker-compose python-agent 容器）；agent 不可达时
 * BookSplitService 自动降级 Java 启发式（SplitHeuristics），保证断网可用。</p>
 */
@ConfigurationProperties(prefix = "aether.agent")
public class AgentProperties {

    /** python-agent 服务地址（默认本机 8100） */
    private String baseUrl = "http://localhost:8100";

    /** 连接超时（秒） */
    private int connectTimeoutSeconds = 2;

    /** 读取超时（秒，LLM 精调较慢） */
    private int readTimeoutSeconds = 60;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }
}
