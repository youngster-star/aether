# Aether 前端 UI 开发文档（UI-Plan）

> 本文档是基于 UI.md（原始需求，保留不动）生成的**详细前端开发方案**，作为前端开发的唯一依据。
> 参考站：eyeondesign.aiga.org（杂志编辑风，供版式参考）。参考截图已逐张分析（直接查看 + Ollama 视觉模型），结论已融入各页面设计。
> 文档版本：v2.0（2026-08-31）
>
> ✅ **设计基调已确认（2026-08-24）**：站长从五套候选展示页（`design/moodboards/`，仅本地保留不入库）中选定**方案 D · 羊皮卷 — 古典文学**（全衬线、纸张纹理、装帧书脊封面），§2 设计系统与 §3 LOGO 方案已定稿。
>
> ✅ **v2.0 交互体系升级（2026-08-31，站长已确认）**：参考站 10 张截图逐张精读后重构交互层——① 新增 §2.7 **双主题色调系统**（每模块 Normal / Hover 两套色调，卡片 hover 整卡反色，每模块一色）；② 新增 §2.8 **全站微交互动画规范**（44 项清单，核心项附代码）；③ §8.2 RELATED ARTICLES 按参考站实测行为重做（竖排跳动按钮 + 左滑面板）；④ 装饰语言引入取景框角标 + 虚线框。

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

### 2.1 设计基调（✅ 已定稿：方案 D · 羊皮卷 — 古典文学；v2.0 装饰语言微调）

继承参考站"杂志编辑风"的**版式骨架**（超大标题、卡片网格、极简汉堡导航、高饱和分区、hover 双色调反色），配色与字体按"以太"气质定制为**羊皮卷古典文学风**：羊皮纸底、全衬线字体、墨褐 + 赭石 + 铜绿三色体系、旧书装帧质感（纸张纹理、书脊封面、扉页框线、卷号编号）。

- 气质关键词：旧书、文学、星界古典哲学源头（以太=第五元素）
- 展示页（仅本地保留，不入库）：`design/moodboards/style-d.html`（含全部交互演示与 light/night 双主题）
- **v2.0 装饰语言微调**：在双线饰线基础上，引入参考站的**取景框角标（四角 L 形短角线）与虚线描边框**——与羊皮卷"扉页框线"天然契合。应用范围：Hero 区、各板块容器、文章 Hero 图（取景框 = "裁切/装帧"隐喻）；双线饰线保留用于区块标题与分隔

### 2.2 颜色 Tokens（✅ 定稿：羊皮卷，light / night 两套 CSS 变量；v2.0 补分区色）

```css
:root {
  /* 中性 */
  --background: #F3EAD8;   /* 羊皮纸（页面底） */
  --foreground: #2B2118;   /* 墨褐（文字） */
  --card: #FBF5E8;         /* 书页卡底 */
  --muted: #7A6A55;        /* 褐灰（次级文字） */
  --border: #D9C9A8;       /* 淡褐（描边） */
  /* 主色与强调 */
  --primary: #2B2118;      /* 墨褐（按钮/链接主色，hover 反色） */
  --accent: #8C5B2D;       /* 赭石（强调/悬停反色/热门排名数字） */
  --accent-2: #4F6D5A;     /* 铜绿（副强调：公告 tag/认证/装饰线） */
  --highlight: #E8C98A;    /* 书签金（::selection 高亮/荧光标注） */
  /* 功能色 */
  --success: #4A7A5A;
  --danger: #B0442E;
  /* v2.0 分区色（双主题色调系统，见 §2.7）：hover 底色 + 反色文字 */
  --tint-article:  #8C5B2D;  --tint-article-on:  #F3EAD8;  /* 文章：赭石 */
  --tint-book:     #4F6D5A;  --tint-book-on:     #F3EAD8;  /* 书籍：铜绿 */
  --tint-album:    #7A3B2E;  --tint-album-on:    #F3EAD8;  /* 图集：旧书布面酒红 */
  --tint-music:    #3E5C76;  --tint-music-on:    #F3EAD8;  /* 音乐：钢笔墨水蓝 */
  --tint-announce: #B0442E;  --tint-announce-on: #F3EAD8;  /* 公告：印章朱 */
  /* 当前生效分区色（默认=文章，由 data-module 切换） */
  --tint: var(--tint-article);
  --tint-on: var(--tint-article-on);
  /* 圆角（克制：3 小元素 / 6 图片 / 8 卡片 / 12 大分区 / 999 pill） */
  --radius-sm: 3px;  --radius-md: 6px;  --radius-lg: 8px;  --radius-xl: 12px;  --radius-pill: 999px;
  /* 阴影（暖褐调） */
  --shadow: 0 2px 14px rgba(43, 33, 24, .10);
}

.dark {
  /* 深夜书房：烛光褐金 */
  --background: #1E1812;   /* 深褐黑底 */
  --foreground: #E8DCC3;   /* 烛光米白（前景） */
  --card: #262019;
  --muted: #A08D72;
  --border: #3A2F22;
  --primary: #E8DCC3;
  --accent: #C68B4E;       /* 烛光赭 */
  --accent-2: #6E8F79;     /* 旧铜绿 */
  --highlight: #B99A5B;
  --success: #6E8F79;
  --danger: #D9765C;
  /* v2.0 分区色（dark 组：提亮一档保证对比度，反色文字用深褐） */
  --tint-article:  #C68B4E;  --tint-article-on:  #1E1812;
  --tint-book:     #6E8F79;  --tint-book-on:     #1E1812;
  --tint-album:    #A85A48;  --tint-album-on:    #F3EAD8;
  --tint-music:    #5E7E9B;  --tint-music-on:    #F3EAD8;
  --tint-announce: #D9765C;  --tint-announce-on: #1E1812;
  --tint: var(--tint-article);
  --tint-on: var(--tint-article-on);
  --shadow: 0 2px 14px rgba(0, 0, 0, .5);
}
```

Tailwind 4 落地：以上变量经 `@theme` 映射为 `bg-background` / `text-foreground` / `border-border` / `bg-accent` 等工具类（阶段 2 globals.css 落地）。

**纸张纹理**（body 叠层，CSS 噪点近似羊皮纸质感，`pointer-events: none`）：

```css
body::before {
  content: ""; position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2'/%3E%3CfeColorMatrix values='0 0 0 0 0.55 0 0 0 0 0.47 0 0 0 0 0.35 0 0 0 0.04 0'/%3E%3C/filter%3E%3Crect width='120' height='120' filter='url(%23n)'/%3E%3C/svg%3E");
  opacity: .5;              /* dark 下建议降至 .2（深底噪点易脏） */
}
body > * { position: relative; z-index: 1; }
```

### 2.3 字体（✅ 定稿：全衬线）

| 用途 | 拉丁 | 中文 | 说明 |
| --- | --- | --- | --- |
| 标题 | Source Serif 4（衬线，可变 opsz/wght 300-900） | Noto Serif SC 900 | `--font-display` 变量驱动，文章独立样式可覆盖 |
| 正文 | Source Serif 4 | Noto Serif SC | `--font-body`，全站衬线（书籍正文气质） |
| 代码 | JetBrains Mono | — | 代码块/行内代码 |

> 全部走 `next/font/google` 自托管（国内访问稳定），fallback 系统字体栈：`"Noto Serif SC", "Songti SC", "STSong", "SimSun", serif`（中文）/ `Georgia, "Times New Roman", serif`（拉丁）。

### 2.4 排版与间距

| Token | 值 |
| --- | --- |
| 字号阶梯 | 12 / 14 / 16 / 18 / 20 / 24 / 30 / 36 / 48 / 64（px）；正文 16，文章内容区默认 17-18（可配） |
| 行高 | 正文 1.9（书卷宽松行距），标题 1.3 |
| 间距 | 4px 基准：4 / 8 / 12 / 16 / 24 / 32 / 48 / 64 / 96 |
| 容器 | `max-w-6xl`（1152px），文章内容区 720px |
| 圆角 | 克制：3（小元素）/ 6（图片）/ 8（卡片）/ 12（大分区）/ 999（pill 按钮） |
| 阴影 | light：`0 2px 14px rgba(43,33,24,.10)`（暖褐）；dark：`0 2px 14px rgba(0,0,0,.5)` |
| 区块装饰 | 区块标题双线饰线（下边框 + accent 双线短条）；书籍/图集/音乐编号用中文卷号"卷 一"；**Hero 区与板块容器外加取景框角标（四角 L 形角线）+ 虚线描边框（§2.1 v2.0 微调）** |

### 2.5 动效规范

| 场景 | 时长 | 缓动 |
| --- | --- | --- |
| 微交互（hover/点击反馈） | 150ms | `cubic-bezier(0.22, 1, 0.36, 1)` |
| 标准过渡（卡片/面板） | 300ms | 同上 |
| 抽屉/侧边栏/覆盖层 | 500ms | 同上 |
| 页面转场 | 800ms | 同上 |
| spring 动画 | — | `{ stiffness: 160, damping: 25 }` |

- **全站尊重 `prefers-reduced-motion`**：开启时禁用非必要动画，直接呈现最终状态（§2.8 每项动画均需提供降级）
- 滚动驱动动画统一用 framer-motion `useScroll` + `useTransform`（见 §8.1）
- 全站缓动统一收口 CSS 变量：`--ease: cubic-bezier(.22,1,.36,1)`

### 2.6 全局样式规则（对应参考站交互）

```css
/* 全站鼠标选择事件效果统一（article-03 参考图）：
   用当前分区色作选区底、反色文字，随 data-module 自动换色 */
::selection {
  background: var(--tint);
  color: var(--tint-on);
}

/* 卡片悬停反色的完整规则升级为 §2.7 双主题色调系统（T2 整卡反色），
   此处仅保留兜底（未标 data-module 的游离卡片用全局默认分区色） */
.hover-card { transition: background-color 300ms var(--ease), color 300ms var(--ease); }
.hover-card:hover { background: var(--tint); color: var(--tint-on); }
```

### 2.7 双主题色调系统（✅ v2.0 新增，站长已确认"整卡反色 + 每模块一色"）

#### 2.7.1 定义

参考站核心交互 DNA：**同一套版式配两种主题色调，正常展示一种（羊皮纸 + 墨褐），鼠标悬浮切换为另一种（分区色整体替换）**。每个内容模块拥有一对色调：

- **Normal 调**：羊皮纸底 `--card` + 墨褐文字 `--foreground`
- **Hover 调**：分区色底 `--tint` + 反色文字 `--tint-on`（light/night 各一组，CSS 变量自动适配）

#### 2.7.2 分区色映射

| 模块 | Hover 底色 (light) | Hover 文字 (light) | Hover 底色 (dark) | Hover 文字 (dark) | 色名 |
| --- | --- | --- | --- | --- | --- |
| 文章（全站默认） | `#8C5B2D` | `#F3EAD8` | `#C68B4E` | `#1E1812` | 赭石 |
| 书籍 | `#4F6D5A` | `#F3EAD8` | `#6E8F79` | `#1E1812` | 铜绿 |
| 图集 | `#7A3B2E` | `#F3EAD8` | `#A85A48` | `#F3EAD8` | 旧书布面酒红 |
| 音乐 | `#3E5C76` | `#F3EAD8` | `#5E7E9B` | `#F3EAD8` | 钢笔墨水蓝 |
| 公告 | `#B0442E` | `#F3EAD8` | `#D9765C` | `#1E1812` | 印章朱 |

分区色经模块容器的 `data-module` 属性注入（变量定义见 §2.2）：

```css
[data-module="article"] { --tint: var(--tint-article);  --tint-on: var(--tint-article-on); }
[data-module="book"]    { --tint: var(--tint-book);     --tint-on: var(--tint-book-on); }
[data-module="album"]   { --tint: var(--tint-album);    --tint-on: var(--tint-album-on); }
[data-module="music"]   { --tint: var(--tint-music);    --tint-on: var(--tint-music-on); }
[data-module="announce"]{ --tint: var(--tint-announce); --tint-on: var(--tint-announce-on); }
```

#### 2.7.3 三档 Hover 强度

| 档位 | 效果 | 适用场景 |
| --- | --- | --- |
| T1 行内染色 | 文字/图标 → `--tint`，下划线自左展开 | 正文内链、面包屑、导航项、页脚链接、分享行 |
| T2 整卡反色 | 卡背景→`--tint`、全卡文字→`--tint-on`、图片放大 + 分区色薄纱罩、阴影加深 | 文章/书籍/图集/音乐卡片、热门文章、问卷选项（✅ 站长选定的卡片默认档） |
| T3 整区换色 | 整个区块背景 → `--tint` 渐入（文字反色） | 文章吸顶横幅、相关文章覆盖层主区、公告条、全屏导航 |

#### 2.7.4 T2 整卡反色基类（全站 HoverCard 统一样式代码）

```css
.hover-card {
  background: var(--card);
  color: var(--foreground);
  transition: background-color 300ms var(--ease),
              color 300ms var(--ease),
              box-shadow 300ms var(--ease);
}
.hover-card .card-img { position: relative; overflow: hidden; }
.hover-card .card-img img { transition: transform 500ms var(--ease); }
.hover-card:hover {
  background: var(--tint);                          /* 整卡底色→分区色 */
  color: var(--tint-on);                            /* 全卡文字反色 */
  box-shadow: 0 6px 24px rgba(43, 33, 24, .18);
}
.hover-card:hover .card-img img { transform: scale(1.04); }
.hover-card:hover .card-img::after {               /* 图片分区色薄纱罩 */
  content: ""; position: absolute; inset: 0;
  background: var(--tint); opacity: .18; mix-blend-mode: multiply;
}
/* 卡内分类标签、简介等子元素颜色全部继承 color，无需单独处理；
   独立描边/图标元素 hover 时同步染色： */
.hover-card:hover .tag,
.hover-card:hover .icon { color: var(--tint-on); border-color: var(--tint-on); }
```

### 2.8 全站微交互动画规范（✅ v2.0 新增，44 项）

> 总则：① 缓动统一 `--ease: cubic-bezier(.22,1,.36,1)`，时长遵循 §2.5；② 每项动画在 `prefers-reduced-motion: reduce` 下直接呈现最终状态；③ 标注"内置"的项直接使用 magicui/framer-motion 组件能力，其余用 CSS keyframes 自实现。

#### A 框架与导航（8 项）

| # | 场景 | 触发 | 动画描述 | 参数/实现 |
| --- | --- | --- | --- | --- |
| A1 | 汉堡按钮 | hover | 三条杠中杆由 70% 宽伸至 100% | CSS width 150ms |
| A2 | 汉堡按钮 | 点击 | 三条杠→X：上杆 rotate 45° 下移、中杆淡出、下杆 -45° 上移 | 300ms，framer-motion |
| A3 | 全屏导航展开 | 点击汉堡 | 羊皮纸色块自上而下 clip-path 展开；组标题/条目 blur-fade 级联入场（组间 90ms、条目 40ms stagger）；关闭时 X 旋转回落 | 500ms，AnimatePresence |
| A4 | 导航条目 | hover | 文字左移 4px + 前缀"❧"花饰淡入 + 变 `--tint` | 150ms |
| A5 | 字标 LOGO | hover | 光环内环旋转 360°（transform-origin: center） | 600ms |
| A6 | 面包屑 | hover | 每级下划线 scaleX 0→1 自左展开 + 变 `--tint` | 150ms |
| A7 | 语言/主题按钮 | hover | 图标 rotate ±15° 回弹；主题切换用 AnimatedThemeToggler（View Transitions 圆形扩散自按钮位置） | 150ms / 内置 |
| A8 | 返回顶部 | 滚动超过 1 屏 | 圆形按钮 blur-fade 上浮出现；hover 整卡反色（T2）；点击平滑滚顶 | 300ms |

#### B 卡片与列表（9 项，双色调主战场）

| # | 场景 | 触发 | 动画描述 | 参数/实现 |
| --- | --- | --- | --- | --- |
| B1 | 内容卡片 | hover | T2 整卡反色（§2.7.4） | 300ms |
| B2 | 卡片图片 | hover | scale 1.04 + 分区色薄纱罩 | 500ms |
| B3 | 分类标签 chip | hover | 背景染色、文字反色、上跳 2px 回落 | 150ms |
| B4 | 热门排名数字 | hover | kinetic-text 字重动画（300→900） | 内置 |
| B5 | "阅读"按钮 | hover | interactive-hover-button：文字左移、花饰"❧"自右滑入 | 内置 |
| B6 | 列表入场 | 进入视口 | blur-fade 级联，delay = idx × 50ms | 内置 |
| B7 | 封面懒加载 | 加载 | pixel-image 像素块渐显 | 内置 |
| B8 | 空状态 | 无数据 | 卷轴插画 + 文字逐字淡入 | 600ms |
| B9 | 骨架屏 | loading | 羊皮纸色 shimmer 扫光 | 1.2s 循环 |

#### C 文章详情（8 项，v2.0 重写重点）

| # | 场景 | 触发 | 动画描述 | 参数/实现 |
| --- | --- | --- | --- | --- |
| C1 | 吸顶横幅 | 滚动 5%-8% | T3 整区换色横幅（`--tint` 底 + 反色文字）自顶部滑入：LOGO 眼睛 + 文章标题 + 面包屑 + 阅读进度条（§8.1） | framer-motion useTransform |
| C2 | 阅读进度条 | 滚动 | 吸顶栏底部 2px 进度条按 scrollYProgress 填充 | useScroll |
| C3 | **竖排侧边按钮** | hover | **文字竖排非正放**（`writing-mode: vertical-rl`）+ 变 `--tint` + **逐字跳动**（每字 translateY -6px 回落，stagger 45ms）+ 伴随细线伸长；代码见 §8.2 | CSS keyframes |
| C4 | 竖排侧边按钮 | 打开 | 文字"查看更多文章"⇄"收起" MorphingText 轮换；面板滑入 | 500ms |
| C5 | 相关文章覆盖层 | 点击按钮 | 按参考站：**左侧滑出白色列表面板（约 1/3 宽，文章卡片列）+ 主内容区背景渐变为淡 `--tint`（T3）+ 顶部引导语**（"或许你还想读：" / "You may also like:"）；右缘按钮变"收起"；ESC/遮罩关闭（§8.2） | AnimatePresence |
| C6 | 分享行 | hover | 每个分享项下划线展开 + 变 `--tint` | 150ms |
| C7 | 正文内链 | hover | T1：变 `--tint` + highlighter 荧光标注扫过 | 内置 |
| C8 | 图片注解 | hover | 图注淡入 + 图片微降饱和 | 200ms |

**C3 竖排跳动按钮参考代码**（每个字包一层 `<span class="ch" style="--i:0">`，`--i` 为字序号）：

```css
.side-tab {
  position: fixed; right: 16px; top: 50%; transform: translateY(-50%);
  writing-mode: vertical-rl;      /* 文字竖排——非正放 */
  text-orientation: mixed;        /* 中文直立，拉丁字母顺时针旋转 90° */
  letter-spacing: .35em;
  font-size: 13px; color: var(--muted);
  background: transparent; border: 0; cursor: pointer;
  transition: color 150ms var(--ease);
}
.side-tab::after {               /* 伴随细线：hover 伸长 */
  content: ""; display: block; width: 1px; height: 24px;
  margin: 12px auto 0; background: currentColor;
  transition: height 300ms var(--ease);
}
.side-tab:hover { color: var(--tint); }
.side-tab:hover::after { height: 48px; }
.side-tab .ch { display: inline-block; }
.side-tab:hover .ch {
  animation: hop .5s var(--ease) both;
  animation-delay: calc(var(--i) * 45ms);   /* 逐字接力跳动 */
}
@keyframes hop {
  0%, 100% { transform: translateY(0); }
  35%      { transform: translateY(-6px); }
  70%      { transform: translateY(2px); }
}
@media (prefers-reduced-motion: reduce) {
  .side-tab:hover .ch { animation: none; }  /* 降级：仅变色不跳动 */
}
```

#### D 媒体（5 项）

| # | 场景 | 触发 | 动画描述 | 参数/实现 |
| --- | --- | --- | --- | --- |
| D1 | 图集卡片 | hover | lens 放大镜跟随鼠标（zoomFactor 2） | 内置 |
| D2 | 图片预览 | 点击 | 遮罩淡入 + 图片 scale .96→1 + 信息面板上滑 | 300ms |
| D3 | 音乐特效 | 播放 | MusicVisualizer 三层引擎（§8.4） | Canvas rAF |
| D4 | 歌词 | 播放 | 当前行 `--tint` 高亮 + 平滑滚至视口中部 | 300ms |
| D5 | 视频封面 | hover | 播放键圆环扩散 + 封面 zoom | 300ms |

#### E 反馈与状态（6 项）

| # | 场景 | 触发 | 动画描述 | 参数/实现 |
| --- | --- | --- | --- | --- |
| E1 | 文字选中 | 选取 | `::selection` = `--tint` 底 + 反色字（§2.6，随模块换色） | CSS |
| E2 | 输入框 | focus | 边框变 `--tint` + 2px 外发光晕开 | 150ms |
| E3 | 订阅按钮 | 点击 | cool-mode 粒子迸发 | 内置 |
| E4 | 表单提交 | 成功/失败 | 成功打勾画线（stroke-dashoffset）；失败摇晃 2 次 | 400ms |
| E5 | Toast | 出现 | 底部上滑入场 + 自动消退 | 300ms |
| E6 | 问卷选项 | hover/选中 | 卡片 T2 反色；选中时"❧"印章盖戳（scale 1.4→1 + rotate -8°） | 300ms |

#### F 氛围（8 项，品牌记忆点）

| # | 场景 | 触发 | 动画描述 | 参数/实现 |
| --- | --- | --- | --- | --- |
| F1 | 页面加载 | 首次 | AetherRing 光环苏醒 1.9s 后遮罩淡出（§3.4） | 自实现 |
| F2 | 版权区 LOGO | 滚入视口 | AetherRing 闭眼→睁眼展开（呼应参考站眼睛 LOGO） | §3.4 |
| F3 | 公告轮播 | 常态 | Marquee 循环滚动；hover 暂停 + 标题 highlighter 荧光标注 | 内置 |
| F4 | Hero | hover | 信息面板淡入上滑 + 字标 kinetic-text + 口号 morphing-text | 内置 |
| F5 | 板块标题 | 进入视口 | 双线饰线自中心向两侧展开 + 卷号淡入 | 600ms |
| F6 | 书籍阅读器 | 翻页 | rotateY 翻页（§8.6） | framer-motion |
| F7 | 管理端登录 | 常态 | 背景文字浪（§8.7） | Canvas |
| F8 | AI 悬浮窗 | 唤醒/闲置 | LOGO 呼吸光晕；展开 spring 弹出；闲置收至右缘露 40px | §8.5 |

## 3 LOGO 方案（✅ 已定稿：羊皮卷 · 古典文学风）

### 3.1 视觉语言

- 主题：以太（Aether）——古典哲学第五元素、星界之气、流动与静谧，与"羊皮卷"古典文学基调呼应
- 风格：**蚀刻版画（etching/woodcut）线条** + 藏书票（bookplate）气质，细线雕刻感，禁现代扁平/霓虹
- 主图形：**以太光环**（同心圆环）+ 中央羽笔与展开书页剪影
- 配色：羊皮纸底 `#F3EAD8`、墨褐线条 `#2B2118`、赭石点缀 `#8C5B2D`、铜绿副色 `#4F6D5A`
- 形态：① web 图片 LOGO（ComfyUI，§3.2）；② SVG 文字 LOGO（§3.3）；③ JS 光环加载动画（§3.4）；④ favicon / og-image / 管理端小标（§3.5）

### 3.2 ComfyUI prompt（图片 LOGO）

```text
Positive prompt: minimalist vintage logo, concentric orbital rings forming an
aether halo, antique quill pen crossing an open book silhouette at center,
fine line engraving, woodcut etching style, classical literature bookplate
aesthetic, parchment cream background (#F3EAD8), ink brown linework (#2B2118),
ochre accents (#8C5B2D), verdigris green secondary accents (#4F6D5A),
ancient Greek "aether" fifth element concept, elegant thin lines,
centered composition, high contrast, clean edges, no text

Negative prompt: text, letters, words, watermark, signature, photorealistic,
3d render, photograph, busy background, blurry, low quality, jpeg artifacts,
multiple objects, modern flat design, neon colors, bright saturated colors
```

生成参数建议：SDXL 或 Flux 模型，1024×1024，steps 30，cfg 7，无负面词叠加；出图后用 PS/remove.bg 抠透明底，导出 1024 / 512 / 256 / 64 四档 PNG（favicon 用 64）。生成多张候选后站长挑选，落盘 `frontend/public/logo/`（命名 `aether-logo-{size}.png`）。

### 3.3 SVG 文字 LOGO（字标，light/dark 自适应）

静态版（Navbar 用）：衬线大写 "AETHER" + 上方以太光环符 + 下方双线饰线；颜色用 CSS 变量，主题切换自动适配。

```html
<!-- 静态字标：nav / footer 通用（fill 用 var(--foreground)，光环用 var(--accent)） -->
<svg class="aether-wordmark" viewBox="0 0 360 100" role="img" aria-label="Aether">
  <!-- 以太光环符：外环 + 内环 + 中心点 -->
  <circle cx="180" cy="22" r="13" fill="none" stroke="var(--accent)" stroke-width="1.5"/>
  <circle cx="180" cy="22" r="7"  fill="none" stroke="var(--accent-2)" stroke-width="1.5"/>
  <circle cx="180" cy="22" r="2"  fill="var(--accent)"/>
  <!-- 字标 -->
  <text x="180" y="66" text-anchor="middle" font-family="'Source Serif 4','Noto Serif SC',Georgia,serif"
        font-weight="900" font-size="44" letter-spacing="10" fill="var(--foreground)">AETHER</text>
  <!-- 双线饰线：两端细线 + 中心菱形 -->
  <path d="M 88 82 H 172 M 188 82 H 272" stroke="var(--border)" stroke-width="1"/>
  <rect x="176" y="78" width="8" height="8" transform="rotate(45 180 82)" fill="var(--accent)"/>
</svg>
```

动画版（Hero / 加载过渡用）：光环先"苏醒"，字母逐字上浮入场（stagger），随后饰线展开。

```html
<!-- 动画字标：光环苏醒 → 字母逐字浮现 → 饰线展开（CSS 动画，无需 JS） -->
<svg class="aether-wordmark animated" viewBox="0 0 360 100" role="img" aria-label="Aether">
  <style>
    .aether-wordmark.animated .halo-o { stroke-dasharray: 82; stroke-dashoffset: 82; animation: halo 1s cubic-bezier(0.22,1,0.36,1) forwards; }
    .aether-wordmark.animated .halo-i { opacity: 0; animation: halo-in .6s ease .5s forwards; }
    .aether-wordmark.animated .halo-c { opacity: 0; animation: halo-in .4s ease .8s forwards; }
    .aether-wordmark.animated .letter { opacity: 0; transform: translateY(14px); animation: rise .6s cubic-bezier(0.22,1,0.36,1) forwards; }
    .aether-wordmark.animated .rule { stroke-dasharray: 84; stroke-dashoffset: 84; animation: halo 1.2s ease .4s forwards; }
    .aether-wordmark.animated .diamond { opacity: 0; animation: halo-in .4s ease 1.3s forwards; }
    @keyframes halo { to { stroke-dashoffset: 0; } }
    @keyframes halo-in { to { opacity: 1; } }
    @keyframes rise { to { opacity: 1; transform: translateY(0); } }
    /* 字母 stagger：A 起 0.35s，步进 0.12s */
    .letter:nth-of-type(1) { animation-delay: .35s; } .letter:nth-of-type(2) { animation-delay: .47s; }
    .letter:nth-of-type(3) { animation-delay: .59s; } .letter:nth-of-type(4) { animation-delay: .71s; }
    .letter:nth-of-type(5) { animation-delay: .83s; } .letter:nth-of-type(6) { animation-delay: .95s; }
    @media (prefers-reduced-motion: reduce) {
      .aether-wordmark.animated * { animation: none !important; opacity: 1 !important; stroke-dashoffset: 0 !important; transform: none !important; }
    }
  </style>
  <circle class="halo-o" cx="180" cy="22" r="13" fill="none" stroke="var(--accent)" stroke-width="1.5"/>
  <circle class="halo-i" cx="180" cy="22" r="7" fill="none" stroke="var(--accent-2)" stroke-width="1.5"/>
  <circle class="halo-c" cx="180" cy="22" r="2" fill="var(--accent)"/>
  <text x="180" y="66" text-anchor="middle" font-family="'Source Serif 4','Noto Serif SC',Georgia,serif"
        font-weight="900" font-size="44" letter-spacing="10" fill="var(--foreground)">
    <tspan class="letter">A</tspan><tspan class="letter">E</tspan><tspan class="letter">T</tspan>
    <tspan class="letter">H</tspan><tspan class="letter">E</tspan><tspan class="letter">R</tspan>
  </text>
  <path class="rule" d="M 88 82 H 172 M 188 82 H 272" stroke="var(--border)" stroke-width="1"/>
  <rect class="diamond" x="176" y="78" width="8" height="8" transform="rotate(45 180 82)" fill="var(--accent)"/>
</svg>
```

注意：SVG `<tspan>` 不支持 letter-spacing（部分浏览器忽略），阶段 2 落地时字母间距改用 `dx` 逐字偏移实现，此处为动画结构参考。

### 3.4 JS 光环加载动画（AetherRing）

同心圆环由闭到开、由暗到亮"苏醒"（呼应参考站眼睛由闭到睁），两处复用：

| 位置 | 行为 |
| --- | --- |
| 页面加载 loading | 全屏遮罩中央播放，动画完成（约 1.9s）后淡出遮罩 |
| 首页版权区 | 滚动进入视口（IntersectionObserver，threshold 0.4）触发播放，不循环 |

参数规格：

| 参数 | 值 |
| --- | --- |
| 环数 | 4（半径 60 / 105 / 150 / 195 px，交替 accent / accent-2 描边 1-2px）+ 中心亮点（8px 圆，box-shadow 光晕） |
| 动画 | 每环 `scale .55→1` + `opacity 0→.9`，时长 1.4s，缓动 `cubic-bezier(0.22,1,0.36,1)`，stagger 120ms；中心亮点 delay 480ms；下方 "AETHER" 字标 1s 后淡入 |
| 无障碍 | `prefers-reduced-motion` 时跳过动画直接呈现最终状态 |
| 组件接口 | `components/layout/AetherRing.tsx`：`<AetherRing trigger: "loading" | "scroll" />`，阶段 2 落地；CSS 参考实现见本地展示页 `design/moodboards/style-d.html`（不入库）版权区（`#ringStage`） |

### 3.5 favicon / og-image / 管理端小标

| 产物 | 规格 | 说明 |
| --- | --- | --- |
| favicon | 64×64 透明 PNG（图片 LOGO 缩小版）+ `favicon.ico` 打包 | 蚀刻线条在 16px 下会糊，favicon 简化为**单环 + 中心点**（§3.3 光环符），可另出 SVG favicon 并 `media="(prefers-color-scheme: dark)"` 双版 |
| og-image | 1200×630：羊皮纸底 + 字标 + 光环，右下角铜绿"❧"装饰 | 由 §3.2 图片 LOGO 排版合成（非 AI 直出） |
| 管理端小标 | 32×32：单环 + 中心点（cryptex 登录页与侧栏用） | 复用 favicon 图形 |
| apple-touch-icon | 180×180 PNG | 羊皮纸底圆角方形版 |

生成物落盘 `frontend/public/logo/`，命名 `aether-{logo|favicon|og|touch}-{size}.png`。

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
      layout/                 # Navbar/Footer/Breadcrumb/AetherRing/SideTab
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
| Navbar | 左上角汉堡按钮（A1/A2，点击展开全屏 Sheet 导航 A3：文章/图集/视频/音乐/书籍/公告/分类入口，菜单项 blur-fade 级联入场，参考 home-03）；中间文字 LOGO（A5）；右上角：AnimatedThemeToggler（A7）、语言切换、订阅按钮、GitHub 图标、管理端入口 |
| Footer | 版权行 + 底部滚动展开的 AetherRing LOGO（F2，§3.1-③） |
| Breadcrumb | 全站完整面包屑：首页 / 模块 / 详情（含当前页，aria-label 规范；hover 动画 A6） |
| ThemeProvider | next-themes：light/night，localStorage 记忆 + `prefers-color-scheme` 兜底 |
| LocaleSwitcher | 中文简/EN 切换按钮：切换后 URL 不变，仅重渲染界面文字（next-intl `setRequestLocale` + Cookie 持久化） |
| ProtectedImage | 防下载图片组件（§9.2）：统一 contextmenu/dragstart 拦截 + 签名 URL 加载 + 过期自动刷新 |
| ProtectedMedia | 视频/音频容器：签名 URL + Referer 白名单由后端保障（§9.2） |
| HoverCard | 悬停反色卡片基类（§2.7.4 T2 整卡反色），全站卡片统一继承 |
| SideTab | 竖排侧边文字按钮（C3/C4，§8.2）：竖排逐字跳动 + 变色 + MorphingText 轮换 |
| SseChatView | SSE 流式对话渲染（AI 助手 tab 复用） |

## 6 逐页面设计（v2.0：标注动效编号，见 §2.8）

### 6.1 主页（参考 home-01 长截图，板块自上而下）

| # | 板块 | 设计 | 动效 |
| --- | --- | --- | --- |
| 1 | 导航 | 见 §5 Navbar；Hero/板块容器取景框角标 + 虚线框装饰 | A1-A5 |
| 2 | Hero 区 | 大张 LOGO 样式区：图片 LOGO（§3）为底 + `kinetic-text` 渲染 "AETHER" 字标（逐字字重动画）+ `morphing-text` 轮换项目口号（中英双语词条）；**鼠标悬浮时浮现项目信息面板**（简介/定位/入口链接，参考 home-02 的 hover 信息展示）；取景框角标 + 虚线描边 | F4 |
| 3 | 公告区 | 顺时针轮播最近 3-5 条公告（公告/动态/新闻混合，`GET /aether/api/v1/announcements/latest`）；自定义 Marquee（循环滚动 + 点击暂停）+ 标题关键词 `highlighter` 荧光标注；本区 `data-module="announce"` | F3 |
| 4 | 热门文章 | 6-10 条，`GET /articles/hot`；卡片网格，**不显示创建时间**；hover T2 整卡反色（§2.7.4，分区色=赭石）；排名数字 kinetic-text；卡片带 interactive-hover-button "阅读"；后端 `hot_order` 可自定义排序 | B1/B2/B4/B5/B6 |
| 5 | 推荐书籍 | 3-4 本并排（参考 book.png：封面微微缩小 + 书名 + 作者 + 一句话介绍或分类标签）；封面 `pixel-image` 懒加载像素过渡；入场 `blur-fade` 级联；hover 反色同热门文章（分区色=铜绿）；卷号"卷 一"编号 | B1/B2/B6/B7，F5 |
| 6 | 推荐图集 | 3-4 个，`lens` 放大镜卡片（见 §6.4）；分区色=酒红 | D1，B6 |
| 7 | 推荐音乐/专辑 | 3-4 个（封面 + 歌名 + 歌手/合集名），hover T2 反色；分区色=墨水蓝 | B1/B2/B6 |
| 8 | 版权区 | 版权文字 + 下方 AetherRing 滚动睁眼 LOGO（§3.1-③） | F2 |

### 6.2 文章列表 / 搜索页

- 列表卡片：封面图（ProtectedImage）+ 标题 + 简介 + 分类标签 + 阅读数（无创建时间）；hover T2 反色（赭石）；`data-module="article"`
- 筛选：分类/标签下拉 + 排序（最新/热门）
- 搜索页（`/aether/search?q=`）：仅文章可搜索（标题/简介/内容），音乐搜索独立入口分 tab 展示；面包屑：首页 / 搜索
- 动效：B1/B2/B3/B6/B7/B9、A6

### 6.3 文章详情（参考 article-01 ~ article-04）

| 区域 | 设计 | 动效 |
| --- | --- | --- |
| 页面结构 | 标题（大号衬线）→ 分类/标签/阅读数 → 封面 → 正文；面包屑：首页 / 文章 / 标题 | A6 |
| 正文渲染 | `content_html` 经 DOMPurify sanitize 后渲染；注入文章独立样式（article_style.style_json → CSS Variables 容器内联，覆盖默认排版）；排版参数：字号/行高/段间距/首行缩进/主题色/衬线开关/内容区宽度 | C7/C8 |
| 吸顶标题栏 | 滚动页面 5%-8% 时顶部浮现 T3 整区换色横幅（§8.1，参考 article-02——`--tint` 底 + LOGO 眼睛 + 标题 + 分享行） | C1/C2 |
| RELATED ARTICLES | 文章末尾 + 网页最右侧竖排跳动按钮 + 相关文章覆盖层（§8.2，参考 article-03/04） | C3/C4/C5 |
| 阅读数 | 详情接口自动计数（后端 IP 去重）；**SSR 预取带 `X-Aether-No-Count: 1` 跳过计数，浏览器 hydrate 补偿请求以真实访客 IP 计数（阶段 2 落地，2026-08-25）** | — |

### 6.4 图集列表与详情（参考 photo-01）

| 区域 | 设计 | 动效 |
| --- | --- | --- |
| 列表卡片 | `lens` 卡片：封面（hover 放大镜效果）+ 题目 + 简介；**去除参考图 "Let's go / Another time" 按钮**，点击卡片直接跳转图集详情；入场 blur-fade；hover T2 反色（酒红） | D1，B1/B6 |
| 详情页 | 封面头部 + 介绍 + 图片瀑布流：`blur-fade` 主题框架（`columns-2 sm:columns-3` 瀑布流）+ 图片 `pixel-image` 像素过渡懒加载；面包屑：首页 / 图集 / 标题 | B6/B7，A6 |
| 图片预览 | 点击图片打开预览层（不可下载）：悬浮显示图片信息（大小必显，标题/介绍可选）；加载过程用 `pixel-image`；关闭 ESC/遮罩 | D2 |

### 6.5 视频列表与详情

- 列表：封面 + 标题 + 时长（后端探测）卡片，hover T2 反色（赭石，视频归入文章默认分区色）
- 详情：ArtPlayer 播放器（倍速、清晰度、**关键时间节点**右侧列表点击跳转、画中画）；下方标题/介绍；签名 URL 播放（ProtectedMedia）
- 面包屑：首页 / 视频 / 标题
- 动效：B1/B2/B6、D5、A6

### 6.6 音乐（详情页 + 悬浮 tab + 歌词同步）

| 区域 | 设计 | 动效 |
| --- | --- | --- |
| 详情页 | 专辑/单曲封面 + 歌手 + 歌词（滚动 + 当前行高亮）+ 进度条 + 音量；背景与主体为 **MusicVisualizer AI 特效层**（§8.4）；`data-module="music"` | D3/D4 |
| 合集页 | 自定义合集 / 固定合集（含认证信息展示）分组列表；支持单曲搜索（独立于文章搜索）；卡片 hover T2 反色（墨水蓝） | B1/B2/B6 |
| 悬浮播放 | AI 助手悬浮窗内"音乐"tab（§8.4）：封面缩略 + 歌名 + 播放/暂停/上一首/下一首/退出；切歌后悬浮窗可收起继续播放（后台 Audio 实例全局单例） | F8 |

**歌词不同步解决方案**（后端存储与对齐方案见 BackEnd-Plan §7.6）：

| 场景 | 前端渲染策略 |
| --- | --- |
| LRC 时间轴 | 解析 `[mm:ss.xx]` 逐行同步滚动，当前行 accent 高亮 + 自动滚到可视区中部 |
| 纯文本歌词 | 按"总行数 ÷ 总时长"均分滚动（粗略同步，兜底） |
| 全局偏移 | 应用 `music.lyric_offset`（毫秒，正负可调）统一前移/后移全部行时间 |
| 手动打点 | 管理端歌词编辑器播放时逐行点击"标记当前行"生成时间轴（§6.11） |

### 6.7 书籍列表与阅读器

| 区域 | 设计 | 动效 |
| --- | --- | --- |
| 列表 | 封面 + 书名 + 作者 + 一句话介绍或分类标签（同参考 book.png 版式）；hover T2 反色（铜绿）；`data-module="book"` | B1/B2/B6 |
| 详情 | 封面 + 简介 + 章节目录（章-节两级树） | F5，A6 |
| 阅读器 | 第一页封面 → framer-motion `rotateY` 翻页动画 → **第二页版权声明**（文案模板由 `book.ownership_type` 决定：1 本人——"本书为本站原创作品，版权归作者所有…"；2 他人出版——"本书版权归原作者/出版社所有，本站仅作分享展示，请支持正版…"）；目录抽屉（章/节树）、字号/行距/阅读主题（仿纸色夜间模式）设置、阅读进度存 localStorage、章节锚点与 ←/→ 键盘翻页、段首空两格与段间半行距由后端排版保证 | F6 |

### 6.8 公告

- 列表：分类 tab（公告/动态/新闻）+ 卡片列表；hover T2 反色（印章朱）；`data-module="announce"`
- 详情：标题 + 时间 + 图文内容；面包屑：首页 / 公告 / 标题
- 动效：B1/B2/B6、A6

### 6.9 订阅弹窗与问卷

| 步骤 | 设计 | 动效 |
| --- | --- | --- |
| 入口 | 右上角订阅按钮 → 屏幕中央 Modal | E3（cool-mode 粒子） |
| 订阅 | 邮箱输入（前端格式校验）→ 提交 → 该 IP 已提交的邮箱列表展示（`GET /subscribe/emails`）→ 提示问卷 | E2/E4 |
| 问卷 | 可跳过；选项来自 `GET /survey-options`（年龄段/性别/职业/兴趣爱好）；**一个 IP 一份**（重复提交返回 30702）；**可修改 2 次**（第 3 次返回 30703 并展示文案"修改次数已用完"） | E5/E6 |

### 6.10 AI 助手悬浮窗

| 项 | 设计 |
| --- | --- |
| 注入条件 | `GET /ai/config` 返回 `enabled=true` 才渲染（管理端可关闭） |
| 外观 | 右下角圆形 LOGO 按钮（图片 LOGO 512，呼吸光晕）；点击展开悬浮窗（宽 ~380px，spring 弹出） |
| 唤醒 | `CTRL + 空格` 唤醒/隐藏（焦点在 input/textarea/contenteditable 时忽略，避免输入法冲突） |
| 自动收起 | 无交互（mousemove/keydown 重置计时）约 5 分钟 → 收起至屏幕右边缘只露 40px 图标，点击重新展开；ESC 可关闭 |
| 内容 | 两个 tab：**对话**（SSE 流式渲染 SseChatView，会话 UUID 存 localStorage，历史取服务端）与**音乐**（§6.6 悬浮播放器）；达到每日限额时展示"今日使用次数已达上限"（30803 文案） |
| 动效 | F8、E5 |

### 6.11 管理端（cryptex，仅中文，新窗口）

| 页面 | 设计 |
| --- | --- |
| 登录页 `/aether/cryptex/login` | 背景文字浪（Canvas 字符矩阵 "AETHER/以太" + 正弦波浪位移，F7）；中央卡片：**仅一个密码输入框**（即 cryptex，无用户名输入）+ （失败 3 次后出现）图形验证码；登录失败提示统一为"密码错误"；成功跳 dashboard；输入框 focus 光晕 E2、失败摇晃 E4 |
| 框架 | 左侧栏（内容模块 + 统计 + 日志 + AI 配置）+ 顶栏（面包屑 + 登出）；middleware 校验 JWT，401 → 登录页；侧栏条目 hover 动画 A4 |
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
| morphing-text | Hero 口号轮换、SideTab 竖排按钮文字轮换（"查看更多文章"⇄"收起"） | `npx shadcn@latest add @magicui/morphing-text` |
| text-3d-flip | 备选：SideTab 按钮逐字翻折（若 MorphingText 竖排渲染异常的降级方案） | `npx shadcn@latest add @magicui/text-3d-flip` |
| highlighter | 公告轮播标题关键词荧光标注、正文内链 hover | `npx shadcn@latest add @magicui/highlighter` |
| lens | 图集卡片放大镜（去按钮，点击跳详情） | `npx shadcn@latest add @magicui/lens` |
| blur-fade | 全站列表/区块入场、汉堡菜单项级联、返回顶部按钮 | `npx shadcn@latest add @magicui/blur-fade` |
| pixel-image | 封面/图片懒加载像素过渡 | `npx shadcn@latest add @magicui/pixel-image` |
| cool-mode | 订阅按钮点击粒子 | `npx shadcn@latest add @magicui/cool-mode` |
| interactive-hover-button | 热门文章卡片"阅读"按钮 | `npx shadcn@latest add @magicui/interactive-hover-button` |
| comic-text | 可选：公告彩蛋强视觉场景，**不硬塞** | `npx shadcn@latest add @magicui/comic-text` |

原则：组件风格统一适配设计系统 tokens（色调跟随 CSS 变量），魔法效果用于增强、不喧宾夺主。竖排跳动（C3）为 CSS keyframes 自实现（§2.8 代码），不依赖组件。

## 8 关键交互难点方案

### 8.1 文章详情吸顶标题栏（滚动 5%-8%）

- framer-motion `useScroll({ target: containerRef })` 取 `scrollYProgress`（0~1 比值，天然满足"整个页面的 5%-8%"）
- `useTransform(scrollYProgress, [0.05, 0.08], [0, 1])` 映射标题栏 opacity，`y: -100% → 0` 滑入
- 标题栏：**T3 整区换色横幅（`--tint` 底 + 反色文字）** + LOGO 眼睛 + 文章标题 + 分类面包屑 + 阅读进度条；`position: fixed top-0`，正文容器 `padding-top` 预留防跳变；`prefers-reduced-motion` 时直接显示

```tsx
"use client"
// 滚动 5%-8% 区间映射吸顶横幅的透明度与位移
const { scrollYProgress } = useScroll({ target: articleRef });
const opacity = useTransform(scrollYProgress, [0.05, 0.08], [0, 1]);
const y = useTransform(scrollYProgress, [0.05, 0.08], ["-100%", "0%"]);
return (
  <motion.header className="fixed inset-x-0 top-0 z-40"
    style={{ opacity, y, background: "var(--tint)", color: "var(--tint-on)" }}>
    {/* LOGO 眼睛 + 标题 + 面包屑 + 阅读进度条（C2） */}
  </motion.header>
);
```

### 8.2 RELATED ARTICLES 侧边栏（✅ v2.0 按参考站实测行为重做）

**默认态（参考 article-01/03）**：

- 桌面端（≥lg）：屏幕右侧 `fixed right-4 top-1/2` 竖排透明文字按钮（SideTab 组件，§5）——"查看更多文章"竖排（`writing-mode: vertical-rl`，中文逐字直立），宽字距，muted 色
- **hover（C3）**：文字变 `--tint` + 逐字跳动（translateY -6px 回落，stagger 45ms，代码见 §2.8）+ 伴随细线伸长 24px→48px；可选叠加 Text3DFlip 逐字翻折（§7 备选）
- 移动端（<lg）：不渲染侧边按钮，正文底部"其他文章推荐"区块

**打开态（参考 article-04，点击按钮后）**：

- **左侧滑出白色列表面板**（约 1/3 视口宽，书页卡底色）：顶部保留吸顶横幅，下方为相关文章卡片列表（封面 + 分类 + 标题 + 简介，卡片可 hover T1/T2）
- **主内容区背景渐变为淡分区色**（T3：`color-mix(in srgb, var(--tint) 12%, var(--background))`），与面板形成参照站的双区视觉
- 主区顶部引导语："或许你还想读：" / "You may also like:"，其下为相关文章 2 列网格
- **右缘竖排按钮文字切换为"收起"**（MorphingText 轮换或直接替换 + 逐字跳动保留）；点击/ESC/遮罩关闭，面板滑出、主区背景还原
- 动画：面板 translateX -100%→0（500ms），主区背景色过渡 300ms，均走 AnimatePresence
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
- **SSR 双保险落地方式（阶段 2，2026-08-25）**：SSR 渲染后端已 jsoup 清洗的 HTML（DOMPurify 依赖浏览器环境不可用）；客户端挂载后动态加载 DOMPurify 二次清洗再替换
- AI 沙箱代码仅限 iframe sandbox 执行（§8.4 第三层）

### 9.4 管理端鉴权

- proxy.ts（Next 16 已用 proxy 替代 middleware）：`/aether/cryptex/*`（login 除外）校验 Access Token（存在 + 未过期），失败 302 `/aether/cryptex/login`；API 401 时 api client 自动走 refresh 流程（`/auth/refresh`），refresh 失败登出
- **proxy.ts 已落地（阶段 2，2026-08-25，i18n 部分）**：next-intl `createMiddleware` 无前缀 rewrite 与 Next 16 basePath 组合存在 404 兼容问题，改为最小自实现（Cookie `NEXT_LOCALE` → `X-NEXT-INTL-LOCALE` header 注入，逻辑等价）；JWT 校验在阶段 8 追加

## 10 响应式设计

| 断点 | 适配策略 |
| --- | --- |
| ≥1024px（lg） | 完整版式：SideTab 竖排按钮 + 相关文章覆盖层、4 列书籍推荐 |
| 768-1024px（md） | 卡片 3 列；侧边按钮隐藏（正文底部推荐区块） |
| <768px（sm） | 单列；Hero 字标缩小；汉堡导航全屏；管理端表格转卡片；MusicVisualizer 粒子减半 + 30fps |
| 触摸设备 | hover 双色调反色改为 tap 反馈（:active 触发 T2）；逐字跳动保留（无 hover 依赖）；CTRL+Space 悬浮窗改为点按按钮 |

## 11 SEO 与 i18n

- **i18n**：next-intl，**无语言路由前缀**（`localePrefix: 'never'`）——同一 URL 站内切换中简/英文；语言选择持久化（Cookie + localStorage），首访按浏览器语言自动选择；词条文件 `messages/zh.json`、`messages/en.json`（**全部系统文字双语**：导航/按钮/提示/错误码文案映射；用户内容不翻译；SideTab 文字"查看更多文章/收起"同为双语词条）；管理端仅中文
- **主题**：next-themes + AnimatedThemeToggler；无 JS 时由 `prefers-color-scheme` 兜底
- **元数据**：generateMetadata 输出 title/description/OG；canonical 指向本站当前 URL
- 错误码文案映射：如 30703 → "问卷修改次数已用完" / "Survey update limit reached"

## 12 附录

### 12.1 参考截图索引（v2.0 补精读结论）

| 截图 | 对应设计 | v2.0 精读结论 |
| --- | --- | --- |
| home-01 | 主页整体版式（§6.1） | 极简框架 + 高饱和色块分区 + 取景框角标/虚线框装饰 + 手写花体板块分隔 + 页脚大色块 |
| home-02 | 文章 Hero 整区换色（§6.1-2、§2.7 T3） | 整个 Hero 同版式换色即换氛围——T3 整区换色的原型 |
| home-03 | 汉堡菜单导航（§5 Navbar、A3） | 全屏色块覆盖 + 分组分列（组标题反色大写、条目主体色） |
| home-04 | 主页卡片 hover（§2.7） | hover 态卡片：分类+标题+简介整体切换为另一套色调——双主题色调直接证据 |
| article-01/02 | 文章详情结构 + 吸顶标题栏（§6.3、§8.1） | 右缘竖排 RELATED ARTICLES（宽字距、透明底）；吸顶为通栏色块横幅（LOGO+标题+SHARE） |
| article-03/04 | RELATED ARTICLES 侧边栏（§8.2） | hover 卡片文字染色；点击后左侧滑出列表面板 + 主区淡染色 + 右缘按钮变 CLOSE |
| book.png | 书籍推荐卡片版式（§6.1-5、§6.7） | 封面居中微缩 + 书名 + 作者 + 灰色小字标签 |
| photo-01 | Lens 图集卡片（§6.4） | 放大镜卡片，本项目去除底部按钮整卡可点 |

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

1. ~~设计基调确认~~ ✅ 已完成（2026-08-24）：站长选定方案 D · 羊皮卷（展示页 design/moodboards/style-d.html，仅本地保留不入库），§2 tokens 与 §3 LOGO 已定稿；阶段 2 前端按本文档落地
2. ~~v2.0 交互体系升级~~ ✅ 方案已确认（2026-08-31）：§2.7 双主题色调系统、§2.8 微交互 44 项清单、§8.2 侧边栏重做已定稿；**§2.8 各项动画在 UI 视觉打磨阶段逐项落地并自测**（每项需验证 `prefers-reduced-motion` 降级）
3. **magicui 依赖未引入（2026-08-27 标注，站长已确认延后）**：阶段 2 前端动效为联调用语义简化版（framer-motion 自实现），非最终效果——Hero 图片 LOGO 底 + kinetic-text 逐字字重动画 + 光环苏醒、AetherRing 加载/版权区滚动展开动画（§3.4）、热门卡片 kinetic-text 排名数字与 interactive-hover-button "阅读"按钮等均待 UI 视觉打磨阶段按 §7 清单引入 magicui 统一补齐
4. **MorphingText 竖排兼容性待验证（v2.0 新增）**：morphing-text 在 `writing-mode: vertical-rl` 下的渲染行为未验证，若异常则降级为 text-3d-flip 或直接文字替换（保留逐字跳动）；SideTab 每字 span 化与 `--i` 序号在 i18n 切换（中/英词条长度不同）时需重新计算 stagger
5. 中文衬线字体（标题 Noto Serif SC 900 + 正文 Noto Serif SC）体积较大且全站使用，需子集化/按需加载（`next/font` display=swap），必要时正文中文考虑系统宋体栈直用（免下载）
6. ArtPlayer 清晰度切换依赖多码率源文件，站长上传单文件时该按钮隐藏（预留多码率扩展）
7. 歌词时间轴：LRC 优先 + 纯文本均分兜底 + 打点校准 + 全局偏移（BackEnd-Plan §7.6）；AI 自动对齐为可选增强
8. ECharts 中国地图 GeoJSON 数据源需随行政区划更新（打包内置）
