package top.heyqing.aether.model.vo;

/**
 * 图形验证码返回体（BackEnd-Plan §5.2 认证 auth）
 *
 * @param captchaId    验证码 ID（登录时回传）
 * @param captchaImage base64 图片（data:image/png;base64,...）
 */
public record CaptchaVO(String captchaId, String captchaImage) {
}
