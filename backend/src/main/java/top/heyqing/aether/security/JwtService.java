package top.heyqing.aether.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.common.cache.CacheStore;
import top.heyqing.aether.config.JwtProperties;
import top.heyqing.aether.exception.BusinessException;

/**
 * JWT 双 token 服务（BackEnd-Plan §4.2）
 *
 * <p>Access Token：请求头 Authorization: Bearer，30 分钟，无状态自然过期；
 * Refresh Token：HttpOnly Cookie（aether_refresh），7 天，Redis/CacheStore 白名单
 * 轮换——刷新时旧 jti 立即作废（getAndDelete），防重放；登出拉黑。</p>
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final CacheStore cacheStore;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties, CacheStore cacheStore) {
        this.properties = properties;
        this.cacheStore = cacheStore;
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // 生产环境 JWT_SECRET 未配置或过短时直接启动失败（fail-fast，防弱密钥上线）
            throw new IllegalStateException("JWT 密钥未配置或长度不足 32 字节，请检查 JWT_SECRET 环境变量");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 Access Token（负载：userId/role/jti/iat/exp，BackEnd-Plan §4.2）
     */
    public String createAccessToken(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessExpireMinutes(), ChronoUnit.MINUTES)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 签发 Refresh Token 并写入白名单（jti -> userId）
     */
    public String issueRefreshToken(Long userId) {
        String jti = UUID.randomUUID().toString();
        String token = buildRefreshToken(userId, jti);
        cacheStore.set(SecurityConst.REFRESH_KEY + jti, String.valueOf(userId),
                Duration.ofDays(properties.getRefreshExpireDays()));
        return token;
    }

    /**
     * 解析 Access Token；过期返回 20002，其余无效返回 20001
     */
    public Claims parseAccessToken(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 解析 Refresh Token；任何无效（过期/签名错/白名单缺失）统一 20006
     */
    public Claims parseRefreshToken(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    /**
     * 轮换 Refresh Token：旧 jti 原子作废（防重放），签发新 token
     *
     * @param oldToken 旧 Refresh Token
     * @return 新 Refresh Token 与用户 ID
     * @throws BusinessException 20006（白名单缺失=已轮换/已登出，拒绝）
     */
    public RefreshRotation rotateRefreshToken(String oldToken) {
        Claims claims = parseRefreshToken(oldToken);
        String jti = claims.getId();
        String userId = cacheStore.getAndDelete(SecurityConst.REFRESH_KEY + jti);
        if (userId == null) {
            // 白名单不存在：已被轮换过（重放）或已登出
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        Long uid = Long.valueOf(claims.getSubject());
        return new RefreshRotation(issueRefreshToken(uid), uid);
    }

    /**
     * Refresh 轮换结果
     *
     * @param token  新 Refresh Token
     * @param userId 用户 ID
     */
    public record RefreshRotation(String token, Long userId) {
    }

    /**
     * 作废 Refresh Token（登出）
     */
    public void revokeRefreshToken(String token) {
        try {
            Claims claims = parseRefreshToken(token);
            if (claims.getId() != null) {
                cacheStore.delete(SecurityConst.REFRESH_KEY + claims.getId());
            }
        } catch (BusinessException ignored) {
            // token 已无效，无需处理（幂等登出）
        }
    }

    private String buildRefreshToken(Long userId, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getRefreshExpireDays(), ChronoUnit.DAYS)))
                .signWith(secretKey)
                .compact();
    }
}
