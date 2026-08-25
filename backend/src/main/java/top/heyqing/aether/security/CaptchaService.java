package top.heyqing.aether.security;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import top.heyqing.aether.common.cache.CacheStore;
import top.heyqing.aether.model.vo.CaptchaVO;

/**
 * 图形验证码服务（BackEnd-Plan §4.1 二级防护）
 *
 * <p>Hutool Captcha 生成 4 位字符验证码（纯 AWT 实现）；
 * 答案存 CacheStore（captchaId -> code），5 分钟有效且一次性。</p>
 */
@Service
public class CaptchaService {

    /** 验证码有效期 */
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    private final CacheStore cacheStore;

    public CaptchaService(CacheStore cacheStore) {
        this.cacheStore = cacheStore;
    }

    /**
     * 生成验证码
     *
     * @return captchaId + base64 图片
     */
    public CaptchaVO create() {
        // 4 位字符，30 条干扰线，150x48
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(150, 48, 4, 30);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        cacheStore.set(SecurityConst.CAPTCHA_KEY + captchaId, captcha.getCode(), CAPTCHA_TTL);
        return new CaptchaVO(captchaId, captcha.getImageBase64Data());
    }

    /**
     * 校验验证码（一次性，校验后即失效）
     *
     * @param captchaId   验证码 ID
     * @param captchaCode 用户输入
     * @return 是否正确
     */
    public boolean verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null || captchaId.isBlank() || captchaCode.isBlank()) {
            return false;
        }
        String expected = cacheStore.getAndDelete(SecurityConst.CAPTCHA_KEY + captchaId);
        return expected != null && expected.equalsIgnoreCase(captchaCode.trim());
    }
}
