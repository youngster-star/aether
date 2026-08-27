package top.heyqing.aether.constant;

/**
 * API 路径与公开端点常量（BackEnd-Plan §2 关键约定：代码禁止硬编码路径前缀）
 *
 * <p>注意：{@link #CONTEXT_PATH} 必须与 application.yml 的 server.servlet.context-path 保持一致。</p>
 */
public final class ApiConst {

    private ApiConst() {
    }

    /** 外部 context-path（与 application.yml 同步，生成签名 URL 等场景引用） */
    public static final String CONTEXT_PATH = "/aether/api";

    /** API 版本前缀（BackEnd-Plan §5.1：外部完整路径 /aether/api/v1） */
    public static final String API_V1 = "/v1";

    /**
     * 公开端点清单（无需 Access Token，BackEnd-Plan §5.1）
     * 后续阶段新增公开业务接口（/v1/articles 等）时在此追加。
     */
    public static final String[] PUBLIC_PATHS = {
            "/health",
            "/error",
            "/v1/auth/**",
            "/v1/storage/file/**",
            // 文章公开接口（BackEnd-Plan §5.2）
            "/v1/articles/**",
            "/v1/categories",
            "/v1/tags",
            // 图集/视频公开接口（BackEnd-Plan §5.2，阶段 3）
            "/v1/albums/**",
            "/v1/videos/**",
            // dev 环境调试工具（h2-console/swagger/actuator），由 SecurityConfig 按 profile 追加
    };
}
