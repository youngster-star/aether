package top.heyqing.aether.model.vo;

/**
 * 登录返回体（BackEnd-Plan §5.2 认证 auth）
 *
 * <p>Refresh Token 不返回 body，由后端经 HttpOnly Cookie 下发。</p>
 *
 * @param accessToken Access Token（30 分钟，请求头 Authorization: Bearer 携带）
 */
public record LoginVO(String accessToken) {
}
