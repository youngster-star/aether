package top.heyqing.aether.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import top.heyqing.aether.constant.ApiConst;
import top.heyqing.aether.security.JwtAuthFilter;
import top.heyqing.aether.security.RestAccessDeniedHandler;
import top.heyqing.aether.security.RestAuthEntryPoint;

/**
 * 安全配置（BackEnd-Plan §4）
 *
 * <p>无状态 JWT：Access Token 经 Authorization 请求头携带（免 CSRF）；
 * /v1/admin/** 与存储写接口需认证，其余公开端点白名单放行；
 * 兜底 denyAll（fail-closed）：新增公开接口必须显式登记到 ApiConst.PUBLIC_PATHS。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 需认证的写接口与管理接口（BackEnd-Plan §5.1）
     */
    private static final String[] PROTECTED_PATHS = {
            "/v1/admin/**",
            "/v1/storage/init",
            "/v1/storage/chunk",
            "/v1/storage/merge",
    };

    /** dev/test 环境调试端点（swagger/h2-console/actuator 全部端点） */
    private static final String[] DEV_ONLY_PATHS = {
            "/h2-console/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                                   RestAuthEntryPoint entryPoint, RestAccessDeniedHandler deniedHandler,
                                                   Environment environment) throws Exception {
        boolean dev = environment.acceptsProfiles(Profiles.of("dev", "test", "local"));
        if (dev) {
            // H2 控制台以 iframe 展示，需允许同源 frame
            http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        }
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(handler -> handler
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(deniedHandler))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PROTECTED_PATHS).authenticated()
                    .requestMatchers(publicPaths(dev)).permitAll()
                    .anyRequest().denyAll())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 密码编码器：Argon2（salt 16B / hash 32B / 并行度 1 / 内存 64MB / 迭代 3）
     * 管理员登录低频场景，单次校验约数百毫秒可接受（BackEnd-Plan §4.1）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }

    private String[] publicPaths(boolean dev) {
        List<String> paths = new ArrayList<>(Arrays.asList(ApiConst.PUBLIC_PATHS));
        if (dev) {
            paths.addAll(Arrays.asList(DEV_ONLY_PATHS));
        }
        return paths.toArray(String[]::new);
    }
}
