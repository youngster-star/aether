# Aether 项目开发规范（Claude Code 专用）

> 本文件是 Claude Code 在本项目工作的行为规范，内容与 Stage.md 开发规范一致并补充 AI 工作流。

## 测试门禁（强制执行）

1. **git push 前必须完成 `/test` 全流程并通过**——否则 PreToolUse hook 会阻止 push（提示"测试门禁未通过"）
2. 用户触发"测试代码 / 检查代码 / 安全测试 / 代码审查 / 跑一下测试"等关键词时，主动执行测试流程（可按用户指定维度）
3. 测试体系位于 `.claude/`（code-tester agent + 6 个维度 skill + /test 命令 + 门禁 hook），**与项目业务 `agent/`（Python 分章服务）完全无关，禁止混淆**
4. 纯文档改动（仅 .md/.txt/.gitignore）不受门禁限制；`git push --no-verify` 可强制跳过（仅限紧急情况，需向用户说明）
5. **push 含代码变更时必须先更新 Stage.md**（§1.4 提交记录表 + 当前阶段进度 + 遗留问题），门禁 hook 强制校验"推送范围内含 Stage.md 变更"，否则阻止 push

## 提交规范

1. **commit 署名仅 dkb，禁止添加 `Co-Authored-By: Claude`**
2. Conventional Commits：`feat(article): 支持文章独立样式`；类型 feat/fix/docs/style/refactor/test/chore
3. 分支：main（仅合并）/ dev（日常开发）/ feature/*；PR 单次改动 ≤ 400 行
4. push 到 remote `dkb`（`git push dkb dev`）
5. 提交前自查 → 测试门禁通过后才 push（见上）

## 文档同步

- 改动 API 或表结构**必须同步更新 BackEnd-Plan §5/§6、UI-Plan**（先改文档再改代码，Stage.md §1.2）
- 任何与 Plan 文档不一致的实现，先改文档再改代码；阶段完成后更新 Stage.md"当前阶段"状态
- 开发依据优先级：Stage.md（阶段/规范）> BackEnd-Plan.md（后端）/ UI-Plan.md（前端）> README.md（对外说明）

## 环境要点

- 后端测试/本地运行用 dev profile（H2 内存库）：`mvn -Dspring.profiles.active=dev`
- Docker 环境项目末期统一安装（当前不要求 docker）
- Next.js 16 存在 breaking changes：写前端代码前先读 `frontend/node_modules/next/dist/docs/` 对应文档
- 本机网络封锁 `github.com:22`：已配置 `~/.ssh/config` 走 `ssh.github.com:443`（SSH over 443），push 无需额外操作
