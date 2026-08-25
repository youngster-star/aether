---
name: frontend-test
description: Aether 前端代码测试。当测试前端、检查 Next.js/React 代码、或执行 code-tester 的 frontend 维度时使用。执行 eslint + build，并按 UI-Plan 规范审查前端代码。
---

# 前端测试（frontend）

依据文档：UI-Plan.md（§1 技术选型 / §2 设计系统 / §9 安全限制 / §10 响应式）、Stage.md。

## 0. Next.js 16 注意

**本版本有 breaking changes**：写检查意见前先确认对应 API 规范，读 `frontend/node_modules/next/dist/docs/` 下相关文档（如 layout/metadata/rewrites/proxy 的差异），检查结果不得基于旧版 Next 习惯误报。

## 1. 确定性测试（先执行）

```bash
cd frontend && npm run lint        # ESLint
cd frontend && npm run build       # 生产构建（含类型检查）
```

- lint 报错 / build 失败 → 本维度直接 FAIL，附完整输出
- 警告项列出但不判 FAIL（累计超过 5 条记问题）

## 2. 代码规范审查（对照 UI-Plan）

| 检查点 | 依据 |
| --- | --- |
| 设计 tokens 用 CSS Variables（globals.css 定义 light/dark 两套：羊皮纸 #F3EAD8 / 墨褐 #2B2118 / 赭石 #8C5B2D / 铜绿 #4F6D5A），禁止硬编码色值 | §2.2 |
| 全站衬线（Source Serif 4 + Noto Serif SC），`--font-display/--font-body` 变量驱动 | §2.3 |
| hover 反色卡片用 `.hover-card` 规则；::selection 全局统一 | §2.6 |
| 页面内容全部来自后端接口，禁止写死内容（文章/列表数据等） | Stage.md §1.1-9 |
| 所有系统文字双语（next-intl，messages/zh.json + en.json），用户内容不翻译 | §11 |
| 图片/媒体统一 ProtectedImage / ProtectedMedia 组件 + 签名 URL，禁止直接暴露存储地址 | §9.2 |
| 富文本渲染前 DOMPurify sanitize | §9.3 |
| 动效尊重 prefers-reduced-motion | §2.5 |
| 响应式断点：lg 完整版式 / md 3 列 / sm 单列 | §10 |
| 路由与页面结构符合 §4.1（(site)/、cryptex/、proxy.ts 鉴权） | §4.1 |

## 3. 输出格式

- 确定性测试结果（命令 + 关键输出行）
- 规范问题清单：`文件:行 — 问题 — 依据（UI-Plan 章节）`
- 结论：PASS / FAIL
