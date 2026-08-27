# Aether 项目目录说明

> 仅记录项目目录结构与各目录的功能作用；生成物目录（`node_modules`、`target`、`.next`、`.git`、`.idea`）不入库、不展开说明。

## 目录树

```text
Aether/
├── .claude/                          # Claude Code 工作区配置：Aether 测试体系
│   ├── agents/                       # 测试主代理定义（code-tester.md）
│   ├── commands/                     # 自定义命令（/test 全维度测试）
│   ├── hooks/                        # pre-push 测试门禁 hook（Claude Code 内拦截 push）
│   ├── skills/                       # 6 个测试维度 skill（backend/frontend/functional/security/comment/efficiency）
│   └── test-reports/                 # 测试报告存档
├── .githooks/                        # git 原生 pre-push 兜底 hook（终端手动 push 时拦截）
├── .github/                          # GitHub 平台配置
│   └── workflows/                    # CI 工作流：后端 mvn verify（dev profile）+ 前端构建
├── agent/                            # Python 分章服务（FastAPI 骨架：书籍分章、歌词时间轴对齐）
├── assets/                           # 根文档引用图片（README 截图、封面图）
├── backend/                          # Spring Boot 后端工程（Java 21 + Maven）
│   └── src/
│       ├── main/
│       │   ├── java/top/heyqing/aether/
│       │   │   ├── ai/               # AI 集成包（预留，阶段5实现）
│       │   │   ├── aspect/           # AOP 切面：操作日志注解与切面实现
│       │   │   ├── common/           # 通用组件：统一响应 Result、分页 PageResult、请求上下文、缓存抽象
│       │   │   ├── config/           # Spring 配置类：Security、JWT、存储、缓存、Jackson、数据种子
│       │   │   ├── constant/         # 常量定义（API 路径 ApiConst 等）
│       │   │   ├── controller/       # REST 控制器（按业务模块分包）
│       │   │   │   ├── admin/        #   管理端：文章/样式/分类管理
│       │   │   │   ├── article/      #   文章前台接口
│       │   │   │   ├── auth/         #   认证：登录/刷新/验证码
│       │   │   │   ├── storage/      #   存储：分片上传/合并/签名访问
│       │   │   │   └── ai|album|announcement|book|music|stats|subscribe|video/   # 预留模块
│       │   │   ├── exception/        # 业务异常 BusinessException 与全局异常处理
│       │   │   ├── interceptor/      # MVC 拦截器（访客日志记录 VisitorLogInterceptor）
│       │   │   ├── job/              # 定时任务（访客日统计 VisitorStatJob）
│       │   │   ├── model/            # 数据模型
│       │   │   │   ├── dto/          #   接口入参（Request）
│       │   │   │   ├── entity/       #   表实体（JPA Entity）
│       │   │   │   └── vo/           #   接口出参（View Object）
│       │   │   ├── repository/       # Spring Data JPA 数据访问层
│       │   │   ├── security/         # 安全组件：JWT 签发/过滤、验证码、登录保护、认证失败处理
│       │   │   ├── service/          # 业务服务接口（按模块分包：article/auth/storage/visitor）
│       │   │   │   └── impl/         #   服务实现类（对应模块子包）
│       │   │   ├── storage/          # 存储抽象：本地/OSS 双实现、分片、秒传、合并、路由
│       │   │   └── util/             # 工具类：摘要、图片、IP 归属地、UA 解析、XSS 清洗
│       │   └── resources/            # 配置文件与静态资源
│       │       ├── db/               #   MySQL 建表脚本（schema-mysql.sql）
│       │       └── ip2region/        #   IP 归属地离线库（ip2region.xdb）
│       └── test/java/top/heyqing/aether/   # 集成测试（H2 内存库，dev profile）
├── deploy/                           # Docker 部署编排（docker-compose.dev.yml）
├── design/                           # 设计资产
│   └── moodboards/                   # 风格稿 HTML（方案A~E，LOGO 资产产出地）
├── frontend/                         # Next.js 16 前端工程
│   ├── messages/                     # i18n 文案（en.json / zh.json）
│   └── src/
│       ├── app/                      # App Router 路由：全局布局、站点页组 (site)、favicon/logo
│       │   └── (site)/               #   站点页面组：首页、文章列表、文章详情
│       ├── components/               # React 组件（按域分包）
│       │   ├── article/              #   文章：卡片、内容渲染、详情、筛选、相关推荐
│       │   ├── home/                 #   首页：Hero 区
│       │   ├── layout/               #   布局：导航栏、页脚、主题切换、语言切换、Logo
│       │   └── ui/                   #   通用 UI 组件（BlurFade 动效等）
│       ├── i18n/                     # 国际化配置（routing / request / navigation）
│       └── lib/api/                  # API 客户端封装（client 浏览器端 / server 服务端 / types 类型）
└── scripts/                          # 辅助脚本（setup-hooks.sh：安装 git 测试门禁 hook）
```

## 目录功能速查

| 目录 | 功能作用 |
| --- | --- |
| `.claude/` | Claude Code 专属工作区：测试体系（agent + 6 skill + /test 命令 + push 门禁 hook），与业务代码无关 |
| `.githooks/` | git 原生 pre-push hook 源文件，`scripts/setup-hooks.sh` 将其安装到 `.git/hooks/`，保证终端手动 push 也受测试门禁约束 |
| `.github/workflows/` | GitHub Actions CI：push/PR 时自动执行后端 `mvn verify`（dev profile）与前端构建 |
| `agent/` | Python FastAPI 分章服务：承担 Java 不便实现的 AI 任务（书籍 txt 自动分章、歌词时间轴对齐），仅内网可达 |
| `assets/` | 根文档（README 等）引用的图片资源，与运行时代码无关 |
| `backend/` | Spring Boot 后端工程：分层架构（controller → service → repository），含安全、存储双实现、访客统计、操作日志 |
| `backend/src/main/resources/` | 后端配置（application.yml / dev / local）、MySQL 建表 SQL、ip2region 离线库、logback 日志配置 |
| `backend/src/test/` | 集成测试（文章流、认证安全、登录保护、上传流、OSS 分片、访客流），H2 内存库运行 |
| `deploy/` | Docker Compose 编排文件，项目末期统一部署用（当前开发不依赖 Docker） |
| `design/moodboards/` | 设计阶段风格稿（羊皮卷方案D 等）与 LOGO 资产，前端视觉设计依据 |
| `frontend/` | Next.js 16 前端工程：App Router + i18n + 主题切换，通过 proxy 代理后端 API |
| `scripts/` | 开发辅助脚本：一键安装 git 测试门禁 hook |

## 根目录文档（非目录，说明）

| 文件 | 作用 |
| --- | --- |
| `Stage.md` | 阶段规划与开发规范（最高优先级依据） |
| `BackEnd-Plan.md` | 后端设计文档（API §5、表结构 §6、安全清单 §4） |
| `UI-Plan.md` | 前端设计文档 |
| `README.md` | 对外项目说明 |
| `CLAUDE.md` | Claude Code 协作规范（测试门禁、提交规范、文档同步） |
| `BackEnd.md` / `UI.md` | 早期设计文档 |
| `ProjectContent.md` | 本文件：项目目录结构说明 |

## 说明

1. 目录树的层级注释即该目录的功能作用；速查表仅对顶层目录做补充说明。
2. `backend/.idea`（IDEA 工程配置）、`backend/target`（Maven 构建产物）、`frontend/node_modules`、`frontend/.next` 均为生成物，由 `.gitignore` 排除，不参与版本管理。
3. 标注「预留」的目录（`ai/`、controller 下部分模块）为后续阶段规划模块，当前仅有 `.gitkeep` 占位。
