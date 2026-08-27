package top.heyqing.aether;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aether（以太）个人网站后端启动类
 *
 * <p>context-path 配置见 application.yml（/aether/api），
 * 外部访问前缀统一为 www.heyqing.top/aether/api，代码中禁止硬编码路径前缀。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AetherApplication {

    public static void main(String[] args) {
        SpringApplication.run(AetherApplication.class, args);
    }
}
