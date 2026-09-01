# Aether | 以太

> 本文档从使用者视角介绍 Aether 项目：有什么功能、怎么用、如何部署。

个人网站：**个人博客 + 多媒体内容管理 + 日常记录**。站长一人上传内容（文章、图集、视频、音乐、书籍、公告），游客自由浏览、订阅、与 AI 助手对话。

线上地址：https://www.heyqing.top/aether

| 类别 | 名称 | 备注 |
| :--: | :--: | :--: |
| 网站名称（英 \| 中） | Aether \| 以太 | 个人网站品牌 |
| 工程名称 | aether | — |
| 核心定位 | 个人博客 + 多媒体内容管理 + 日常记录 | 详见开发文档 |

## 功能特性

| 模块 | 功能 | 游客可用 | 说明 |
| --- | --- | --- | --- |
| 文章 | 博客文章、分类、标签、搜索（标题/简介/内容）、热门文章、每篇独立排版样式 | ✅ | 阅读数按 IP 24 小时去重统计 |
| 图集 | 相册图集、放大镜卡片、瀑布流浏览、图片预览（**不可下载**） | ✅ | 图片悬浮显示大小等参数信息 |
| 视频 | 在线播放、倍速、清晰度、关键时间节点跳转 | ✅ | ArtPlayer 播放器 |
| 音乐 | 单曲/自定义合集/固定合集（专辑认证）、歌词、AI 定制播放特效 | ✅ | 播放页 + AI 助手内悬浮播放；音乐不可下载 |
| 书籍 | 小说阅读、AI 自动分章分节、阅读器（目录/字号/阅读主题/进度记忆）、版权声明页 | ✅ | 支持出版图书与本人作品，txt 上传自动分章 |
| 公告 | 公告/动态/新闻，主页顺时针轮播 | ✅ | — |
| 订阅 | 邮箱订阅（文章与公告推送）、用户分布问卷调查（可跳过） | ✅ | 一个 IP 可订阅多个邮箱；问卷一个 IP 一份、最多修改 2 次 |
| AI 助手 | 站内智能对话（本地 Ollama / 云端 DeepSeek 双模型） | ✅ | 悬浮窗，`CTRL + 空格` 唤醒；每 IP 每分钟 10 条、每天 50 条 |
| 游客分析 | IP 地域定位（如"中国·陕西·西安"）、访问统计 | — | 站长为访客提供 |
| 管理端 | 全模块内容管理、双存储上传（本地/OSS）、AI 配置、用户地域分布地图、操作日志 | ❌ 站长专属 | 路由 `/aether/cryptex`，登录后使用 |
| 内容保护 | 全站禁止爬取；媒体文件签名 URL 防直链下载 | — | robots 全禁 |

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | Next.js 16.3（App Router、TypeScript、Turbopack）+ Tailwind CSS 4 + shadcn/ui + magicui + framer-motion + next-themes（light/night 主题）+ next-intl（中简/英双语）+ ECharts + ArtPlayer |
| 后端 | Java 21 + Spring Boot 4.1 + Spring Data JPA + MySQL 8.4 + Redis 8 + ip2region + LangChain4j |
| AI | 本地 Ollama（离线模型）+ 云端 DeepSeek；书籍分章 agent 由 Python 3.12 + FastAPI 独立服务承担 |
| 存储 | 本地磁盘 + 阿里云 OSS 双通道（分片上传/断点续传/秒传/零拷贝合并） |
| 部署 | Docker Compose（nginx + mysql + redis + backend + frontend + python-agent），path 部署 `/aether` |

## 使用指南（游客视角）

1. **浏览**：主页自上而下为 Hero 区 → 公告轮播 → 热门文章 → 推荐书籍 → 推荐图集 → 推荐音乐 → 版权区；左上角汉堡菜单进入各模块导航；右上角可切换主题/语言/订阅
2. **搜索**：文章可搜标题/简介/内容；音乐独立搜索；全站有完整面包屑
3. **订阅**：点击右上角订阅按钮 → 填写邮箱 → 可跳过或填写用户分布问卷（问卷仅一份，可修改 2 次）
4. **AI 助手**：右下角以太 LOGO 悬浮窗，`CTRL + 空格` 唤醒/隐藏；无操作约 5 分钟自动收起至屏幕边缘；内含对话与音乐播放两个标签页
5. **阅读**：文章页滚动 5%-8% 时顶部浮现标题栏；右侧 RELATED ARTICLES 可打开相关文章侧边栏；书籍第一页封面后为版权声明页
6. **内容保护**：全站图片/视频/音乐/书籍内容仅供在线浏览，不支持下载

## 本地开发指南

前置环境：JDK 21、Maven 3.9+、Node.js 22+、npm、（可选）Ollama（Docker 部署环境于项目末期配置）

```bash
# 0. 测试门禁初始化（首次克隆执行一次，注册 git 原生 pre-push 兜底）
bash scripts/setup-hooks.sh

# 1. 启动中间件（mysql/redis/ollama）
docker compose -f deploy/docker-compose.dev.yml up -d

# 2. 启动后端（context-path=/aether/api，Swagger 见 /aether/api/swagger-ui.html）
#    注意：spring-boot:run 默认 fork 独立 JVM，系统属性不继承——必须用插件参数
#    spring-boot.run.profiles；且后置 profile 优先（local 覆盖 dev 数据源 → 顺序 dev,local）
cd backend && mvn spring-boot:run "-Dspring-boot.run.profiles=dev,local"

# 3. 启动前端（basePath=/aether，开发地址 http://localhost:3000/aether）
cd frontend && npm install && npm run dev

# 4. 启动 python-agent（书籍分章，可选）
cd agent && uvicorn main:app --port 8000

# 5. 演示测试数据（可选，开发阶段走通页面用；上线前清理）
bash scripts/download-demo-media.sh   # 下载真实网络媒体到本地存储（图 7/视频 3/音频 3）
mysql -uroot -p aether < backend/src/main/resources/db/seed-demo-data.sql
# 清理：先执行 backend/src/main/resources/db/seed-demo-data-cleanup.sql
#       （保留分类/标签等字典数据），再 rm -rf backend/data/storage/files/seed/
```

管理端登录：地址 `/aether/cryptex/login`，**仅需输入密码**（即 cryptex），密码为环境变量 `AETHER_CRYPTEX` 配置（dev 环境默认 `heyqing2aether`，**生产环境必须修改**）。

## 测试门禁（开发必读）

- **git push 前必须完成 `/test` 全流程并通过**：在 Claude Code 中运行 `/test`（默认 6 维度：后端/前端/安全/注释/功能/效率；可指定如 `/test security`），全部通过后自动生成测试通过标记（24 小时有效），push 才会放行
- **push 含代码变更时必须先更新 Stage.md**（§1.4 提交记录表 + 当前阶段进度 + 遗留问题）：门禁 hook 强制校验推送范围内含 Stage.md 变更，否则阻止 push
- 双层拦截：Claude Code hook（拦截 Claude 执行的 push）+ git 原生 pre-push（拦截终端手动 push，需 `bash scripts/setup-hooks.sh` 初始化一次）
- 纯文档改动（.md/.txt/.gitignore）自动放行；紧急情况 `git push --no-verify` 跳过（需说明）
- 提交署名仅 dkb（不加 Claude Co-Authored-By），Conventional Commits 规范
- 测试体系详见 `.claude/`（code-tester agent + 6 维度 skill + /test 命令 + 门禁 hook），**与业务 agent/（Python 分章服务）无关**

## 部署指南

1. 配置环境变量（见 BackEnd-Plan §11.2：`AETHER_CRYPTEX`、数据库、Redis、JWT、OSS、DeepSeek、SMTP 等）
2. `docker compose -f deploy/docker-compose.yml up -d` 一键启动六服务（nginx/mysql/redis/backend/frontend/python-agent）
3. nginx 已内置 path 路由（`/aether/` 前端、`/aether/api/` 后端、`/aether/agent/` 分章服务），配置域名解析与 HTTPS 证书即可
4. 数据备份：每日凌晨自动备份（mysqldump + Redis RDB + 文件卷，保留 30 天）

注意事项：

- 生产环境部署后**必须先修改默认密码**（强度校验：≥12 位、含大小写+数字+特殊字符）
- 全站禁止爬虫收录（robots 全禁），属预期行为
- 大文件（视频 2GB / 音频 200MB / 图片 50MB）上传走分片 + 断点续传，nginx `client_max_body_size` 已放宽

## 目录结构

```text
aether/
├── backend/            # Java 后端（Spring Boot）
├── frontend/           # Next.js 前端（用户端 + 管理端）
├── agent/              # Python agent（书籍 AI 分章，业务服务）
├── deploy/             # docker-compose 与 nginx 配置
├── .claude/            # Claude Code 开发体系：测试门禁（agent/skill/hook，与业务 agent/ 无关）
├── .githooks/          # git 原生 pre-push 测试门禁兜底
├── scripts/            # 开发辅助脚本
├── assets/             # 设计参考素材
├── CLAUDE.md           # Claude Code 开发规范
├── README.md           # 本文档
├── UI-Plan.md          # 前端开发文档
├── BackEnd-Plan.md     # 后端开发文档
├── Stage.md            # 开发阶段文档
└── UI.md / BackEnd.md  # 原始需求文档（保留）
```

> 设计选型展示页 design/ 为开发文件，仅本地保留不入库。

## 文档索引

- [README.md](README.md)：项目介绍与使用指南（本文档）
- [UI-Plan.md](UI-Plan.md)：前端 UI 开发文档（技术选型/设计系统/逐页面设计/交互方案）
- [BackEnd-Plan.md](BackEnd-Plan.md)：后端开发文档（接口/表结构/安全/AI/存储/部署）
- [Stage.md](Stage.md)：开发规范与阶段划分
- [ProjectContent.md](ProjectContent.md)：项目目录结构说明
- [CLAUDE.md](CLAUDE.md)：Claude Code 开发规范（测试门禁/提交规范）
- [UI.md](UI.md) / [BackEnd.md](BackEnd.md)：原始需求文档（开发依据，保留不动）
