package top.heyqing.aether;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 上下文启动测试（dev profile：H2 内存库）
 */
@SpringBootTest
@ActiveProfiles("dev")
class AetherApplicationTests {

    @Test
    void contextLoads() {
    }
}
