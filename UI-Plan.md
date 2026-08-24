# Aether 前端 UI 开发文档（UI-Plan）

> 本文档是基于 UI.md（原始需求，保留不动）生成的**详细前端开发方案**，作为前端开发的唯一依据。
> 参考站：eyeondesign.aiga.org（杂志编辑风）。参考截图已逐张分析（直接查看 + Ollama 视觉模型），结论已融入各页面设计。
> 文档版本：v1.0（2026-08-24）
>
> ⚠️ **待确认项**：设计基调（§2）当前为**默认方案 A（占位）**，开发阶段将生成多套基调 HTML 展示页（配色/字体/版式对比）供站长选定后定稿；LOGO 方案（§3）随基调确认后生成。

## 1 技术选型

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Next.js | 16.3.x | App Router + Turbopack；`basePath: '/aether'`（path 部署） |
| React / TypeScript | 19 / 5.x | 严格模式 |
| Tailwind CSS | 4.x | 设计 token 落地（CSS Variables） |
| shadcn/ui | 最新 | 基础组件（Button/Card/Sheet/Dialog/Drawer/Input/Select/Tabs…） |
| magicui | 最新 | 动效组件（清单见 §7） |
| framer-motion | 最新 | 动画引擎（scroll 驱动、spring、AnimatePresence） |
| next-themes | 最新 | light/night 主题切换 |
| next-intl | 最新 | 中简/英双语（**无语言路由前缀，站内点击切换**，`localePrefix: 'never'`） |
| ECharts | 最新 | 管理端用户地域分布地图 |
| ArtPlayer | 最新 | 视频播放（倍速/清晰度/章节/画中画） |
| DOMPurify | 最新 | 富文本渲染前 sanitize |
| 包管理器 | npm | 用户指定（Windows 环境） |

## 2 设计系统

### 2.1 设计基调（默认方案 A，待多套展示后确认）

继承参考站"杂志编辑风"：暖纸底色、超大号粗体标题、高饱和强调色分区、卡片网格、极简汉堡导航；配色按"以太"气质（星界/流动/静谧）定制为**暖纸白 + 墨黑 + 以太金**。

> **确认流程**：开发阶段（Stage 阶段 2）先产出 3-5 套基调 HTML 展示页（不同配色/字体组合的同一主页版式），站长选定后本节定稿，全站 tokens 随之锁定。

### 2.2 颜色 Tokens（light / night 两套 CSS 变量，方案 A 占位值）

```css
:root {
  /* 中性 */
  --background: #F7F4EC;   /* 暖纸白（页面底） */
  --foreground: #1A1B1F;   /* 墨黑（文字） */
  --card: #FFFFFF;         /* 卡片底 */
  --muted: #6E6F76;        /* 次级文字 */
  --border: #E3DED0;       /* 描边 */
  /* 主色与强调 */
  --primary: #1A1B1F;      /* 主色（按钮/链接） */
  --accent: #E8C94A;       /* 以太金（强调分区/悬停反色/hover） */
  --highlight: #FFE566;    /* 荧光高亮（highlighter 组件） */
  /* 功能色 */
  --success: #3A9B5D;
  --danger: #D64545;
  /* 圆角 */
  --radius-sm: 4px;  --radius-md: 8px;  --radius-lg: 12px;  --radius-pill: 999px;
}

.dark {
  --background: #0F1013;   /* 深空底 */
  --foreground: #EDEBE3;   /* 暖白前景 */
  --card: #17181C;
  --muted: #9A9BA1;
  --border: #26272C;
  --primary: #EDEBE3;
  --accent: #F2CE4F;       /* dark 下同系金 */
  --highlight: #C9A92C;
  --success: #4CAF73;
  --danger: #E05B5B;
}
```

### 2.3 字体（方案 A）

| 用途 | 拉丁 | 中文 | 说明 |
| --- | --- | --- | --- |
| 标题 | Fraunces（衬线，编辑感） | Noto Serif SC 900 | `--font-display` 变量驱动，文章独立样式可覆盖 |
| 正文 | Inter | Noto Sans SC | `--font-body` |
| 代码 | JetBrains Mono | — | 代码块/行内代码 |

> 全部走 `next/font/google` 自托管（国内访问稳定），fallback 系统字体。

### 2.4 排版与间距

| Token | 值 |
| --- | --- |
| 字号阶梯 | 12 / 14 / 16 / 18 / 20 / 24 / 30 / 36 / 48 / 64（px）；正文 16，文章内容区默认 17-18（可配） |
| 行高 | 正文 1.7，标题 1.15 |
| 间距 | 4px 基准：4 / 8 / 12 / 16 / 24 / 32 / 48 / 64 / 96 |
| 容器 | `max-w-6xl`（1152px），文章内容区 720px |
| 圆角 | 克制：4（小元素）/ 8（图片）/ 12（卡片）/ 999（pill 按钮） |
| 阴影 | light：`0 2px 12px rgba(26,27,31,.06)`；dark：`0 2px 12px rgba(0,0,0,.4)` |

### 2.5 动效规范

| 场景 | 时长 | 缓动 |
| --- | --- | --- |
| 微交互（hover/点击反馈） | 150ms | `cubic-bezier(0.22, 1, 0.36, 1)` |
| 标准过渡（卡片/面板） | 300ms | 同上 |
| 抽屉/侧边栏 | 500ms | 同上 |
| 页面转场 | 800ms | 同上 |
| spring 动画 | — | `{ stiffness: 160, damping: 25 }` |

- **全站尊重 `prefers-reduced-motion`**：开启时禁用非必要动画，直接呈现最终状态
- 滚动驱动动画统一用 framer-motion `useScroll` + `useTransform`（见 §8.1）

### 2.6 全局样式规则（对应参考站交互）

```css
/* 全站鼠标选择事件效果统一（article-03 参考图） */
::selection {
  background: var(--accent);
  color: var(--foreground);
}

/* 卡片悬停反色规则（参考站长截图核心特征）：
   hover 时整卡背景切换为 accent，前景反色 */
.hover-card {
  background: var(--card);
  color: var(--foreground);
  transition: background-color 300ms cubic-bezier(0.22, 1, 0.36, 1),
              color 300ms cubic-bezier(0.22, 1, 0.36, 1);
}
.hover-card:hover {
  background: var(--accent);   /* dark 模式由 CSS 变量自动适配 */
  color: var(--background);
}
```

## 3 LOGO 方案（草案，待基调确认后生成具体方案）

> ⚠️ 本节为方向性草案：设计基调（§2）经多套 HTML 展示确认后，再按选定基调生成具体 LOGO 方案与 ComfyUI prompt，届时更新本节并生成样例供确认。

### 3.1 方向要点

- 主题：以太（Aether）——古典哲学第五元素、星界之气、流动与静谧
- 元素：发光圆环/星云光晕 + 暖金主色（与选定基调 accent 一致）
- 形态：① web 图片 LOGO（ComfyUI 生成，给出 prompt，多尺寸 + 透明底）；② 文字 LOGO（"Aether" SVG 字标 + 描边流光/字母悬浮动画，light/dark 自适应）；③ JS 加载动画（**以太光环**：同心圆环由闭到开、由暗到亮"苏醒"动画，呼应参考站"眼睛由闭到睁"，用于 loading 与首页版权区滚动展开 LOGO）；④ favicon / og-image / 管理端小标规格清单

### 3.2 占位 ComfyUI prompt 模板（基调确认后精修）

```text
Positive prompt: minimalist logo, ethereal glowing ring, concentric orbital rings,
soft nebula halo, warm golden light on deep ink background,
ancient Greek "aether" fifth element concept, elegant line art, vector style,
centered composition, high contrast, clean edges, no text
Negative prompt: text, letters, watermark, photorealistic, busy background,
blurry, low quality, multiple objects
```

## 4 工程结构与路由

### 4.1 目录结构（单 URL 双语，无语言路由段）

```text
frontend/
  next.config.ts              # basePath: '/aether'
  src/
    app/
      (site)/                 # 用户端（中英双语由 next-intl 站内切换，URL 不变）
        layout.tsx            # 用户端布局（Nav/Footer/主题/i18n 注入）
        page.tsx              # 主页
        articles/page.tsx     # 文章列表
        articles/[id]/page.tsx
        albums/page.tsx
        albums/[id]/page.tsx
        videos/page.tsx
        videos/[id]/page.tsx
        music/page.tsx
        music/albums/[id]/page.tsx
        books/page.tsx
        books/[id]/page.tsx
        announcements/page.tsx
        announcements/[id]/page.tsx
        search/page.tsx       # 搜索结果页（文章/音乐分 tab）
        not-found.tsx
      cryptex/                # 管理端（仅中文，独立布局，新窗口打开）
        login/page.tsx
        (admin)/
          layout.tsx          # 侧栏 + 顶栏（JWT 校验，未登录跳 login）
          dashboard/page.tsx
          articles/ styles/ categories/ tags/
          albums/ videos/ music/ books/
          announcements/ subscribers/ surveys/
          stats/ logs/ ai-config/
    components/
      ui/                     # shadcn/ui
      magicui/                # magicui 组件
      layout/                 # Navbar/Footer/Breadcrumb/AetherRing
      article/ music/ book/ album/ video/ announce/ subscribe/ assistant/
      admin/                  # 管理端业务组件
    lib/
      api/                    # API client（fetch 封装 + 签名 URL 刷新）
      i18n/                   # next-intl 配置（localePrefix: 'never'）
      effect/                 # 音乐特效渲染引擎（MusicVisualizer）
      utils/
    messages/                 # zh.json / en.json
    proxy.ts                  # 管理端路由 JWT 校验（Next 16 已将 middleware 更名为 proxy）
  public/                     # robots.txt、favicon 等静态物
```

### 4.2 路由表与权限

| 路由（外部） | 页面 | 权限 |
| --- | --- | --- |
| /aether/ 及全部子路径 | 用户端页面（单 URL，语言站内切换） | 公开 |
| /aether/cryptex/login | 管理端登录 | 公开（已登录则跳 dashboard） |
| /aether/cryptex/* | 管理端 | 需 JWT（middleware 校验，失败重定向 login） |

- **无 `/zh` `/en` 语言路由**：语言由 LocaleSwitcher 站内切换（localStorage + Cookie 持久化，URL 保持不变）
- 管理端新窗口打开：用户端右上角入口 `<a href="/aether/cryptex" target="_blank" rel="noopener">`

## 5 全局组件

| 组件 | 说明 |
| --- | --- |
| Navbar | 左上角汉堡按钮（点击展开全屏 Sheet 导航：文章/图集/视频/音乐/书籍/公告/分类入口，菜单项 blur-fade 级联入场，参考 home-03）；中间文字 LOGO；右上角：AnimatedThemeToggler（主题）、语言切换、订阅按钮、GitHub 图标、管理端入口 |
| Footer | 版权行 + 底部滚动展开的 AetherRing LOGO（§3.1-③） |
| Breadcrumb | 全站完整面包屑：首页 / 模块 / 详情（含当前页，aria-label 规范） |
| ThemeProvider | next-themes：light/night，localStorage 记忆 + `prefers-color-scheme` 兜底 |
| LocaleSwitcher | 中文简/EN 切换按钮：切换后 URL 不变，仅重渲染界面文字（next-intl `setRequestLocale` + Cookie 持久化） |
| ProtectedImage | 防下载图片组件（§9.2）：统一 contextmenu/dragstart 拦截 + 签名 URL 加载 + 过期自动刷新 |
| ProtectedMedia | 视频/音频容器：签名 URL + Referer 白名单由后端保障（§9.2） |
| HoverCard | 悬停反色卡片基类（§2.6 样式） |
| SseChatView | SSE 流式对话渲染（AI 助手 tab 复用） |

## 6 逐页面设计

### 6.1 主页（参考 home-01 长截图，板块自上而下）

| # | 板块 | 设计 |
| --- | --- | --- |
| 1 | 导航 | 见 §5 Navbar |
| 2 | Hero 区 | 大张 LOGO 样式区：图片 LOGO（§3）为底 + `kinetic-text` 渲染 "AETHER" 字标（逐字字重动画）+ `morphing-text` 轮换项目口号（中英双语词条）；**鼠标悬浮时浮现项目信息面板**（简介/定位/入口链接，参考 home-02 的 hover 信息展示） |
| 3 | 公告区 | 顺时针轮播最近 3-5 条公告（公告/动态/新闻混合，`GET /aether/api/v1/announcements/latest`）；自定义 Marquee（循环滚动 + 点击暂停）+ 标题关键词 `highlighter` 荧光标注 |
| 4 | 热门文章 | 6-10 条，`GET /articles/hot`；卡片网格，**不显示创建时间**；hover 整卡反色（§2.6）；排名数字用 kinetic-text；卡片带 interactive-hover-button "阅读"；后端 `hot_order` 可自定义排序 |
| 5 | 推荐书籍 | 3-4 本并排（参考 book.png：封面微微缩小 + 书名 + 作者 + 一句话介绍或分类标签）；封面 `pixel-image` 懒加载像素过渡；入场 `blur-fade` 级联；hover 反色同热门文章 |
| 6 | 推荐图集 | 3-4 个，`lens` 放大镜卡片（见 §6.4） |
| 7 | 推荐音乐/专辑 | 3-4 个（封面 + 歌名 + 歌手/合集名），hover 反色 |
| 8 | 版权区 | 版权文字 + 下方 AetherRing 滚动展开 LOGO（§3.1-③） |

### 6.2 文章列表 / 搜索页

- 列表卡片：封面图（ProtectedImage）+ 标题 + 简介 + 分类标签 + 阅读数（无创建时间）；hover 反色
- 筛选：分类/标签下拉 + 排序（最新/热门）
- 搜索页（`/aether/search?q=`）：仅文章可搜索（标题/简介/内容），音乐搜索独立入口分 tab 展示；面包屑：首页 / 搜索

### 6.3 文章详情（参考 article-01 ~ article-04）

| 区域 | 设计 |
| --- | --- |
| 页面结构 | 标题（大号衬线）→ 分类/标签/阅读数 → 封面 → 正文；面包屑：首页 / 文章 / 标题 |
| 正文渲染 | `content_html` 经 DOMPurify sanitize 后渲染；注入文章独立样式（article_style.style_json → CSS Variables 容器内联，覆盖默认排版）；排版参数：字号/行高/段间距/首行缩进/主题色/衬线开关/内容区宽度 |
| 吸顶标题栏 | 滚动页面 5%-8% 时顶部浮现（§8.1，参考 article-02 横幅效果——本项目用 accent 横幅 + 标题文字） |
| RELATED ARTICLES | 文章末尾 + 网页最右侧竖排透明文字按钮（§8.2，参考 article-03/04） |
| 阅读数 | 详情接口自动计数（后端 IP 去重） |

### 6.4 图集列表与详情（参考 photo-01）

| 区域 | 设计 |
| --- | --- |
| 列表卡片 | `lens` 卡片：封面（hover 放大镜效果）+ 题目 + 简介；**去除参考图 "Let's go / Another time" 按钮**，点击卡片直接跳转图集详情；入场 blur-fade |
| 详情页 | 封面头部 + 介绍 + 图片瀑布流：`blur-fade` 主题框架（`columns-2 sm:columns-3` 瀑布流）+ 图片 `pixel-image` 像素过渡懒加载；面包屑：首页 / 图集 / 标题 |
| 图片预览 | 点击图片打开预览层（不可下载）：悬浮显示图片信息（大小必显，标题/介绍可选）；加载过程用 `pixel-image`；关闭 ESC/遮罩 |

### 6.5 视频列表与详情

- 列表：封面 + 标题 + 时长（后端探测）卡片，hover 反色
- 详情：ArtPlayer 播放器（倍速、清晰度、**关键时间节点**右侧列表点击跳转、画中画）；下方标题/介绍；签名 URL 播放（ProtectedMedia）
- 面包屑：首页 / 视频 / 标题

### 6.6 音乐（详情页 + 悬浮 tab + 歌词同步）

| 区域 | 设计 |
| --- | --- |
| 详情页 | 专辑/单曲封面 + 歌手 + 歌词（滚动 + 当前行高亮）+ 进度条 + 音量；背景与主体为 **MusicVisualizer AI 特效层**（§8.4） |
| 合集页 | 自定义合集 / 固定合集（含认证信息展示）分组列表；支持单曲搜索（独立于文章搜索） |
| 悬浮播放 | AI 助手悬浮窗内"音乐"tab（§8.4）：封面缩略 + 歌名 + 播放/暂停/上一首/下一首/退出；切歌后悬浮窗可收起继续播放（后台 Audio 实例全局单例） |

**歌词不同步解决方案**（后端存储与对齐方案见 BackEnd-Plan §7.6）：

| 场景 | 前端渲染策略 |
| --- | --- |
| LRC 时间轴 | 解析 `[mm:ss.xx]` 逐行同步滚动，当前行 accent 高亮 + 自动滚到可视区中部 |
| 纯文本歌词 | 按"总行数 ÷ 总时长"均分滚动（粗略同步，兜底） |
| 全局偏移 | 应用 `music.lyric_offset`（毫秒，正负可调）统一前移/后移全部行时间 |
| 手动打点 | 管理端歌词编辑器播放时逐行点击"标记当前行"生成时间轴（§6.11） |

### 6.7 书籍列表与阅读器

| 区域 | 设计 |
| --- | --- |
| 列表 | 封面 + 书名 + 作者 + 一句话介绍或分类标签（同参考 book.png 版式） |
| 详情 | 封面 + 简介 + 章节目录（章-节两级树） |
| 阅读器 | 第一页封面 → framer-motion `rotateY` 翻页动画 → **第二页版权声明**（文案模板由 `book.ownership_type` 决定：1 本人——"本书为本站原创作品，版权归作者所有…"；2 他人出版——"本书版权归原作者/出版社所有，本站仅作分享展示，请支持正版…"）；目录抽屉（章/节树）、字号/行距/阅读主题（仿纸色夜间模式）设置、阅读进度存 localStorage、章节锚点与 ←/→ 键盘翻页、段首空两格与段间半行距由后端排版保证 |

### 6.8 公告

- 列表：分类 tab（公告/动态/新闻）+ 卡片列表；详情：标题 + 时间 + 图文内容；面包屑：首页 / 公告 / 标题

### 6.9 订阅弹窗与问卷

| 步骤 | 设计 |
| --- | --- |
| 入口 | 右上角订阅按钮（`cool-mode` 粒子点击效果）→ 屏幕中央 Modal |
| 订阅 | 邮箱输入（前端格式校验）→ 提交 → 该 IP 已提交的邮箱列表展示（`GET /subscribe/emails`）→ 提示问卷 |
| 问卷 | 可跳过；选项来自 `GET /survey-options`（年龄段/性别/职业/兴趣爱好）；**一个 IP 一份**（重复提交返回 30702）；**可修改 2 次**（第 3 次返回 30703 并展示文案"修改次数已用完"） |

### 6.10 AI 助手悬浮窗

| 项 | 设计 |
| --- | --- |
| 注入条件 | `GET /ai/config` 返回 `enabled=true` 才渲染（管理端可关闭） |
| 外观 | 右下角圆形 LOGO 按钮（图片 LOGO 512）；点击展开悬浮窗（宽 ~380px） |
| 唤醒 | `CTRL + 空格` 唤醒/隐藏（焦点在 input/textarea/contenteditable 时忽略，避免输入法冲突） |
| 自动收起 | 无交互（mousemove/keydown 重置计时）约 5 分钟 → 收起至屏幕右边缘只露 40px 图标，点击重新展开；ESC 可关闭 |
| 内容 | 两个 tab：**对话**（SSE 流式渲染 SseChatView，会话 UUID 存 localStorage，历史取服务端）与**音乐**（§6.6 悬浮播放器）；达到每日限额时展示"今日使用次数已达上限"（30803 文案） |

### 6.11 管理端（cryptex，仅中文，新窗口）

| 页面 | 设计 |
| --- | --- |
| 登录页 `/aether/cryptex/login` | 背景文字浪（Canvas 字符矩阵 "AETHER/以太" + 正弦波浪位移）；中央卡片：**仅一个密码输入框**（即 cryptex，无用户名输入）+ （失败 3 次后出现）图形验证码；登录失败提示统一为"密码错误"；成功跳 dashboard |
| 框架 | 左侧栏（内容模块 + 统计 + 日志 + AI 配置）+ 顶栏（面包屑 + 登出）；middleware 校验 JWT，401 → 登录页 |
| 文章管理 | 列表（草稿/已发布/热度）；编辑器三模式：**HTML / Markdown / LaTeX**（源编辑 + 实时预览面板，预览渲染与用户端一致）；封面/简介/分类标签（AI 推荐一键采纳）；独立样式绑定（样式编辑器：字体/字号/行高/段间距/首行缩进/主题色 + 预览）；热度设置（isHot + hotOrder） |
| 图集/视频管理 | 上传控件**明确展示两套存储选择（本地/OSS）**；上传走分片（进度条 + 断点续传 + 秒传提示）；视频管理含关键时间节点编辑 + ArtPlayer 预览 |
| 音乐管理 | 上传音频/封面；**歌词编辑器**（LRC/纯文本双格式 + 播放试听 + 打点校准"标记当前行" + `lyric_offset` 全局偏移微调实时预览）；**特效生成按钮**（调 `/admin/music/{id}/effect/generate`）+ **内置特效预览播放器**（生成后立即以真实音频试听 EffectConfig 效果，支持调参/重生成/手工微调） |
| 书籍管理 | 上传 txt（分片上传）→ 触发 AI 分章 → 分章任务进度（解析中/待确认）→ **分章预览编辑器**（章节树可增删改、可合并拆分，确认后落库）→ 章节内容预览（分段排版效果） |
| 公告/订阅管理 | 公告 CRUD（类型/置顶）；订阅列表/退订；问卷统计（年龄段/性别/职业/兴趣分布图表） |
| 统计页 | 概览卡片（今日访问/阅读/订阅趋势折线）+ **ECharts 用户地域分布地图**（`/admin/stats/visitor-distribution`，地区人数越多颜色越重——连续色阶映射；后端已按"到市则市、到不了市则省"降级聚合，见 BackEnd-Plan §4.6） |
| 日志页 | 操作日志表格（模块/操作/路径/结果/IP/耗时过滤） |
| AI 配置页 | 场景（chat/effect/agent/classify）× 厂商（Ollama/DeepSeek）× 模型/温度/开关；**对话限额**（每分钟/每天）配置 |

## 7 magicui 组件映射清单

| 组件 | 使用位置 | 安装命令 |
| --- | --- | --- |
| animated-theme-toggler | Navbar 主题切换（clip-path circle，origin=按钮位置） | `npx shadcn@latest add @magicui/animated-theme-toggler` |
| kinetic-text | Hero "AETHER" 字标、热门文章排名数字 | `npx shadcn@latest add @magicui/kinetic-text` |
| morphing-text | Hero 口号轮换、RELATED ARTICLES 按钮文字 | `npx shadcn@latest add @magicui/morphing-text` |
| text-3d-flip | RELATED ARTICLES 竖排按钮 hover 3D 翻转 | `npx shadcn@latest add @magicui/text-3d-flip` |
| highlighter | 公告轮播标题关键词荧光标注 | `npx shadcn@latest add @magicui/highlighter` |
| lens | 图集卡片放大镜（去按钮，点击跳详情） | `npx shadcn@latest add @magicui/lens` |
| blur-fade | 全站列表/区块入场、汉堡菜单项级联 | `npx shadcn@latest add @magicui/blur-fade` |
| pixel-image | 封面/图片懒加载像素过渡 | `npx shadcn@latest add @magicui/pixel-image` |
| cool-mode | 订阅按钮点击粒子 | `npx shadcn@latest add @magicui/cool-mode` |
| interactive-hover-button | 热门文章卡片"阅读"按钮 | `npx shadcn@latest add @magicui/interactive-hover-button` |
| comic-text | 可选：公告彩蛋强视觉场景，**不硬塞** | `npx shadcn@latest add @magicui/comic-text` |

原则：组件风格统一适配设计系统 tokens（色调跟随 CSS 变量），魔法效果用于增强、不喧宾夺主。

## 8 关键交互难点方案

### 8.1 文章详情吸顶标题栏（滚动 5%-8%）

- framer-motion `useScroll({ target: containerRef })` 取 `scrollYProgress`（0~1 比值，天然满足"整个页面的 5%-8%"）
- `useTransform(scrollYProgress, [0.05, 0.08], [0, 1])` 映射标题栏 opacity，`y: -100% → 0` 滑入
- 标题栏：accent 横幅 + 文章标题 + 分类面包屑 + 阅读进度条；`position: fixed top-0`，正文容器 `padding-top` 预留防跳变；`prefers-reduced-motion` 时直接显示

```tsx
"use client"
const { scrollYProgress } = useScroll({ target: articleRef });
const opacity = useTransform(scrollYProgress, [0.05, 0.08], [0, 1]);
const y = useTransform(scrollYProgress, [0.05, 0.08], ["-100%", "0%"]);
return (
  <motion.header className="fixed inset-x-0 top-0 z-40"
    style={{ opacity, y, background: "var(--accent)" }}>
    {/* 标题 + 面包屑 + 阅读进度条 */}
  </motion.header>
);
```

### 8.2 RELATED ARTICLES 侧边栏

- 桌面端（≥lg）：屏幕右侧 `fixed right-4 top-1/2` 竖排透明文字卡片（`writing-mode: vertical-rl` + Text3DFlip，文字在"其他文章 / RELATED ARTICLES"间 MorphingText 轮换）；点击后 AnimatePresence 滑出右侧抽屉（宽 360px：相关文章列表 blur-fade 级联，封面+标题+日期）；遮罩/ESC 关闭
- 移动端（<lg）：不渲染侧边栏，正文底部"其他文章推荐"区块
- 数据：`GET /articles/{id}/related`（同分类优先 + 最新兜底）

### 8.3 文章独立样式渲染

- 后端 `article_style.style_json`（键见 BackEnd-Plan §6.3）→ 前端转换为 CSS Variables 注入文章容器 `<article style={{...cssVars}}>`
- 默认样式（无绑定）：读取全局默认 article_style（is_default=1），不存在则用设计系统默认排版

```tsx
// style_json → CSS 变量映射（示例）
const cssVars = {
  "--art-font": style.fontFamily,
  "--art-font-size": `${style.fontSize}px`,
  "--art-line-height": style.lineHeight,
  "--art-para-spacing": `${style.paragraphSpacing}px`,
  "--art-indent": style.firstLineIndent,
  "--art-width": `${style.contentWidth}px`,
  "--art-theme": style.themeColor,
} as React.CSSProperties;
```

### 8.4 音乐 AI 特效渲染引擎（三层架构）

| 层 | 内容 | 说明 |
| --- | --- | --- |
| 1 配置层 | `music.effect_config`（EffectConfig JSON，BackEnd-Plan §7.3） | AI 生成 + 人工微调；含 palette/layers/background/transition |
| 2 渲染层 | `MusicVisualizer` 组件（**固定引擎，永远存在**） | `AudioContext.createAnalyser()` 取时域/频域数据 → rAF 按 config 逐层渲染 Canvas 2D；粒子计算放 OffscreenCanvas + Worker；不引 Three/PixiJS 控制包体积；`layers[].bind` 映射：amplitude→响度、freqBand→频段、beat→节拍脉冲；切歌/换主题参数插值过渡（transition.duration）；移动端粒子数减半、限 30fps |
| 3 沙箱层 | `effect_config.sandboxCode`（仅特殊场景） | AI 生成的 JS 在 `<iframe sandbox="allow-scripts">` 内执行，postMessage 通信，只暴露受限 `effectApi`（draw/fillRect/arc/audioLevel）；无网络、无 DOM 访问、3s 超时；执行异常自动回退 config 渲染 |

```tsx
// MusicVisualizer 数据流要点
const ctx = new AudioContext();
const analyser = ctx.createAnalyser();
analyser.fftSize = 256;
const src = ctx.createMediaElementSource(audioEl);
src.connect(analyser); analyser.connect(ctx.destination);
// rAF 循环: analyser.getByteTimeDomainData / getByteFrequencyData → 按 layers 渲染
```

### 8.5 AI 助手悬浮窗交互

- `keydown` 监听 `CTRL+Space`（排除 input/textarea/contenteditable 焦点）；后端 `ai_config.enabled=false` 时整个组件不渲染
- 5 分钟无交互收起：mousemove/keydown 重置 5min 定时器 → 收起动画至右边缘 40px 露出
- 对话 SSE：`fetch + ReadableStream` 解析 `data:` 帧增量渲染；错误/断线自动重连一次；限额触发展示 30803/10003 文案

### 8.6 书籍阅读器翻页与版权页

- 封面（第一页）→ 点击/键盘 → framer-motion `rotateY: 180deg` 翻页 → 第二页版权声明（`book.ownership_type` 模板：1 本人 / 2 他人出版）；从第二页进入目录
- 阅读设置持久化 localStorage：`{fontSize, lineHeight, theme}`；章节锚点 `#chapter-{id}` 与 URL 同步；←/→ 翻章

### 8.7 管理端登录文字浪背景

- Canvas 字符矩阵（"AETHER / 以太"）+ 每列字符 y 轴正弦位移（不同相位/波速），深色底 + accent 半透明字符；`prefers-reduced-motion` 时静态

## 9 安全限制

### 9.1 禁爬与 SEO

- `public/robots.txt`（Next 下用 `app/robots.ts`）：`User-agent: * Disallow: /aether/`（生产 nginx 直接托管）
- SEO 章节收缩为：`generateMetadata`（title/description 双语）、Open Graph 标签规范化、canonical；**不做 sitemap**（与禁爬一致）

### 9.2 媒体防下载（前端防线，核心在后端）

- `ProtectedImage`：统一拦截 `contextmenu`/`dragstart`、`user-select: none`、`-webkit-touch-callout: none`；`src` 一律使用后端签名 URL（VO 中的 coverUrl/fileUrl 字段）；签名过期（HTTP 403/410）自动重新拉取刷新
- 视频/音频：ProtectedMedia 容器 + ArtPlayer/Audio 只接收签名 URL；页面不暴露任何原始对象存储地址
- 后端核心防线（签名 URL + Referer 白名单）见 BackEnd-Plan §4.4

### 9.3 XSS

- 所有富文本（文章/公告/书籍章节）渲染前 DOMPurify sanitize（白名单与后端 jsoup 对齐）
- AI 沙箱代码仅限 iframe sandbox 执行（§8.4 第三层）

### 9.4 管理端鉴权

- proxy.ts（Next 16 已用 proxy 替代 middleware）：`/aether/cryptex/*`（login 除外）校验 Access Token（存在 + 未过期），失败 302 `/aether/cryptex/login`；API 401 时 api client 自动走 refresh 流程（`/auth/refresh`），refresh 失败登出

## 10 响应式设计

| 断点 | 适配策略 |
| --- | --- |
| ≥1024px（lg） | 完整版式：RELATED ARTICLES 侧边栏、4 列书籍推荐 |
| 768-1024px（md） | 卡片 3 列；侧边栏隐藏（正文底部推荐区块） |
| <768px（sm） | 单列；Hero 字标缩小；汉堡导航全屏；管理端表格转卡片；MusicVisualizer 粒子减半 + 30fps |
| 触摸设备 | hover 反色改为 tap 反馈；CTRL+Space 悬浮窗改为点按按钮 |

## 11 SEO 与 i18n

- **i18n**：next-intl，**无语言路由前缀**（`localePrefix: 'never'`）——同一 URL 站内切换中简/英文；语言选择持久化（Cookie + localStorage），首访按浏览器语言自动选择；词条文件 `messages/zh.json`、`messages/en.json`（**全部系统文字双语**：导航/按钮/提示/错误码文案映射；用户内容不翻译）；管理端仅中文
- **主题**：next-themes + AnimatedThemeToggler；无 JS 时由 `prefers-color-scheme` 兜底
- **元数据**：generateMetadata 输出 title/description/OG；canonical 指向本站当前 URL
- 错误码文案映射：如 30703 → "问卷修改次数已用完" / "Survey update limit reached"

## 12 附录

### 12.1 参考截图索引

| 截图 | 对应设计 |
| --- | --- |
| home-01 | 主页整体版式（§6.1） |
| home-02 | Hero hover 信息面板（§6.1-2） |
| home-03 | 汉堡菜单导航（§5 Navbar） |
| home-04 | 分区配色参考 |
| article-01/02 | 文章详情结构 + 吸顶标题栏（§6.3、§8.1） |
| article-03/04 | RELATED ARTICLES 侧边栏与选中效果（§8.2） |
| book.png | 书籍推荐卡片版式（§6.1-5） |
| photo-01 | Lens 图集卡片（§6.4） |

### 12.2 组件安装命令汇总

```bash
npx create-next-app@latest frontend --typescript --tailwind --app
npx shadcn@latest init
# magicui 组件（§7 清单）逐个安装，如：
npx shadcn@latest add @magicui/lens
# 基础依赖
npm install framer-motion next-themes next-intl dompurify echarts artplayer
```

### 12.3 待办与遗留问题

1. **设计基调确认**：生成 3-5 套基调 HTML 展示页（配色/字体/版式对比）供站长选定 → 定稿 §2 tokens → 生成 LOGO 方案（§3）→ 全站统一落地
2. 中文衬线标题字体 Noto Serif SC 体积较大，需子集化/按需加载（`next/font` display=swap）
3. ArtPlayer 清晰度切换依赖多码率源文件，站长上传单文件时该按钮隐藏（预留多码率扩展）
4. 歌词时间轴：LRC 优先 + 纯文本均分兜底 + 打点校准 + 全局偏移（BackEnd-Plan §7.6）；AI 自动对齐为可选增强
5. ECharts 中国地图 GeoJSON 数据源需随行政区划更新（打包内置）
