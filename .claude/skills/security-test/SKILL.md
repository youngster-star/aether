---
name: security-test
description: Aether 安全测试。当用户说"安全测试/安全检查"或执行 code-tester 的 security 维度时使用。按 BackEnd-Plan §4 安全清单逐项审查，并调用内置 security-review 补充。
---

# 安全测试（security）

依据文档：BackEnd-Plan.md §4（安全设计）、§8.4（上传白名单）、UI-Plan §9。

## 1. 检查清单（对照变更范围逐项核对）

| 检查点 | 依据 |
| --- | --- |
| 认证：/admin/** 接口必须鉴权；未认证返回 20001 | §4.2 |
| 登录防护三级齐全：限流（IP 每分钟 5 次）/ 失败 3 次验证码 / 失败 10 次锁定 15min 指数翻倍 | §4.1 |
| 密码：Argon2 哈希；生产强度校验（≥12 位含大小写+数字+特殊字符）；默认密码仅 dev | §4.1 |
| JWT：Access 30min + Refresh 7d HttpOnly Cookie；refresh 白名单轮换、登出拉黑；密钥环境变量注入，禁止硬编码 | §4.2 |
| 媒体防下载：所有媒体走签名 URL（HMAC-SHA256，fileId+expires）；视频 Range 支持；OSS 私有 Bucket + 短时效签名 | §4.4 |
| XSS：富文本入库前 jsoup 白名单 sanitize；前端 DOMPurify；AI sandbox 代码仅 iframe sandbox 执行 | §4.3、UI-Plan §9.3 |
| SQL 注入：禁止字符串拼接 SQL，JPA 参数绑定/Specification | §4.3 |
| 上传：扩展名 + 魔数双重校验；大小白名单（图 50MB/视频 2GB/音频 200MB/文本 100MB）；存储名 UUID 防路径穿越；EXIF 抹除 | §4.5、§8.4 |
| 脱敏：日志/操作日志中 password/token 等敏感字段替换 ***；对外异常不泄露堆栈 | §3.3、§10 |
| 配置：数据库/Redis/OSS/DeepSeek/SMTP 凭据全部环境变量外置，仓库内禁止出现真实密钥 | §11.2 |
| 限流：公开接口（AI 对话 10/min + 50/day、登录 5/min）Redis 实现 | §7.5、§4.1 |

## 2. 补充动作

- 对变更 diff 调用内置 security-review skill 做补充审查（如有新依赖、新接口面）
- 检查新增依赖是否有已知 CVE（可疑时用 WebSearch 查证，不确定标注"待验证"）

## 3. 输出格式

- 逐项 PASS/FAIL 表
- 漏洞清单：`文件:行 — 漏洞 — 风险等级（高/中/低）— 修复建议`
- 结论：PASS / FAIL（任一高危 → FAIL）
