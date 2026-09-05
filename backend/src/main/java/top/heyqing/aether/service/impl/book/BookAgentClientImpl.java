package top.heyqing.aether.service.impl.book;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import top.heyqing.aether.config.AgentProperties;
import top.heyqing.aether.service.book.BookAgentClient;
import top.heyqing.aether.util.SplitHeuristics;

/**
 * python-agent 分章客户端实现（RestClient，BackEnd-Plan §7.2）
 *
 * <p>任何网络异常/响应不合法一律返回 empty 静默降级（断网可用是完成标准），
 * 仅记录 warn 日志；level 容错（越界值归 1 章）。</p>
 */
@Component
public class BookAgentClientImpl implements BookAgentClient {

    private static final Logger log = LoggerFactory.getLogger(BookAgentClientImpl.class);

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public BookAgentClientImpl(AgentProperties agentProperties, JsonMapper jsonMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(agentProperties.getConnectTimeoutSeconds()).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(agentProperties.getReadTimeoutSeconds()).toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(agentProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<List<SplitHeuristics.ChapterDraft>> split(String text, int chunkIndex, int chunkTotal) {
        try {
            String body = jsonMapper.writeValueAsString(jsonMapper.createObjectNode()
                    .put("text", text)
                    .put("chunkIndex", chunkIndex)
                    .put("chunkTotal", chunkTotal));
            String responseBody = restClient.post()
                    .uri("/split-book")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode response = responseBody == null ? null : jsonMapper.readTree(responseBody);
            if (response == null || !response.has("chapters")
                    || !response.get("chapters").isArray() || response.get("chapters").isEmpty()) {
                log.warn("python-agent 分章响应不合法: chunkIndex={}", chunkIndex);
                return Optional.empty();
            }
            return Optional.of(parseChapters(response.get("chapters")));
        } catch (Exception e) {
            // 连接拒绝/超时/反序列化失败：静默降级（SplitHeuristics 兜底）
            log.warn("python-agent 不可用，将降级 Java 启发式: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 解析 chapters 数组（title/level/paragraphs；不合法字段容错跳过）
     */
    private List<SplitHeuristics.ChapterDraft> parseChapters(JsonNode chapters) {
        List<SplitHeuristics.ChapterDraft> result = new ArrayList<>();
        for (JsonNode chapter : chapters) {
            String title = chapter.path("title").asString(null);
            JsonNode paragraphsNode = chapter.path("paragraphs");
            if (title == null || title.isBlank() || !paragraphsNode.isArray()) {
                continue;
            }
            List<String> paragraphs = new ArrayList<>();
            for (JsonNode paragraph : paragraphsNode) {
                String value = paragraph.asString(null);
                if (value != null && !value.isBlank()) {
                    paragraphs.add(value.strip());
                }
            }
            int level = chapter.path("level").asInt(SplitHeuristics.LEVEL_CHAPTER);
            result.add(new SplitHeuristics.ChapterDraft(title.strip(),
                    level == SplitHeuristics.LEVEL_SECTION ? SplitHeuristics.LEVEL_SECTION : SplitHeuristics.LEVEL_CHAPTER,
                    paragraphs));
        }
        return result;
    }
}
