---
name: backend-test
description: Aether 后端代码测试。当测试后端、检查 Java/Spring Boot 代码、或执行 code-tester 的 backend 维度时使用。执行 mvn 编译+单测，并按 BackEnd-Plan 规范审查后端代码质量。
---

# 后端测试（backend）

依据文档：BackEnd-Plan.md（§2 工程结构 / §3 统一返回 / §4 安全 / §8 存储）、Stage.md。

## 1. 确定性测试（先执行）

```bash
# dev profile：H2 内存库，无需 docker（Stage.md §1.3 回退方案）
cd backend && mvn -q -DskipTests compile          # 编译（秒级，先看编译错）
cd backend && mvn test -Dspring.profiles.active=dev   # 全量单测
```

- 编译失败 / 单测失败 → 本维度直接 FAIL，附完整错误输出
- 编译通过但单测覆盖缺失（新增 Service 无对应测试）→ 记为问题项

## 2. 代码规范审查（对照 BackEnd-Plan）

| 检查点 | 依据 |
| --- | --- |
| 统一返回 Result（code/message/data/requestId/timestamp），禁止 Controller 直接返回裸对象 | §3.1 |
| 错误码使用 §3.2 分段（1xxxx 通用 / 2xxxx 认证 / 3xxxx 业务 / 5xxxx 系统），禁止自定义乱码 | §3.2 |
| 异常统一走 BusinessException + @RestControllerAdvice，禁止 try-catch 吞异常 | §3.3 |
| 参数校验用 Jakarta Validation 注解，禁止 Controller 手写 if-else 校验 | §3.3 |
| entity 禁止直接返回前端，统一转 VO；入参统一 DTO | §2 关键约定 |
| Controller 只做参数接收/校验/返回，业务逻辑全在 Service | §2 关键约定 |
| 时间字段 LocalDateTime，对外 JSON 格式 `yyyy-MM-dd HH:mm:ss` | §2 关键约定 |
| 代码中禁止硬编码 `/aether/api` 路径前缀 | §2 关键约定 |
| 数据库操作带事务（写操作 @Transactional），open-in-view=false 下禁止懒加载出事务 | application.yml |
| 新增接口必须与 BackEnd-Plan §5.2 端点清单一致；表结构改动必须同步 §6 | Stage.md §1.2 |

## 3. 输出格式

- 确定性测试结果（命令 + 关键输出行）
- 规范问题清单：`文件:行 — 问题 — 依据（Plan 章节）`
- 结论：PASS / FAIL
