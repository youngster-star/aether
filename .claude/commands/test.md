---
description: Aether 项目代码测试（默认全维度；可指定维度）
argument-hint: [backend|frontend|security|comment|functional|efficiency]
---

调用 code-tester 代理执行 Aether 项目测试流程$ARGUMENTS。

- 无参数：全部 6 维度（backend / frontend / security / comment / functional / efficiency）
- 有参数：只测指定维度（如 `/test security` 只做安全测试；支持多个，如 `/test backend security`）
- 全维度通过后写 `.claude/.test-pass.json`（git push 门禁标记，24h 有效）
- 测试报告落盘 `.claude/test-reports/`（不入库）
