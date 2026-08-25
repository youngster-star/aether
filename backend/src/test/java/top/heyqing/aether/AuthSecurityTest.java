package top.heyqing.aether;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import top.heyqing.aether.config.JwtProperties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 未授权访问与 JWT 认证集成测试（BackEnd-Plan §4.2）
 *
 * <p>完成标准：未授权访问 /admin/** 返回 20001；过期 token 20002；
 * 伪造 token 20001；有效 token 正常访问。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("未授权访问 /admin/** 返回 20001")
    void adminWithoutTokenReturns20001() throws Exception {
        mockMvc.perform(get("/v1/admin/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20001));
    }

    @Test
    @DisplayName("未授权访问存储写接口返回 20001")
    void storageWithoutTokenReturns20001() throws Exception {
        mockMvc.perform(post("/v1/storage/merge").param("uploadId", "any"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20001));
    }

    @Test
    @DisplayName("过期 Access Token 返回 20002，伪造签名 Token 返回 20001")
    void expiredAndForgedTokenRejected() throws Exception {
        // 过期 token：用 dev 密钥签出已过期 1 分钟的合法 token
        var key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
                .subject("1")
                .claim("role", "ROLE_ADMIN")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();
        mockMvc.perform(get("/v1/admin/articles").header("Authorization", "Bearer " + expired))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20002));

        // 伪造 token：错误密钥签发
        var wrongKey = Keys.hmacShaKeyFor("wrong-wrong-wrong-wrong-wrong-wrong-wrong-32bytes!".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .subject("1")
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(wrongKey)
                .compact();
        mockMvc.perform(get("/v1/admin/articles").header("Authorization", "Bearer " + forged))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20001));
    }

    @Test
    @DisplayName("有效 Access Token 可访问受保护接口（登录 → 携带 Bearer → 认证通过）")
    void validTokenGrantsAccess() throws Exception {
        // 登录获取 Access Token（dev 默认密码；本测试 IP 独立，不受其他场景影响）
        String content = mockMvc.perform(post("/v1/auth/login")
                        .header("X-Forwarded-For", "10.10.2.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody("heyqing2aether"))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(content).path("data").path("accessToken").asText();

        // 携带 token 访问受保护接口：认证通过（此处以非法业务参数验证已越过认证层，返回参数错误 10001）
        mockMvc.perform(post("/v1/storage/init")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001));
    }

    @Test
    @DisplayName("Refresh Token 轮换链路：登录下发 Cookie → 刷新轮换 → 旧 token 重放被拒 → 登出后作废")
    void refreshRotationRejectsReplayAndLogout() throws Exception {
        // 登录：Refresh Token 经 HttpOnly Cookie 下发（BackEnd-Plan §4.2）
        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .header("X-Forwarded-For", "10.10.2.11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody("heyqing2aether"))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Cookie refreshCookie = loginResult.getResponse().getCookie("aether_refresh");
        assertNotNull(refreshCookie, "登录应下发 Refresh Cookie");

        // 刷新：签发新 Access + 轮换新 Refresh Cookie
        MvcResult refreshResult = mockMvc.perform(post("/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Cookie rotatedCookie = refreshResult.getResponse().getCookie("aether_refresh");
        assertNotNull(rotatedCookie, "刷新应轮换下发新 Refresh Cookie");
        assertNotEquals(refreshCookie.getValue(), rotatedCookie.getValue(), "轮换后 Cookie 值应变化");

        // 旧 Refresh Token 重放：白名单已作废 → 20006（防重放核心）
        mockMvc.perform(post("/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20006));

        // 登出：作废当前 Refresh Token
        mockMvc.perform(post("/v1/auth/logout").cookie(rotatedCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        // 登出后再刷新：20006
        mockMvc.perform(post("/v1/auth/refresh").cookie(rotatedCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20006));
    }

    private record LoginBody(String password) {
    }
}
