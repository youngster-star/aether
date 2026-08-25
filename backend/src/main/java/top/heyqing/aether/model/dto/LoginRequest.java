package top.heyqing.aether.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求（BackEnd-Plan §5.2：仅密码，用户名内部固定 cryptex）
 *
 * @param password   密码（即 cryptex）
 * @param captchaId  验证码 ID（失败累计 ≥3 次后必填）
 * @param captchaCode 验证码（失败累计 ≥3 次后必填）
 */
public record LoginRequest(
        @NotBlank(message = "密码不能为空") String password,
        String captchaId,
        String captchaCode) {
}
