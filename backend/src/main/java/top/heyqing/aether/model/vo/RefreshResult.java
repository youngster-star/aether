package top.heyqing.aether.model.vo;

/**
 * 刷新登录态返回体（BackEnd-Plan §4.2 双 token 轮换）
 *
 * @param accessToken  新 Access Token（请求头携带）
 * @param refreshToken 新 Refresh Token（Controller 写回 HttpOnly Cookie）
 */
public record RefreshResult(String accessToken, String refreshToken) {
}
