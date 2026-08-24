package top.heyqing.aether.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口
 *
 * <p>外部路径：/aether/api/health（context-path 见 application.yml），
 * 用于前端连通性校验与容器 healthcheck，无需鉴权。</p>
 */
@RestController
public class HealthController {

    /**
     * 存活探针
     *
     * @return {"status":"UP"}
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
