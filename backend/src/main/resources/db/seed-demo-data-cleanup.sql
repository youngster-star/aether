-- =====================================================================
-- Aether 演示测试数据 · 删除脚本（MySQL 8.4，与 seed-demo-data.sql 配套）
--
-- 删除范围：seed-demo-data.sql 插入的全部业务数据与 storage 登记
-- 保留数据：分类/标签/问卷选项/AI 配置/管理员账户/文章样式
--           （字典与可复用配置，与 DataSeeder 数据同等待遇）
-- 物理文件：媒体文件位于本地存储 backend/data/storage/files/seed/，
--           本脚本不动磁盘，需手动删除该目录：
--           rm -rf backend/data/storage/files/seed/
-- 执行：mysql -uroot -p aether < backend/src/main/resources/db/seed-demo-data-cleanup.sql
-- =====================================================================

SET NAMES utf8mb4;

-- 1. AI 链路数据
DELETE FROM ai_chat_message WHERE id BETWEEN 90288 AND 90291;
DELETE FROM ai_chat_session WHERE id BETWEEN 90286 AND 90287;
DELETE FROM ai_generation_task WHERE id BETWEEN 90292 AND 90293;

-- 2. 日志与统计
DELETE FROM operation_log WHERE id BETWEEN 90281 AND 90285;
DELETE FROM visitor_daily_stat WHERE id BETWEEN 90263 AND 90277;
DELETE FROM visitor_log WHERE id BETWEEN 90241 AND 90262;

-- 3. 订阅与问卷
DELETE FROM survey WHERE id BETWEEN 90231 AND 90232;
DELETE FROM subscriber WHERE id BETWEEN 90221 AND 90224;

-- 4. 公告
DELETE FROM announcement WHERE id BETWEEN 90211 AND 90215;

-- 5. 分类标签关联（biz_id 覆盖本脚本全部业务记录区间）
DELETE FROM biz_category_rel WHERE biz_id BETWEEN 90110 AND 90299;
DELETE FROM biz_tag_rel WHERE biz_id BETWEEN 90110 AND 90299;

-- 6. 视频与章节
DELETE FROM video_chapter WHERE id BETWEEN 90161 AND 90168;
DELETE FROM video WHERE id BETWEEN 90151 AND 90154;

-- 7. 图集与图片
DELETE FROM album_image WHERE id BETWEEN 90131 AND 90142;
DELETE FROM album WHERE id BETWEEN 90121 AND 90125;

-- 8. 书籍（分章任务 → 章节 → 书籍）
DELETE FROM book_ai_task WHERE id BETWEEN 90206 AND 90209;
DELETE FROM book_chapter WHERE id BETWEEN 90195 AND 90200;
DELETE FROM book WHERE id BETWEEN 90191 AND 90193;

-- 9. 音乐（曲目 → 合集）
DELETE FROM music WHERE id BETWEEN 90181 AND 90186;
DELETE FROM music_album WHERE id BETWEEN 90171 AND 90172;

-- 10. 文章
DELETE FROM article WHERE id BETWEEN 90110 AND 90120;

-- 11. storage 登记（引用 → 文件元数据）
DELETE FROM storage_ref WHERE file_id BETWEEN 90001 AND 90013;
DELETE FROM storage_file WHERE id BETWEEN 90001 AND 90013;

-- 保留（不删除）：category / tag / survey_option / ai_config / sys_user / article_style
-- 提示：物理媒体文件 backend/data/storage/files/seed/ 需手动删除（见文件头注释）
