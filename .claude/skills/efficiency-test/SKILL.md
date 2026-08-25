---
name: efficiency-test
description: Aether 性能与效率检查。当用户问"是否高效/性能怎么样"或执行 code-tester 的 efficiency 维度时使用。审查变更范围内的性能问题与低效实现。
---

# 效率检查（efficiency）

依据：BackEnd-Plan.md（§4.6 Redis 去重 / §8 存储零拷贝）、UI-Plan.md（§8.4 渲染引擎性能）、Stage.md §1.1（功能符合企业级水平）。

## 检查清单

### 后端

| 检查点 | 说明 |
| --- | --- |
| JPA N+1 查询 | 循环内逐个查询 → 批量/Join Fetch/EntityGraph；关联查询看 SQL 日志（dev profile show-sql） |
| 索引使用 | 高频查询条件（发布状态+时间、类型+时间等）必须有对应索引（对照 §6 DDL 索引） |
| open-in-view=false 遵守 | 事务外禁止触发懒加载；VO 转换在事务内完成 |
| 缓存 | 高频不变数据（分类/标签/问卷选项）可用 Redis 缓存；禁止缓存用户态数据 |
| 大文件处理 | 分片合并 FileChannel.transferTo 零拷贝，禁止字节数组全量读入内存；流式转发禁止全量 buffering |
| 循环内重复查询/重复计算 | 提取到循环外；批处理用 saveAll 而非逐条 save |
| 分页 | 列表接口必须分页（Pageable），禁止全表查出后内存分页 |

### 前端

| 检查点 | 说明 |
| --- | --- |
| 图片懒加载 | 封面/瀑布流用 pixel-image 像素过渡懒加载（UI-Plan §7） |
| 包体控制 | 不引入 Three/PixiJS 等重依赖（UI-Plan §8.4 明确禁止）；新增依赖需说明必要性 |
| 重复请求 | 列表页与详情页之间不重复请求可复用数据；SWR/缓存策略合理 |
| 动画性能 | Canvas 动画 rAF 驱动、粒子计算 OffscreenCanvas + Worker；移动端粒子减半 + 30fps |
| 记忆化 | 大列表组件合理用 memo/useMemo，避免无关状态变化导致全量重渲染 |

## 输出格式

- 问题清单：`文件:行 — 问题 — 影响 — 优化建议`（标注严重程度：高/中/低）
- 结论：PASS / FAIL（存在高危性能问题 → FAIL）
