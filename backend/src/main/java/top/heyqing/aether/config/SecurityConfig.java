package top.heyqing.aether.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置
 *
 * <p>阶段0（脚手架）临时配置：放行全部请求，保证 health 连通性校验可通。
 * TODO 阶段1 按 BackEnd-Plan §4 实现：cryptex 登录三级暴力防护（限流/验证码/锁定）、
 * JWT 双 token（Access 30min + Refresh 7d）、/admin/** 鉴权。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
