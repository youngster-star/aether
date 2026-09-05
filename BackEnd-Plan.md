# Aether 后端开发文档（BackEnd-Plan）

> 本文档是基于 BackEnd.md（原始需求，保留不动）生成的**详细开发方案**，作为后端开发的唯一依据。
> 文档版本：v1.1（2026-08-24）

## 1 技术选型

| 组件 | 版本 | 选型理由 |
| --- | --- | --- |
| Java | 21 LTS | 用户拍板；虚拟线程；Spring Boot 4.x 基线 |
| Spring Boot | 4.1.x | 4.1.0 于 2026-06-10 GA（基于 Spring Framework 7.0.8 + Spring Security 7.1.0），活跃支持至 2027-07-31。3.5 已 EOL（2026-06-30）、4.0 开源支持 2026-12-31 到期，均不可选。⚠️ 4.x 变化：Jackson 3、Jakarta EE 11、`HttpHeaders` 不再实现 `MultiValueMap` |
| Spring Data JPA | 随 Boot BOM | 需求文档指定 JPA；Hibernate 7.4 |
| MySQL | 8.4 LTS | 8.0 已 EOL（2026-04-30）；字符集 utf8mb4 |
| Redis | 8.x | 限流、验证码、Refresh token 白名单、分片进度。SSPL 许可自托管个人使用无碍 |
| ip2region | 2.7.0 + xdb 数据文件 | 离线 IP 定位，IPv4/IPv6、微秒级查询；`BufferCache` 全量内存（xdb 约 11MB）。数据文件采用 2025-09 版 v2 格式 xdb（仓库现行 master 已切 v4/v6 双文件新格式，其 Java 3.x 库尚未发布 Maven 中心，故采用中心版 2.7.0 + 历史 v2 数据文件；数据更新策略见附录 B） |
| jsoup | 1.19.x | 富文本 XSS sanitize 白名单过滤（§4.3，文章/公告/书籍内容入库前清洗） |
| commons-imaging | 1.0-alpha3 | 图片 EXIF 抹除（§4.5）：JPEG `ExifRewriter.removeExif` 无损移除（不重编码像素），PNG 剥离 eXIf 辅助块（手写 chunk 过滤），GIF/WebP 无标准定位元数据不处理 |
| LangChain4j | 1.19.0 | 统一 Ollama（本地）+ DeepSeek（OpenAI 兼容协议）双 provider |
| 验证码 | Hutool Captcha 5.8.x | 图形验证码（纯 AWT 实现）。原方案 easy-captcha 依赖 javax.servlet-api，与 Boot 4（Jakarta EE 11）冲突，按预案切换 Hutool |
| JWT | jjwt 0.12.x | Access/Refresh 双 token（HMAC-SHA256），见 §4.2 |
| OSS SDK | aliyun-sdk-oss 3.17.x | 私有 Bucket 分片上传（CompleteMultipartUpload），见 §8 |
| 构建/部署 | Maven + Docker Compose | 需求指定 |
| Python agent | Python 3.12 + FastAPI | 书籍 txt 分章等 agent 任务（需求明确"java 不好写就用 python"） |

**风险标注**：Spring Boot 4.x 为 2025 年末新大版本，个别第三方 starter 可能存在兼容滞后。缓解措施：pom 依赖版本锁定、优先选择已适配 4.x 的库（easy-captcha 若不适配即换 Hutool）、集成期对每个 starter 做冒烟验证。

## 2 工程结构

包结构在 BackEnd.md 基础上扩展（新增 `common`/`storage`/`ai`/`job`），controller/service 内按业务模块分子包：

```text
top.heyqing.aether
├── controller                  # 控制器（按业务分子包）
│   ├── auth                    # 登录认证
│   ├── article                 # 文章
│   ├── album                   # 图集
│   ├── video                   # 视频
│   ├── music                   # 音乐
│   ├── book                    # 书籍
│   ├── announcement            # 公告
│   ├── subscribe               # 订阅问卷
│   ├── storage                 # 文件存储（上传/媒体访问）
│   ├── ai                      # AI 助手
│   ├── stats                   # 统计（管理端）
│   └── admin                   # 管理端（日志、概览等）
├── service                     # 业务接口（与 controller 对应分子包）
│   └── impl                    # 业务实现
├── repository                  # 数据访问（JPA）
├── model
│   ├── entity                  # 数据库实体（与表一一对应）
│   ├── dto                     # 数据传输对象（入参）
│   └── vo                      # 视图对象（出参）
├── config                      # 配置类（Web/Jackson/JPA/线程池）
├── security                    # 安全认证（JWT 过滤器、cryptex 登录、限流锁定）
├── interceptor                 # 拦截器（游客日志记录等）
├── aspect                      # AOP 切面（@OperationLog 操作日志）
├── common                      # 通用（Result、错误码、分页、常量）
├── storage                     # 存储抽象（StorageService + Local/OSS 实现）
├── ai                          # AI（LangChain4j 配置、AiService 接口、场景路由）
├── job                         # 定时任务（订阅推送、存储补偿、统计聚合）
├── util                        # 工具类（IP 工具、文件工具、签名工具）
├── constant                    # 常量（Redis key、文件类型白名单等）
└── exception                   # 异常处理（业务异常、全局异常处理器）
```

关键约定：

- context-path 为 `/aether/api`（见 §4.7），代码中**禁止硬编码路径前缀**，如确需使用统一从常量读取
- 所有 Controller 只做参数接收/校验/返回，业务逻辑全部在 Service
- entity 禁止直接返回给前端，统一转 VO；入参统一用 DTO + Jakarta Validation 注解
- 时间字段统一 `LocalDateTime`，对外 JSON 格式 `yyyy-MM-dd HH:mm:ss`

## 3 统一返回与异常处理

### 3.1 统一返回体 Result

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "3f2a9c1e8b7d4a2f",
  "timestamp": 1784995200000
}
```

- `code`：0 成功，非 0 失败（错误码分段见下）
- `message`：可读提示（成功为 "success"）
- `requestId`：请求链路 ID（过滤器生成，贯穿日志，便于排障）
- `timestamp`：毫秒时间戳

分页返回统一结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "size": 10
  },
  "requestId": "...",
  "timestamp": 0
}
```

### 3.2 错误码分段

| 分段 | 含义 | 示例 |
| --- | --- | --- |
| 0 | 成功 | — |
| 1xxxx | 通用错误 | 10001 参数错误 / 10002 资源不存在 / 10003 操作频繁 / 10004 数据校验失败 / 10005 文件类型不支持 / 10006 文件大小超限 |
| 2xxxx | 认证错误 | 20001 未认证 / 20002 token 过期 / 20003 密码错误 / 20004 验证码错误或已过期 / 20005 账号已锁定 / 20006 refresh token 无效 |
| 3xxxx | 业务错误 | 300xx 文章（30001 文章不存在 30002 分类不存在 30003 标签不存在 30004 样式不存在）301xx 图集 302xx 视频 303xx 音乐（30301 音乐不存在 30302 音乐合集不存在）304xx 书籍（30401 分章任务不存在 30402 书籍不存在 30403 章节不存在 30404 分章任务状态不符 30405 书籍源文件缺失）305xx 存储（30501 分片缺失 30502 上传会话不存在 30503 文件校验失败（合并后 SHA-256 不一致））306xx 公告 307xx 订阅（30701 邮箱格式错误 30702 该 IP 问卷已提交 30703 问卷修改次数已用完）308xx AI（30801 AI 服务不可用 30802 生成失败 30803 当日使用次数已达上限） |
| 5xxxx | 系统错误 | 50001 系统内部错误 / 50002 存储服务异常 / 50003 AI 服务异常 / 50004 数据库异常 |

### 3.3 全局异常处理

- `@RestControllerAdvice` 统一捕获：`BusinessException`（业务异常，携带错误码）、`MethodArgumentNotValidException`（参数校验，返回 10001 + 字段级错误详情）、`AccessDeniedException`、其余异常兜底 50001（记录完整堆栈日志，对外不泄露内部细节）
- 参数校验：DTO 字段用 Jakarta Validation 注解（`@NotBlank`/`@Email`/`@Size` 等），禁止在 Controller 手工 if-else 校验

## 4 安全设计

### 4.1 cryptex 登录与暴力破解防护

单账户登录：**仅输入密码**（用户名内部固定为 `cryptex`，登录接口不收用户名），密码即需求中的 cryptex，默认 `heyqing2aether`。三级防护（基于 CacheStore 抽象，默认 Redis 实现；dev 环境 Redis 未就绪时经 `aether.cache.store=memory` 切换内存实现回退，与 H2 回退同理）：

| 级别 | 触发条件 | 措施 |
| --- | --- | --- |
| 1 限流 | 所有登录请求 | 滑动窗口：同一 IP 每分钟最多 5 次登录请求（含成功），超限返回 10003 |
| 2 验证码 | 同一 IP 登录失败累计 3 次 | 之后每次登录必须携带图形验证码（easy-captcha，Redis 存 captchaId→答案，5 分钟有效，一次性）；验证码错误返回 20004 |
| 3 锁定 | 连续失败 10 次 | 锁定 15 分钟，且每次解锁后再触发锁定时间指数翻倍（15min→30min→60min…上限 24h），返回 20005 |

- 密码哈希：Argon2（Spring Security `Argon2PasswordEncoder`），**默认密码 `heyqing2aether` 仅用于 dev 环境**，由环境变量 `AETHER_CRYPTEX` 注入初始化（首次启动写入 sys_user，生产环境必须修改且强制强度校验：长度 ≥ 12、含大小写+数字+特殊字符）
- 登录成功后重置失败计数

### 4.2 认证方案：JWT 双 Token

| Token | 存放 | 有效期 | 用途 | 失效控制 |
| --- | --- | --- | --- | --- |
| Access Token | `Authorization: Bearer` 请求头 | 30 分钟 | 访问管理端 API | 无状态，自然过期 |
| Refresh Token | HttpOnly + Secure + SameSite=Lax Cookie（`aether_refresh`，path=/aether） | 7 天 | 刷新 Access | Redis 白名单：每次刷新轮换新 token、旧 token 立即作废；登出时拉黑 |

- 选 JWT 理由：header 携带天然免 CSRF；无状态便于 nginx 水平扩展；refresh 存 Redis 可随时强制失效
- Access 负载：`{userId, role, jti, iat, exp}`，HMAC-SHA256（密钥环境变量注入）
- 缓存层：限流计数/验证码/refresh 白名单统一走 `CacheStore` 接口（set/get/getAndDelete/setIfAbsent/increment/windowAdd/windowCount），Redis 实现用于 prod，内存实现（ConcurrentHashMap + 窗口队列）用于 dev 回退，`@ConditionalOnProperty` 按 `aether.cache.store` 装配（仅 prod 允许 redis，dev 默认 memory）
- 刷新接口校验 Cookie + 白名单 + 轮换，防 refresh 重放

### 4.3 CSRF / XSS / 注入

| 威胁 | 防护 |
| --- | --- |
| CSRF | Access Token 在 header 非 Cookie，天然免疫；登录/刷新接口 SameSite=Lax + 同源校验 |
| XSS | 富文本（文章/公告/书籍内容）入库前 jsoup 白名单 sanitize（保留 p/h1-h6/img/a/blockquote/code/table 等白名单标签，剥除事件属性与 script），前端渲染再经 DOMPurify 双保险；响应头 CSP（见部署） |
| SQL 注入 | JPA 参数绑定；搜索接口禁止拼接 SQL，用 Specification/原生查询参数绑定 |
| SSRF | 后端无任意 URL 回源场景（预留检查项） |

### 4.4 媒体防直链下载

- **所有**媒体（图片/视频/音频/书籍封面等）不直接暴露存储 URL，统一走签名访问：

```text
GET /aether/api/v1/storage/file/{fileId}?expires=1785000000&sign=abc123...
```

- `sign = HMAC-SHA256(fileId + ":" + expires, 密钥)`，有效期默认 10 分钟（图片可放宽至 30 分钟）
- 后端校验通过后流式转发：本地实现用 `FileChannel` + 支持 `Range` 请求头（**视频拖动进度条依赖 Range**）；OSS 用私有 Bucket + `generatePresignedUrl`（前端拿到的仍是短时效签名 URL，不能长期直链）
- 辅助：nginx `Referer` 白名单校验（非本站 Referer 直接 403）
- 前端 ProtectedImage 组件拦截右键/拖拽（见 UI-Plan），后端签名 URL 是核心防线

### 4.5 上传安全

- 类型白名单：扩展名 + 文件魔数双重校验（图片 jpg/png/gif/webp、视频 mp4/webm、音频 mp3/flac/wav/aac/m4a、文本 txt/md）
- 大小限制：单文件上限 2GB（分片后每片 8MB，不影响）；图片上传前 EXIF 抹除定位信息（**2026-08-27 阶段 3 已实现**：merge 合并校验通过后、落库前执行——JPEG 走 commons-imaging `ExifRewriter.removeExif` 无损移除（像素不重编码，仅剥离 APP1 等元数据段）；PNG 手写 chunk 过滤剥离 eXIf 块（保留 ICC 等色彩块）；GIF/WebP 无标准定位元数据规范，跳过）
- 存储名一律 UUID（防路径穿越），用户原始文件名仅存 `original_name` 字段
- 客户端 IP 以 nginx `X-Real-IP`（覆盖式设置，§11.3）为准，不信任客户端可控的 X-Forwarded-For

### 4.6 游客 IP 分析

- 首次访问拦截器记录 `visitor_log`：ip2region 解析 `country/province/city` 与完整地域字符串（如 `中国·陕西·西安`），UA 解析设备/浏览器/系统
- **地区降级规则**：城市未知（ip2region 返回空/0）时降级到省份——`region` 显示 `中国·陕西`，`region_code` 取省级行政区划代码；`visitor_daily_stat` 按同规则聚合（ECharts 地图能到市则到市，到不了市以省着色）
- 定时 job（每天 01:30）聚合 `visitor_daily_stat` 供 ECharts 地图直接查询
- 文章阅读数：同一 IP 对同一文章 24 小时内只计 1 次（Redis `SETNX article:read:{articleId}:{ip}`，TTL 24h）

### 4.7 context-path 与 robots

- `server.servlet.context-path=/aether/api`，nginx 直接 `proxy_pass` 透传（见 §11.3），代码中禁止硬编码前缀
- robots 协议：`User-agent: *  Disallow: /aether/`（全站禁爬，nginx 直接托管静态 robots.txt）
- nginx 对非浏览器 UA 限流；对高频 IP 自动熔断（见 §11）

## 5 API 设计

### 5.1 规范

- 版本前缀 `/api/v1`（外部完整路径 `/aether/api/v1`，context-path 为 `/aether/api`）
- RESTful 风格：资源复数 + HTTP 动词；管理端接口统一加 `/admin` 段
- 鉴权：`/admin/**` 需 Access Token；公开接口无鉴权但有限流
- Swagger/OpenAPI 文档地址：`/aether/api/swagger-ui.html`（仅 dev/test 环境开启）
- 破坏性变更升 v2 并与 v1 并存一个版本周期（见 Stage.md）

### 5.2 端点清单

**认证 auth**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /auth/login | 登录 `{password, captchaId?, captchaCode?}`（仅密码，即 cryptex）→ `{accessToken}`（refresh 走 Cookie） |
| POST | /auth/refresh | 刷新 Access Token |
| POST | /auth/logout | 登出（拉黑 refresh） |
| GET | /auth/captcha | 获取图形验证码 `{captchaId, captchaImage(base64)}` |

**文章 article（公开）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /articles | 分页列表 `?page&size&categoryId&tagId&keyword&sort=latest\|hot`；keyword 搜索标题/简介/内容（仅文章可搜索） |
| GET | /articles/hot | 热门文章 `?limit=6-10`（hot_order 排序，不返回创建时间） |
| GET | /articles/{id} | 文章详情（含样式、分类标签、阅读数自增）。**SSR 预取支持请求头 `X-Aether-No-Count: 1` 跳过阅读计数**（前端 RSC 渲染请求的 IP 是渲染服务器而非访客，真实计数由浏览器 hydrate 补偿请求完成，前端 2026-08-25 落地） |
| GET | /articles/{id}/related | 相关文章（同分类优先 + 最新兜底，6 条） |
| GET | /categories?bizType=article | 分类列表（全站通用接口） |
| GET | /tags?bizType=article | 标签列表 |

**图集 album / 视频 video（公开）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /albums | 图集分页 `?page&size&keyword` |
| GET | /albums/recommend | 推荐图集 `?limit=3-4` |
| GET | /albums/{id} | 图集详情（含图片列表：fileId、标题、介绍、宽高、大小） |
| GET | /videos | 视频分页 |
| GET | /videos/{id} | 视频详情（含时长、章节节点、文件扩展名 ext——播放器类型判定） |

**音乐 music（公开）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /music/albums | 合集列表 `?type=1自定义\|2固定&page&size` |
| GET | /music/albums/{id} | 合集详情（含歌曲列表：单曲条目含封面/音频签名 URL——前端播放队列构建依赖 fileUrl，2026-09-01 阶段 4 落地） |
| GET | /music | 单曲搜索 `?keyword&albumId&page&size`（仅音乐可搜索，与其他模块搜索分隔；keyword 匹配歌名/歌手） |
| GET | /music/{id} | 单曲详情（含歌词、时长、effectConfig） |
| GET | /music/recommend | 推荐音乐/专辑 `?limit=3-4` |

**书籍 book（公开）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /books | 书籍分页 `?page&size&categoryId&tagId&keyword` |
| GET | /books/recommend | 推荐书籍 `?limit` |
| GET | /books/{id} | 书籍详情（封面、简介、作者、版权归属、章节目录） |
| GET | /books/{id}/chapters/{chapterId} | 章节内容（分段排版后的 HTML，见 §7.2） |

**公告 announcement（公开）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /announcements | 公告分页 `?type=1公告\|2动态\|3新闻&page&size` |
| GET | /announcements/latest | 最近 3-5 条（主页轮播） |

**订阅 subscribe（公开）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /subscribe | 订阅 `{email}`（格式校验 + 去重） |
| GET | /subscribe/emails | 按请求 IP 返回该 IP 已提交的邮箱列表 |
| POST | /subscribe/survey | 提交问卷 `{email, ageRange, gender, occupation, interests[]}`（IP 限 1 份） |
| PUT | /subscribe/survey | 修改问卷（update_count<2，第 3 次返回 30703） |
| GET | /survey-options | 问卷选项字典 |

**AI 助手 ai（公开，受限流）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /ai/chat | 对话 `{sessionId, message}`，SSE 流式返回；限流见 §7.5 |
| GET | /ai/config | 前端可见配置 `{enabled, ...}`（悬浮窗是否注入页面） |

**存储 storage（登录后）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /storage/init | 初始化上传 `{md5, size, originalName, storageType}` → `{uploadId, chunkSize, uploadedChunks[]}`；已存在完成文件 → 秒传返回 `{fileId}` |
| POST | /storage/chunk | 上传分片（multipart，`uploadId&index`） |
| POST | /storage/merge | 合并分片 → `{fileId, url(签名), width?, height?, duration?}` |
| GET | /storage/file/{fileId} | 媒体访问（签名，见 §4.4，公开访问但必须携带有效签名） |

**管理端 admin（需 Access Token）**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| CRUD | /admin/articles | 文章增删改查（编辑支持 html/md/latex 多模式与预览，见 UI-Plan） |
| PUT | /admin/articles/{id}/publish | 发布/下线 |
| PUT | /admin/articles/{id}/hot | 设置热度 `{isHot, hotOrder}` |
| PUT | /admin/articles/{id}/style | 绑定独立样式 |
| CRUD | /admin/article-styles | 文章样式管理 |
| CRUD | /admin/categories /admin/tags | 分类标签管理（bizType 过滤） |
| CRUD | /admin/albums | 图集管理；POST /admin/albums/{id}/images 批量添加图片（含排序/标题/介绍）；DELETE /admin/albums/{id}/images/{imageId} 删除单张图片（引用归零自动物理清理） |
| CRUD | /admin/videos | 视频管理；POST /admin/videos/{id}/chapters 管理关键时间节点 |
| CRUD | /admin/music/albums /admin/music | 音乐合集/单曲管理（上传音频、歌词） |
| POST | /admin/music/{id}/effect/generate | AI 生成播放特效（EffectConfig） |
| CRUD | /admin/books | 书籍管理（上传 txt） |
| POST | /admin/books/{id}/split | 触发 AI 分章（调用 python-agent） |
| GET | /admin/books/{id}/split-task | 查询分章任务状态与建议 |
| POST | /admin/books/{id}/chapters/confirm | 确认分章建议并落库（可编辑后提交） |
| CRUD | /admin/announcements | 公告管理 |
| GET | /admin/subscribers | 订阅列表；DELETE 退订 |
| GET | /admin/surveys | 问卷列表与统计 |
| GET | /admin/stats/overview | 概览（访问量/阅读量/订阅数趋势） |
| GET | /admin/stats/visitor-distribution | 游客地域分布（ECharts 地图数据） |
| GET | /admin/logs | 操作日志查询 `?page&module&operator&startTime&endTime` |
| GET/PUT | /admin/ai/config | AI 配置查询/修改（模型厂商、开关、提示词、对话限额） |

**健康检查**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /health | 存活探针（`{status:"UP"}`，无需鉴权） |

## 6 数据库设计

约定：所有表 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`；主键 `BIGINT AUTO_INCREMENT`；每表含 `create_time`，可变表含 `update_time`；**所有字段 COMMENT 完整中文注释**；软删除统一 `status` 字段（默认 1 正常）。

### 6.1 ER 关系总览

```text
sys_user（单管理员：username=cryptex）

visitor_log / visitor_daily_stat（游客）

article ─┬─ article_style（样式，N:1 复用，可空=默认样式）
         ├─ biz_category_rel / biz_tag_rel ── category / tag（全站共用字典）
         └─ storage_file（封面）

album ─ album_image ─ storage_file
video ─ video_chapter；video ─ storage_file
music_album ─ music（N:1，可独立）─ storage_file（音频/封面/歌词）
book ─ book_chapter（1:N，章-节两级）；book ─ book_ai_task（进行中 1:1）；book ─ storage_file

storage_file / storage_chunk / storage_ref（文件体系，全模块共用）
announcement / subscriber / survey / survey_option
operation_log
ai_config / ai_chat_session ─ ai_chat_message / ai_generation_task
```

**关键决策（回答 BackEnd.md 遗留问题）**：

- 文章"每篇可独立编辑风格" → 样式独立成表 `article_style`，`article.article_style_id` 外键可空（空 = 默认样式）。理由：样式可被多篇复用（同系列共享）、独立编辑不膨胀文章表。样式 JSON 存 CSS 变量集 + 排版参数，前端渲染时转为 CSS Variables 注入文章容器（见 UI-Plan §8.3）
- 分类/标签：全站共用 `category`/`tag` 表，以 `biz_type` 区分业务域；关联用通用 `biz_category_rel`/`biz_tag_rel`。seed 脚本生成"尽可能全"的初始数据（文章分类：技术/生活/随笔/读书笔记/摄影/音乐/影视等；书籍分类：科幻/奇幻/推理/文学/历史/传记等；兴趣爱好：音乐/阅读/摄影/编程/旅行/运动/电影/游戏/美食/绘画等）

### 6.2 建表 DDL（全部字段含 COMMENT）

```sql
-- ========== 用户与游客 ==========

-- 管理员表（单账户）
CREATE TABLE `sys_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(50)  NOT NULL                COMMENT '登录名（固定 cryptex）',
  `password_hash`   VARCHAR(255) NOT NULL                COMMENT '密码哈希（Argon2，密码即 cryptex，dev 默认 heyqing2aether）',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 正常 0 禁用',
  `last_login_time` DATETIME     NULL                    COMMENT '最后登录时间',
  `last_login_ip`   VARCHAR(45)  NULL                    COMMENT '最后登录 IP',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员表（单账户，用户名 cryptex）';

-- 游客访问日志表
CREATE TABLE `visitor_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ip`          VARCHAR(45)  NOT NULL                COMMENT '访问 IP（IPv4/IPv6）',
  `country`     VARCHAR(50)  NULL                    COMMENT '国家（ip2region 解析）',
  `province`    VARCHAR(50)  NULL                    COMMENT '省份（ip2region 解析）',
  `city`        VARCHAR(50)  NULL                    COMMENT '城市（ip2region 解析）',
  `region`      VARCHAR(100) NULL                    COMMENT '完整地域串，如：中国·陕西·西安',
  `user_agent`  VARCHAR(500) NULL                    COMMENT '原始 User-Agent',
  `device_type` VARCHAR(20)  NULL                    COMMENT '设备类型：PC/Mobile/Tablet',
  `browser`     VARCHAR(50)  NULL                    COMMENT '浏览器名称',
  `os`          VARCHAR(50)  NULL                    COMMENT '操作系统',
  `visit_path`  VARCHAR(255) NULL                    COMMENT '访问路径',
  `referer`     VARCHAR(500) NULL                    COMMENT '来源页面',
  `visit_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_ip_time` (`ip`, `visit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='游客访问日志表（IP 定位分析）';

-- 游客日统计表（地域聚合，ECharts 地图数据源）
CREATE TABLE `visitor_daily_stat` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date`   DATE         NOT NULL                COMMENT '统计日期',
  `region_code` VARCHAR(20)  NOT NULL                COMMENT '行政区划代码（ECharts 地图匹配用）',
  `region_name` VARCHAR(100) NULL                    COMMENT '地区名称',
  `visit_count` INT          NOT NULL DEFAULT 0      COMMENT '访问次数',
  `unique_ip`   INT          NOT NULL DEFAULT 0      COMMENT '独立 IP 数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_region` (`stat_date`, `region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='游客日统计表（按地域聚合）';

-- ========== 文章 ==========

-- 文章表
CREATE TABLE `article` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`            VARCHAR(200) NOT NULL                COMMENT '文章标题',
  `cover_file_id`    BIGINT       NULL                    COMMENT '封面文件 ID（storage_file.id）',
  `summary`          VARCHAR(500) NULL                    COMMENT '文章简介',
  `content_html`     MEDIUMTEXT   NOT NULL                COMMENT 'HTML 内容（sanitize 后）',
  `content_md`       MEDIUMTEXT   NOT NULL                COMMENT 'Markdown 源内容（编辑回显用）',
  `word_count`       INT          NOT NULL DEFAULT 0      COMMENT '字数',
  `reading_count`    INT          NOT NULL DEFAULT 0      COMMENT '阅读次数（IP 24 小时去重）',
  `is_hot`           TINYINT      NOT NULL DEFAULT 0      COMMENT '是否热门：1 是 0 否',
  `hot_order`        INT          NOT NULL DEFAULT 0      COMMENT '热度排序（越大越靠前）',
  `article_style_id` BIGINT       NULL                    COMMENT '文章样式 ID（NULL=默认样式）',
  `is_published`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否发布：1 已发布 0 草稿',
  `publish_time`     DATETIME     NULL                    COMMENT '发布时间',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pub_time` (`is_published`, `publish_time`),
  KEY `idx_hot` (`hot_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章表（博客主体，支持独立样式与自定义热度）';

-- 文章独立样式表
CREATE TABLE `article_style` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50) NOT NULL                COMMENT '样式名称',
  `style_json`  JSON        NOT NULL                COMMENT '样式配置（字体/字号/行高/段间距/首行缩进/主题色等，见 §6.3）',
  `is_default`  TINYINT     NOT NULL DEFAULT 0      COMMENT '是否默认样式：1 是 0 否',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章独立样式表（可被多篇文章复用）';

-- 分类表（全站共用）
CREATE TABLE `category` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50) NOT NULL                COMMENT '分类名称',
  `slug`        VARCHAR(80) NOT NULL                COMMENT '唯一标识（URL 用）',
  `biz_type`    VARCHAR(20) NOT NULL                COMMENT '业务域：article/book/music/album/video/announcement',
  `sort`        INT         NOT NULL DEFAULT 0      COMMENT '排序号',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_slug` (`biz_type`, `slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类表（全站共用，biz_type 区分业务域，seed 生成全量初始数据）';

-- 标签表（全站共用）
CREATE TABLE `tag` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50) NOT NULL                COMMENT '标签名称',
  `slug`        VARCHAR(80) NOT NULL                COMMENT '唯一标识',
  `biz_type`    VARCHAR(20) NOT NULL                COMMENT '业务域：article/book/music/album/video/announcement',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_slug` (`biz_type`, `slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表（全站共用，seed 生成全量初始数据）';

-- 业务-分类关联表
CREATE TABLE `biz_category_rel` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type`    VARCHAR(20) NOT NULL                COMMENT '业务域（与 category.biz_type 对应）',
  `biz_id`      BIGINT      NOT NULL                COMMENT '业务记录 ID',
  `category_id` BIGINT      NOT NULL                COMMENT '分类 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_cat` (`biz_type`, `biz_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务-分类关联表（多对多）';

-- 业务-标签关联表
CREATE TABLE `biz_tag_rel` (
  `id`       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` VARCHAR(20) NOT NULL                COMMENT '业务域',
  `biz_id`   BIGINT      NOT NULL                COMMENT '业务记录 ID',
  `tag_id`   BIGINT      NOT NULL                COMMENT '标签 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_tag` (`biz_type`, `biz_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务-标签关联表（多对多）';

-- ========== 图集与视频 ==========

-- 图集表
CREATE TABLE `album` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(200) NOT NULL                COMMENT '图集标题',
  `cover_file_id` BIGINT       NULL                    COMMENT '封面文件 ID',
  `intro`         VARCHAR(500) NULL                    COMMENT '图集介绍',
  `is_recommend`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否推荐：1 是 0 否',
  `sort`          INT          NOT NULL DEFAULT 0      COMMENT '推荐排序号',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图集表';

-- 图集图片表
CREATE TABLE `album_image` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `album_id`    BIGINT       NOT NULL                COMMENT '所属图集 ID',
  `file_id`     BIGINT       NOT NULL                COMMENT '图片文件 ID（宽高大小等参数存 storage_file）',
  `title`       VARCHAR(200) NULL                    COMMENT '图片标题（可选）',
  `intro`       VARCHAR(500) NULL                    COMMENT '图片介绍（可选）',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序号',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_album` (`album_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图集图片表';

-- 视频表
CREATE TABLE `video` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(200) NOT NULL                COMMENT '视频标题',
  `cover_file_id` BIGINT       NULL                    COMMENT '封面文件 ID',
  `intro`         VARCHAR(500) NULL                    COMMENT '视频介绍',
  `file_id`       BIGINT       NOT NULL                COMMENT '视频文件 ID',
  `duration`      INT          NOT NULL DEFAULT 0      COMMENT '时长（秒，上传合并后自动探测）',
  `is_recommend`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否推荐：1 是 0 否',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频表';

-- 视频关键时间节点表
CREATE TABLE `video_chapter` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `video_id`    BIGINT       NOT NULL                COMMENT '所属视频 ID',
  `title`       VARCHAR(200) NOT NULL                COMMENT '节点标题',
  `time_offset` INT          NOT NULL                COMMENT '时间偏移（秒）',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序号',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_video` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频关键时间节点表';

-- ========== 音乐 ==========

-- 音乐合集表
CREATE TABLE `music_album` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(200) NOT NULL                COMMENT '合集标题',
  `cover_file_id` BIGINT       NULL                    COMMENT '封面文件 ID',
  `intro`         VARCHAR(500) NULL                    COMMENT '合集介绍',
  `type`          TINYINT      NOT NULL                COMMENT '类型：1 自定义合集 2 固定合集（专辑）',
  `certification` VARCHAR(200) NULL                    COMMENT '认证信息（固定合集必填，如发行方/认证编号）',
  `is_recommend`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否推荐：1 是 0 否',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='音乐合集表（自定义合集与固定合集）';

-- 音乐表
CREATE TABLE `music` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(200) NOT NULL                COMMENT '歌曲名称',
  `artist`        VARCHAR(100) NULL                    COMMENT '歌手',
  `album_id`      BIGINT       NULL                    COMMENT '所属合集 ID（可独立单曲）',
  `cover_file_id` BIGINT       NULL                    COMMENT '封面文件 ID',
  `file_id`       BIGINT       NOT NULL                COMMENT '音频文件 ID',
  `lyric_text`    TEXT         NULL                    COMMENT '歌词文本（LRC 带时间轴 / 纯文本）',
  `lyric_offset`  INT          NOT NULL DEFAULT 0      COMMENT '歌词全局偏移（毫秒，正负可调，用于解决歌词不同步）',
  `duration`      INT          NOT NULL DEFAULT 0      COMMENT '时长（秒，上传后自动检测）',
  `effect_config` JSON         NULL                    COMMENT 'AI 特效配置（EffectConfig，见 §7.3）',
  `effect_source` TINYINT      NOT NULL DEFAULT 1      COMMENT '特效来源：1 AI 生成 2 手工调整',
  `is_recommend`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否推荐：1 是 0 否',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_album` (`album_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='音乐表（含 AI 特效配置）';

-- ========== 书籍 ==========

-- 书籍表
CREATE TABLE `book` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`          VARCHAR(200)  NOT NULL                COMMENT '书名',
  `author`         VARCHAR(100)  NULL                    COMMENT '作者',
  `cover_file_id`  BIGINT        NULL                    COMMENT '封面文件 ID',
  `intro`          VARCHAR(1000) NULL                    COMMENT '简介',
  `ownership_type` TINYINT       NOT NULL                COMMENT '版权归属：1 本人 2 他人出版',
  `source_file_id` BIGINT        NULL                    COMMENT '源文件 ID（txt）',
  `total_chapters` INT           NOT NULL DEFAULT 0      COMMENT '总章节数',
  `is_recommend`   TINYINT       NOT NULL DEFAULT 0      COMMENT '是否推荐：1 是 0 否',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍表（含出版图书与本人作品）';

-- 书籍章节表
CREATE TABLE `book_chapter` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `book_id`    BIGINT       NOT NULL                COMMENT '所属书籍 ID',
  `title`      VARCHAR(200) NOT NULL                COMMENT '章节标题',
  `level`      TINYINT      NOT NULL                COMMENT '层级：1 章 2 节',
  `parent_id`  BIGINT       NULL                    COMMENT '父章节 ID（节挂章）',
  `order_no`   INT          NOT NULL                COMMENT '排序号（全书顺序）',
  `content`    MEDIUMTEXT   NOT NULL                COMMENT '章节内容（分段排版后的 HTML：段首空两格、段间半行距）',
  `word_count` INT          NOT NULL DEFAULT 0      COMMENT '字数',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_order` (`book_id`, `order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍章节表（章-节两级结构）';

-- 书籍分章任务表
CREATE TABLE `book_ai_task` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `book_id`      BIGINT       NOT NULL                COMMENT '所属书籍 ID',
  `status`       TINYINT      NOT NULL                COMMENT '状态：1 解析中 2 待确认 3 已完成 4 失败',
  `ai_result`    JSON         NULL                    COMMENT 'AI 分章建议（章节标题+段落+偏移）',
  `fail_reason`  VARCHAR(500) NULL                    COMMENT '失败原因',
  `confirm_time` DATETIME     NULL                    COMMENT '用户确认时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_book` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍 AI 分章任务表（状态机）';

-- ========== 存储 ==========

-- 文件元数据表（本地与 OSS 统一记录）
CREATE TABLE `storage_file` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `original_name` VARCHAR(255)  NOT NULL                COMMENT '原始文件名',
  `storage_type`  TINYINT       NOT NULL                COMMENT '存储类型：1 本地 2 OSS',
  `object_key`    VARCHAR(500)  NOT NULL                COMMENT '存储对象键（本地=相对路径，OSS=objectKey）',
  `size`          BIGINT        NOT NULL                COMMENT '文件大小（字节）',
  `file_md5`      CHAR(64)      NOT NULL                COMMENT '文件 SHA-256 哈希（秒传依据）',
  `mime_type`     VARCHAR(100)  NULL                    COMMENT 'MIME 类型',
  `ext`           VARCHAR(20)   NULL                    COMMENT '扩展名',
  `width`         INT           NULL                    COMMENT '图片宽度（像素）',
  `height`        INT           NULL                    COMMENT '图片高度（像素）',
  `duration`      INT           NULL                    COMMENT '音视频时长（秒）',
  `meta`          JSON          NULL                    COMMENT '其他元数据（编码/帧率/色域等）',
  `status`        TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1 正常 0 已删除（延迟清理）',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_md5` (`file_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件元数据表（本地与 OSS 统一记录）';

-- 分片记录表
CREATE TABLE `storage_chunk` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `upload_id`    VARCHAR(64) NOT NULL                COMMENT '上传会话 ID（UUID）',
  `file_md5`     CHAR(64)    NOT NULL                COMMENT '文件 SHA-256',
  `chunk_index`  INT         NOT NULL                COMMENT '分片序号（从 0）',
  `chunk_total`  INT         NOT NULL                COMMENT '总分片数',
  `size`         BIGINT      NOT NULL                COMMENT '分片大小（字节）',
  `storage_type` TINYINT     NOT NULL                COMMENT '存储类型（与上传时选择一致）',
  `status`       TINYINT     NOT NULL DEFAULT 1      COMMENT '状态：1 已上传 2 已合并 0 失效',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_upload_chunk` (`upload_id`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分片上传进度表（断点续传依据）';

-- 业务-文件引用表（删除一致性核心）
CREATE TABLE `storage_ref` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_id`     BIGINT      NOT NULL                COMMENT '文件 ID',
  `biz_type`    VARCHAR(20) NOT NULL                COMMENT '业务域：article/album/video/music/book/announcement',
  `biz_id`      BIGINT      NOT NULL                COMMENT '业务记录 ID',
  `ref_count`   INT         NOT NULL DEFAULT 1      COMMENT '引用次数（同一文件多处引用时递增）',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_biz` (`file_id`, `biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务-文件引用表（删除一致性核心）';

-- ========== 公告与订阅 ==========

-- 公告表
CREATE TABLE `announcement` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(200) NOT NULL                COMMENT '公告标题',
  `content`       TEXT         NOT NULL                COMMENT '公告内容（文字+图片，sanitize 后 HTML）',
  `type`          TINYINT      NOT NULL                COMMENT '类型：1 公告 2 动态 3 新闻',
  `cover_file_id` BIGINT       NULL                    COMMENT '封面文件 ID（可选）',
  `is_top`        TINYINT      NOT NULL DEFAULT 0      COMMENT '是否置顶：1 是 0 否',
  `publish_time`  DATETIME     NULL                    COMMENT '发布时间',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_pub` (`type`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告表（公告/动态/新闻）';

-- 订阅者表
CREATE TABLE `subscriber` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `email`            VARCHAR(200) NOT NULL                COMMENT '订阅邮箱',
  `ip`               VARCHAR(45)  NOT NULL                COMMENT '订阅时 IP',
  `status`           TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 订阅中 0 已退订',
  `subscribe_time`   DATETIME     NULL                    COMMENT '订阅时间',
  `unsubscribe_time` DATETIME     NULL                    COMMENT '退订时间',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ip` (`ip`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订阅者表（一个 IP 可提交多个邮箱）';

-- 问卷表
CREATE TABLE `survey` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `email`        VARCHAR(200) NOT NULL                COMMENT '邮箱（与订阅对应）',
  `ip_hash`      CHAR(64)     NOT NULL                COMMENT 'IP 加盐哈希（隐私保护，唯一约束）',
  `age_range`    VARCHAR(20)  NULL                    COMMENT '年龄段',
  `gender`       VARCHAR(10)  NULL                    COMMENT '性别',
  `occupation`   VARCHAR(50)  NULL                    COMMENT '职业',
  `interests`    JSON         NULL                    COMMENT '兴趣爱好（survey_option id 列表）',
  `update_count` INT          NOT NULL DEFAULT 0      COMMENT '修改次数（允许最多 2 次）',
  `submit_time`  DATETIME     NULL                    COMMENT '提交时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ip_hash` (`ip_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问卷表（IP 限 1 份、最多修改 2 次）';

-- 问卷选项字典表
CREATE TABLE `survey_option` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `field`       VARCHAR(30) NOT NULL                COMMENT '字段名：age_range/gender/occupation/interests',
  `label`       VARCHAR(50) NOT NULL                COMMENT '选项文本',
  `sort`        INT         NOT NULL DEFAULT 0      COMMENT '排序号',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_field` (`field`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问卷选项字典表（seed 全量生成：年龄段 10 档/性别/职业/兴趣爱好）';

-- ========== 日志 ==========

-- 操作日志表
CREATE TABLE `operation_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator`    VARCHAR(50)  NULL                    COMMENT '操作人',
  `module`      VARCHAR(50)  NULL                    COMMENT '模块',
  `action`      VARCHAR(50)  NULL                    COMMENT '操作类型（新增/修改/删除/登录/上传/生成…）',
  `method`      VARCHAR(200) NULL                    COMMENT '请求方法',
  `path`        VARCHAR(255) NULL                    COMMENT '请求路径',
  `params`      JSON         NULL                    COMMENT '请求参数（脱敏后：password/token 等字段替换为 ***）',
  `result`      TINYINT      NULL                    COMMENT '结果：1 成功 0 失败',
  `ip`          VARCHAR(45)  NULL                    COMMENT '操作 IP',
  `cost_ms`     INT          NULL                    COMMENT '耗时（毫秒）',
  `error_msg`   VARCHAR(500) NULL                    COMMENT '失败原因',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_time` (`create_time`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员操作日志表';

-- ========== AI ==========

-- AI 配置表
CREATE TABLE `ai_config` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene`       VARCHAR(30)   NOT NULL                COMMENT '场景：chat/effect/agent/classify',
  `provider`    VARCHAR(20)   NOT NULL                COMMENT '厂商：local（Ollama）/cloud（DeepSeek）',
  `model_name`  VARCHAR(100)  NOT NULL                COMMENT '模型名称',
  `base_url`    VARCHAR(255)  NULL                    COMMENT '接口地址（local 默认 http://ollama:11434）',
  `api_key`     VARCHAR(500)  NULL                    COMMENT 'API 密钥（加密存储）',
  `temperature` DECIMAL(3,2)  NOT NULL DEFAULT 0.70   COMMENT '采样温度',
  `is_active`   TINYINT       NOT NULL DEFAULT 1      COMMENT '是否启用：1 是 0 否',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene` (`scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 配置表（管理端可切换厂商/模型）';

-- AI 对话会话表
CREATE TABLE `ai_chat_session` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`  VARCHAR(64) NOT NULL                COMMENT '会话 ID（前端生成 UUID）',
  `visitor_ip`  VARCHAR(45) NULL                    COMMENT '访问 IP',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 对话会话表';

-- AI 对话消息表
CREATE TABLE `ai_chat_message` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`  VARCHAR(64) NOT NULL                COMMENT '会话 ID',
  `role`        VARCHAR(20) NOT NULL                COMMENT '角色：user/assistant',
  `content`     TEXT        NOT NULL                COMMENT '消息内容',
  `model`       VARCHAR(100) NULL                   COMMENT '使用的模型名称',
  `token_count` INT         NULL                    COMMENT '消耗 token 数',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 对话消息表';

-- AI 生成任务表
CREATE TABLE `ai_generation_task` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene`       VARCHAR(30)  NOT NULL                COMMENT '场景：effect/classify/split',
  `biz_type`    VARCHAR(20)  NULL                    COMMENT '关联业务域',
  `biz_id`      BIGINT       NULL                    COMMENT '关联业务 ID',
  `provider`    VARCHAR(20)  NULL                    COMMENT '实际使用的厂商',
  `status`      TINYINT      NOT NULL                COMMENT '状态：1 处理中 2 成功 3 失败',
  `result`      JSON         NULL                    COMMENT '生成结果',
  `fail_reason` VARCHAR(500) NULL                    COMMENT '失败原因',
  `cost_ms`     INT          NULL                    COMMENT '耗时（毫秒）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 生成任务表（特效/分类推荐/分章统一任务记录）';
```

### 6.3 article_style.style_json 结构示例

```json
{
  "fontFamily": "Noto Serif SC",
  "fontSize": 17,
  "lineHeight": 1.9,
  "letterSpacing": 0,
  "wordSpacing": 0,
  "paragraphSpacing": 16,
  "firstLineIndent": "2em",
  "contentWidth": 720,
  "themeColor": "#8B6F47",
  "serif": true,
  "customCss": ""
}
```

### 6.4 seed 数据策略

- `sys_user`：username=`cryptex` + Argon2(cryptex 环境变量，dev 默认 heyqing2aether)
- `category`/`tag`：按 biz_type 全量生成（文章/书籍/音乐/图集/视频/公告），尽可能全面
- `survey_option`：年龄段/性别/职业/兴趣爱好全量生成
- `ai_config`：四个 scene 各一行默认配置（chat→cloud/DeepSeek、effect→cloud、agent→local/Ollama 优先、classify→local）
- 仅 dev/test 环境执行，prod 不跑 seed（Stage.md 规范）

**演示测试数据（阶段开发用，上线前清理）**：

- 文件：`backend/src/main/resources/db/seed-demo-data.sql`（插入）+ `seed-demo-data-cleanup.sql`（删除，保留分类/标签/问卷选项/AI 配置/管理员/文章样式等字典与配置数据）
- 媒体素材：真实网络素材（图片 webp/jpg/png 7 张、视频 mp4 3 段、音频 mp3 3 首），先执行 `bash scripts/download-demo-media.sh` 下载到本地存储 `backend/data/storage/files/seed/`（不入库）；storage_file 记录的哈希/尺寸/宽高/时长为素材实际探测值
- ID 固定 90001+ 区间，与 DataSeeder 自增数据互不冲突；全部语句幂等（先清本区间再插入）
- 覆盖范围：文章 11 篇（长/短/多图/站内视频链接/代码/表格/草稿/热门/独立样式）、图集 5 个（含零图片边界与同文件 ref_count=2 保活场景）、视频 4 个（有章节/无章节/无封面/同文件复用）、音乐合集 2 个 + 曲目 6 首（LRC 歌词/EffectConfig）、书籍 3 本 + 章节（章-节两级/分章任务四状态）、公告 5 条（三类型）、订阅/问卷、访问日志与日统计（逐条吻合）、操作日志、AI 会话与生成任务；字数/时长/统计等字段值彼此一致

## 7 AI 设计

### 7.1 双 Provider 架构

- LangChain4j 1.19 统一抽象：`langchain4j-ollama`（本地）+ `langchain4j-open-ai`（DeepSeek 走 OpenAI 兼容协议）
- 配置驱动的路由：按 `ai_config.scene` 取当前启用的 provider/model，`AiServices` 声明式接口绑定
- 管理端可切换场景对应的厂商/模型/温度/提示词；`api_key` 加密落库
- 对话接口 SSE 流式输出（`Flux<String>` / `TokenStream`）

### 7.2 书籍 txt 自动分章（Python agent）

独立 Python FastAPI 服务（`python-agent` 容器，仅内网可达），接口：

```text
POST /split-book
入参: { "text": "全文或分块", "chunkIndex": 0, "chunkTotal": 1 }
出参: { "chapters": [ { "title": "第1章 ...", "level": 1, "paragraphs": ["...", "..."] } ] }
```

流程：

1. Java 上传 txt → 落 storage → 建 `book_ai_task(status=1 解析中)`
2. Java 按字符边界分块（每块 ≤ 2 万字符，含重叠窗口），逐块调 python-agent
3. python-agent 两步走：**启发式预分**（行首缩进、`第X章/第X节`正则、空行段落）→ **LLM 精调**（Ollama 优先，可配 DeepSeek；输出结构化 JSON 分章建议，含章/节两级识别）；LLM 未配置/调用失败/超时/输出不合法时静默回退启发式结果（`source: heuristic` 标记）
3.1 Java 侧内置同构启发式降级器（`SplitHeuristics`）：python-agent 不可达/超时（connect 2s / read 60s）时本地分块兜底，保证断网可用（阶段 5 完成标准）
4. Java 汇总 → 写 `book_ai_task.ai_result(status=2 待确认)` → 管理端展示分章预览（可编辑调整）
5. 用户确认 → 写 `book_chapter`（分段排版：段落开头空两格 `text-indent: 2em`、段间半行距、保留插图标记位）→ `status=3`
6. 失败 → `status=4 + fail_reason`，支持重试

### 7.3 音乐播放特效生成（EffectConfig）

- 输入：歌词文本、封面主色（后端 `ImageIO` 取缩略图 + k-means 提取前 3 主色）、时长、估计 BPM
- LLM 按 EffectConfig JSON Schema 结构化输出（JSON mode），**结果存 `music.effect_config`**，管理端可预览、调参、重生成
- **阶段 4 先行实现（2026-09-01）**：`EffectConfigService.generate` 以封面调色板（k-means 3 主色，缺封面回退羊皮卷默认色）+ 确定性规则生成粒子/波形/圆环三层（覆盖 amplitude/freqBand/beat 三类绑定，节奏参数由时长粗估 BPM 驱动），产物过 EffectConfigValidator Schema 校验后落库；**LLM 结构化生成在阶段 7 LangChain4j 双 Provider 接入后切换内部实现（接口与校验链路不变）**
- 特殊场景（如固定合集认证页）允许 `sandboxCode` 字段携带 AI 生成的 JS，前端在 iframe sandbox 内受限执行（见 UI-Plan §8.5）

EffectConfig Schema（v1）：

```json
{
  "version": 1,
  "palette": ["#E8C94A", "#1A1B1F", "#8B6F47"],
  "layers": [
    {
      "type": "particles",
      "bind": "amplitude",
      "count": 120,
      "sizeRange": [1, 6],
      "speed": 1.2,
      "opacity": 0.7
    },
    {
      "type": "wave",
      "bind": "freqBand",
      "band": [0, 0.3],
      "amplitude": 40,
      "color": "#E8C94A"
    }
  ],
  "background": { "type": "gradient", "from": "#0F1013", "to": "#1A1B1F" },
  "transition": { "duration": 800, "easing": "easeOutCubic" },
  "sandboxCode": null
}
```

- `layers[].type` 枚举：particles / wave / ring / flowline / text
- `layers[].bind` 枚举：amplitude（响度）/ freqBand（频段，可指定 band 范围）/ beat（节拍脉冲）
- 后端校验 JSON Schema 合法性后才落库（防 AI 幻觉字段）

### 7.4 上传内容自动推荐分类/标签

- 输入：标题/简介/正文摘要 + 候选池（对应 biz_type 的全量 category/tag 名称+id）
- 约束式提示词：**LLM 只允许返回候选池内的 id**，杜绝幻觉新分类
- 返回 `{categoryIds[], tagIds[]}` 供管理端一键采纳（不自动落库，需用户确认）

### 7.5 AI 助手对话（含使用量限制）

- 公开接口，游客可用；前端 SSE 流式渲染
- 会话：前端生成 UUID 作 session_id，历史存 `ai_chat_session/message`，上下文取最近 10 轮
- 站点知识：将文章标题+简介注入系统提示词（站内导航型问答），不强制向量库（后续可扩展）

**使用量限制（按用户/IP 双维度，Redis 计数）**：

| 限制 | 阈值 | 超限返回 |
| --- | --- | --- |
| 每分钟 | 10 条消息 | 10003 操作频繁 |
| 每天 | 50 条消息 | 30803 当日使用次数已达上限 |

- 实现：Redis `ai:rate:{ip}:min`（60s 过期）+ `ai:rate:{ip}:day`（当天 23:59:59 过期）原子自增
- 阈值可通过配置调整（`ai.chat.rate-per-minute` / `ai.chat.rate-per-day`），管理端 AI 配置页可修改
- 游客按 IP 识别（NAT 下同出口 IP 共享限额，可接受误差）

### 7.6 歌词同步与时间轴对齐（音乐模块）

歌词不同步的完整解决方案（组合使用，前端渲染策略见 UI-Plan §6.6）：

| 方案 | 说明 | 适用场景 |
| --- | --- | --- |
| LRC 时间轴 | 上传 LRC 格式歌词（逐行 `[mm:ss.xx]` 时间戳），前端逐行同步滚动 | 标准歌曲，首选 |
| 纯文本均分 | 无时间戳歌词按"总行数 ÷ 总时长"均分滚动（粗略同步） | 无 LRC 时兜底 |
| 全局偏移微调 | `music.lyric_offset`（毫秒，正负可调）：管理端试听时实时调整，随曲保存 | 歌词整体统一快/慢 |
| 手动打点校准 | 管理端歌词编辑器：播放时逐行点击"标记当前行"，自动生成该行时间轴 | 需要精确控制 |
| AI 自动对齐（可选增强） | python-agent 扩展接口 `/align-lyric`：本地 whisper 类 ASR 对音频转写输出带时间轴歌词，与已有歌词文本对齐 | 大量歌曲批量处理，列入阶段 4 可选任务 |

推荐默认组合：**LRC 优先 + 全局偏移微调**；管理端支持打点校准；AI 对齐作为增强项后置实现。

## 8 存储设计

### 8.1 StorageService 抽象

```java
public interface StorageService {
    void uploadChunk(UploadContext ctx, InputStream in);          // 上传分片
    StoredFile merge(UploadContext ctx);                          // 合并分片 → 文件元数据
    void delete(String objectKey);                                // 物理删除
    InputStream open(String objectKey);                           // 读取（本地零拷贝/OSS 流）
    String getSignedUrl(String objectKey, long expiresSeconds);   // 短时效签名 URL（OSS）
    boolean exists(String objectKey);
    String initMultipart(UploadContext ctx);                      // 初始化分片会话（OSS 返回 uploadId；本地无需，默认返回 null）
}
```

- `LocalStorageServiceImpl`：分片存临时目录，合并用 NIO `FileChannel.transferTo` **零拷贝**；`open()` 返回 `FileInputStream`，配合 `Range` 支持流式输出
- `OssStorageServiceImpl`：私有 Bucket；分片走 `UploadPart` + ETag 校验，合并走 `CompleteMultipartUpload`；`getSignedUrl` 走 `generatePresignedUrl`

### 8.2 分片上传 / 断点续传 / 秒传

```text
前端计算文件 SHA-256（Web Worker 边读边算，不阻塞 UI）
→ POST /storage/init {md5, size, originalName, storageType}
   ├─ 若 storage_file 已存在同 md5 且 status=1 → 秒传，直接返回 fileId
   └─ 否则生成 uploadId（UUID），Redis 记录已传分片集合，返回 chunkSize(8MB)+已传索引
→ 并发上传分片（最多 3 并发）POST /storage/chunk（multipart: uploadId, index）
→ 全部完成 → POST /storage/merge
   ├─ 本地：FileChannel.transferTo 顺序零拷贝拼接（保持原始字节序）
   └─ OSS：CompleteMultipartUpload（ETag 列表校验）
→ 后端校验整体 SHA-256 与 init 一致 → 图片执行 EXIF 抹除（§4.5）→ 写 storage_file → 探测元数据（图片宽高/音视频时长）
→ 返回 {fileId, 签名访问 URL}
```

- 断点续传：init 返回已传分片索引，前端跳过已传分片
- 上传会话 24h 过期（Redis TTL + 临时分片清理 job）
- **音视频时长探测**（2026-08-27 阶段 3 落地，MediaProbeService；2026-09-01 阶段 4 扩展 MP3 回退）：首选外部 `ffprobe`（路径 `aether.media.ffprobe-path` 配置，默认 PATH 中的 `ffprobe`，生产 backend 容器内置）；ffprobe 不可用或失败时回退内置解析：MP4/M4A 走 `mvhd` 原子解析（Mp4DurationParser），**MP3 走帧结构解析（Mp3DurationParser：Xing/Info/VBRI 总帧数优先精确值，缺失时按 CBR 码率估算，自动跳过 ID3v1/v2 标签）**，均零依赖；webm/flac/wav/aac 无回退（记 0，管理端可手工补录）；探测失败均不阻断上传（duration=0 + warn 日志）
- 存储选择：init 时前端传 `storageType`（1 本地/2 OSS），管理端上传控件明确展示两套选项（UI-Plan）

### 8.3 删除/修改一致性（需求明确要求）

**删除**（删业务 → 物理文件随之清理）：

1. 事务内：删业务表记录 + 删 `storage_ref`（ref_count 归零的文件标记待删）
2. 事务提交后：异步物理删除 → 成功则 storage_file.status=0；失败进补偿 job（每 10 分钟重试，最多 3 天）
3. 同一文件多处引用时 `ref_count > 1` 只减计数不删物理

**修改**（先传新 → 删旧 → 更新库）：

1. 先上传新文件成功（拿到新 fileId）
2. 更新业务记录指向新 fileId（事务内，旧引用移除）
3. 事务提交后异步删旧物理文件（失败进补偿 job），保证"新文件可用后才动旧文件"

### 8.4 文件类型与大小白名单

| 类型 | 扩展名 | 大小上限 | 说明 |
| --- | --- | --- | --- |
| 图片 | jpg/jpeg/png/gif/webp | 50MB | 上传后提取宽高 |
| 视频 | mp4/webm | 2GB | 上传后 ffprobe 探测时长 |
| 音频 | mp3/flac/wav/aac/m4a | 200MB | 上传后探测时长 |
| 文本 | txt/md | 100MB | 书籍 txt 走分章流程 |

魔数检测：JPEG `FFD8FF`、PNG `89504E47`、GIF `GIF8`、WEBP `RIFF....WEBP`、MP4 `....ftyp`、MP3 `ID3`、FLAC `fLaC`、WAV `RIFF....WAVE`

## 9 订阅推送

- 定时 job（Spring Scheduler，可配置 cron，默认每 10 分钟）：扫描 `subscriber(status=1)` + 新发布文章/公告（`publish_time > last_push_time`）
- JavaMail 发送 HTML 邮件（新文章/公告摘要 + 链接）；发送失败重试 2 次
- 退订：邮件底部携带 HMAC 签名退订链接，点击后 status=0（防伪造退订）
- 邮件服务配置环境变量外置（SMTP 主机/账号/密码）
- 问卷数据供管理端做用户分布分析（年龄段/性别/职业/兴趣统计图表）

## 10 日志模块

- **操作日志**：`@OperationLog(module, action)` AOP 注解标注管理端写操作，切面记录 `operation_log`；`params` 序列化时对敏感字段（password/token）SpEL 表达式脱敏为 `***`
- **技术日志**：Logback 按天滚动（30 天保留），`requestId` 写入 MDC 贯穿全链路；错误日志含堆栈
- 管理端日志页查询 `operation_log`（模块/操作人/时间过滤）

## 11 部署设计

### 11.1 架构

```text
                       ┌─────────────────────────────────────────┐
  www.heyqing.top ───► │ nginx（唯一对外 80/443）                    │
                       │   /aether/        → frontend 静态（_next/）│
                       │   /aether/api/    → backend               │
                       │   /aether/agent/  → python-agent（内网）    │
                       └──────┬──────────────┬───────────────┬──────┘
                              │              │               │
                        frontend(Next)   backend(Java21)  python-agent
                              │              │               │
                        mysql:8.4       redis:8        （ollama 可挂宿主机）
```

### 11.2 docker-compose 服务清单

| 服务 | 镜像 | 要点 |
| --- | --- | --- |
| nginx | nginx:alpine | 唯一对外端口；`location /aether/` 前端静态（含 `_next/`）；`/aether/api/` 反代 backend（透传 path）；`/aether/agent/` 反代 python-agent；HTTPS 证书挂载卷；robots.txt 托管；非浏览器 UA 限流 |
| mysql | mysql:8.4 | 数据卷持久化、utf8mb4、healthcheck |
| redis | redis:8-alpine | requirepass、AOF 持久化 |
| backend | eclipse-temurin:21-jre（定制） | context-path=/aether/api；healthcheck /aether/api/health |
| frontend | node:22-alpine | Next.js standalone 输出，构建时 basePath=/aether |
| python-agent | python:3.12-slim + uvicorn | 仅内网可达；LLM 依赖 Ollama 服务地址配置 |

环境变量（backend）：`AETHER_CRYPTEX`（必填）、`DB_URL/DB_USER/DB_PASSWORD`、`REDIS_HOST/REDIS_PASSWORD`、`JWT_SECRET`、`OSS_ACCESS_KEY/OSS_SECRET/OSS_BUCKET/OSS_ENDPOINT`、`AI_DEEPSEEK_KEY`、`MAIL_*`、`STORAGE_DEFAULT(1|2)`

### 11.3 path 部署要点

- backend：`server.servlet.context-path=/aether/api`，nginx 直接 `proxy_pass http://backend:8080` 透传，**不剥离前缀**（避免双写）
- 前端：Next.js `basePath: '/aether'`，静态资源 `/_next/` 自动落在 `/aether/_next/`
- nginx 示例片段：

```nginx
location /aether/api/ {
    proxy_pass http://backend:8080;          # 透传，不动路径
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    client_max_body_size 2g;
}
location /aether/ {
    root /usr/share/nginx/html;              # frontend 产物挂载
    try_files $uri $uri/ /aether/index.html;
}
```

### 11.4 备份与监控

- 每日凌晨 cron：mysqldump + Redis RDB + 本地存储卷打包（保留 30 天，可挂 OSS 同步）
- Actuator 健康检查接入 uptime 监控（可选）

## 12 测试策略

| 层次 | 工具 | 覆盖 |
| --- | --- | --- |
| 单元测试 | JUnit 5 + Mockito | Service 核心逻辑（文章发布、分章确认、问卷限制、签名校验、限流逻辑） |
| 集成测试 | Testcontainers（MySQL+Redis 真实容器） | Repository 层、上传合并流程、登录全流程 |
| 安全测试 | MockMvc + 手工渗透 + ZAP | 暴力破解防护（限流/验证码/锁定）、未授权访问 403、签名 URL 伪造拒绝、XSS payload、上传恶意文件 |
| 防呆测试 | — | 空值/超长/非法字符/重复提交/并发提交（Redis 分布式锁校验） |
| 压力测试 | JMeter | 文章详情页 500 并发、分片上传大文件、AI 对话流式并发、搜索结果 |
| 联调 | 前后端 | 按 Stage.md 每阶段完成标准逐项验收 |

## 附录 A：与 BackEnd.md 的差异说明

| 需求点 | BackEnd.md 原文 | 本方案处理 |
| --- | --- | --- |
| 登录账号 | 只设置一个密码（cryptex），默认 heyqing | 仅输入密码（用户名内部固定 `cryptex`），密码默认 `heyqing2aether`（用户确认），仅 dev 生效 |
| 文章独立风格建表 | "建一个还是多个你考虑清楚" | 样式独立成表（§6.1） |
| 云存储 | "oss 等有更推荐的也可以" | 阿里云 OSS（用户确认），抽象接口可扩展 |
| agent 语言 | "java 不好写就使用 python" | Python FastAPI 独立服务（§7.2） |
| AI 使用限制 | 未提及 | 新增每 IP 每分钟 10 条、每天 50 条限制（§7.5） |

## 附录 B：遗留问题

1. ~~easy-captcha 兼容性~~：已确认与 Boot 4 冲突（javax.servlet-api），按预案切换 Hutool Captcha（§1）；OSS 分片上传需真实凭据联调，留待阶段 9 部署环境验证
2. ip2region xdb 数据文件定期更新策略（随镜像打包 + 定时拉取）
3. DeepSeek 供应商为 DeepSeek 时的 SSRF 面（仅出站 API 调用，无回源，风险低）
4. 文章搜索 LIKE 通配符未转义（2026-08-27 测试记录，低危）：`%`/`_` 作为搜索关键词时会被解释为 SQL LIKE 通配符（仅影响搜索语义，全参数绑定无注入风险）；后续在关键词处统一 `escape` 处理，随搜索优化一并落地
