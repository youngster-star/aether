package top.heyqing.aether;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import top.heyqing.aether.common.cache.CacheStore;
import top.heyqing.aether.security.SecurityConst;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * cryptex 登录三级暴力防护集成测试（BackEnd-Plan §4.1）
 *
 * <p>各场景使用独立 IP（X-Forwarded-For），避免限流/失败计数相互污染：
 * - 级别 1 限流：同一 IP 每分钟第 6 次登录请求返回 10003
 * - 级别 2 验证码：失败累计 3 次后必须携带验证码，错误返回 20004
 * - 级别 3 锁定：失败 10 次锁定 15 分钟（20005），解锁后再触发时长指数翻倍</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class LoginProtectionTest {

    /** dev seed 默认密码（AETHER_CRYPTEX 未配置时） */
    private static final String CORRECT_PASSWORD = "heyqing2aether";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheStore cacheStore;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("级别1限流：同一 IP 每分钟第 6 次登录请求返回 10003")
    void rateLimitBlocksSixthRequest() throws Exception {
        String ip = "10.10.1.1";
        // 前 5 次成功登录（限流含成功请求；本场景不清窗口）
        for (int i = 0; i < 5; i++) {
            login(CORRECT_PASSWORD, null, null, ip, false).andExpect(jsonPath("$.code").value(0));
        }
        // 第 6 次被限流
        login(CORRECT_PASSWORD, null, null, ip, false).andExpect(jsonPath("$.code").value(10003));
    }

    @Test
    @DisplayName("级别2验证码：失败3次后必须携带验证码，验证码错误返回 20004")
    void captchaRequiredAfterThreeFailures() throws Exception {
        String ip = "10.10.1.2";
        // 前 3 次失败（未触发验证码要求）
        for (int i = 0; i < 3; i++) {
            login("wrong-password", null, null, ip, true).andExpect(jsonPath("$.code").value(20003));
        }
        // 第 4 次不带验证码：直接 20004
        login("wrong-password", null, null, ip, true).andExpect(jsonPath("$.code").value(20004));
        // 带错误验证码：20004（且不累计失败次数，防呆）
        login("wrong-password", "fake-id", "0000", ip, true).andExpect(jsonPath("$.code").value(20004));
        // 带正确验证码 + 正确密码：登录成功（验证码经 CacheStore 直接读取答案）
        String captchaId = fetchCaptcha(ip);
        String answer = cacheStore.get(SecurityConst.CAPTCHA_KEY + captchaId);
        assertTrue(answer != null, "验证码答案应存在于缓存");
        login(CORRECT_PASSWORD, captchaId, answer, ip, true).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("级别3锁定：失败10次锁定，锁定期内正确密码也拒绝；解锁后再触发时长翻倍")
    void lockAfterTenFailuresWithExponentialBackoff() throws Exception {
        String ip = "10.10.1.3";
        // 连续失败 10 次（第 3 次失败后每次携带有效验证码）
        for (int i = 0; i < 10; i++) {
            String captchaId = fetchCaptcha(ip);
            String answer = cacheStore.get(SecurityConst.CAPTCHA_KEY + captchaId);
            int expected = i < 9 ? 20003 : 20005; // 第 10 次触发锁定
            login("wrong-password", captchaId, answer, ip, true).andExpect(jsonPath("$.code").value(expected));
        }
        // 锁定期间：正确密码也被拒（20005）
        login(CORRECT_PASSWORD, null, null, ip, true).andExpect(jsonPath("$.code").value(20005));

        // 模拟锁定到期（将解锁时间改为过去），再次失败 10 次触发二次锁定
        cacheStore.set(SecurityConst.LOGIN_LOCK_KEY + ip,
                String.valueOf(System.currentTimeMillis() - 1000), Duration.ofMinutes(1));
        for (int i = 0; i < 10; i++) {
            String captchaId = fetchCaptcha(ip);
            String answer = cacheStore.get(SecurityConst.CAPTCHA_KEY + captchaId);
            login("wrong-password", captchaId, answer, ip, true).andExpect(jsonPath("$.code").value(i < 9 ? 20003 : 20005));
        }
        // 二次锁定时长应为 15min * 2 = 1800 秒（指数翻倍）
        String lockDuration = cacheStore.get(SecurityConst.LOGIN_LOCK_DURATION_KEY + ip);
        assertTrue(lockDuration != null && Long.parseLong(lockDuration) == 1800,
                "二次锁定时长应翻倍为 1800 秒，实际: " + lockDuration);

        // 登录成功后防护状态重置（模拟到期后正确登录，失败计数/锁定清空）
        cacheStore.set(SecurityConst.LOGIN_LOCK_KEY + ip,
                String.valueOf(System.currentTimeMillis() - 1000), Duration.ofMinutes(1));
        login(CORRECT_PASSWORD, null, null, ip, true).andExpect(jsonPath("$.code").value(0));
        assertTrue(cacheStore.get(SecurityConst.LOGIN_FAIL_KEY + ip) == null, "登录成功后失败计数应清空");
    }

    /**
     * 执行登录请求
     *
     * @param clearRate 是否在请求前清空该 IP 的限流窗口（验证码/锁定场景请求次数
     *                  超过每分钟 5 次，测试这些场景时不验证限流，故清窗口隔离）
     */
    private org.springframework.test.web.servlet.ResultActions login(String password, String captchaId,
                                                                    String captchaCode, String ip,
                                                                    boolean clearRate) throws Exception {
        if (clearRate) {
            cacheStore.delete(SecurityConst.LOGIN_RATE_KEY + ip);
        }
        String body = objectMapper.writeValueAsString(new LoginBody(password, captchaId, captchaCode));
        return mockMvc.perform(post("/v1/auth/login")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    /**
     * 获取验证码并返回 captchaId（每次清验证码接口限流窗口：本测试不验证该限流，
     * 场景内验证码请求次数超过每分钟 10 次阈值，需隔离避免误伤）
     */
    private String fetchCaptcha(String ip) throws Exception {
        cacheStore.delete(SecurityConst.CAPTCHA_RATE_KEY + ip);
        String content = mockMvc.perform(get("/v1/auth/captcha").header("X-Forwarded-For", ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content).path("data").path("captchaId").asText();
    }

    private record LoginBody(String password, String captchaId, String captchaCode) {
    }
}
