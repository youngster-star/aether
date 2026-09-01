-- =====================================================================
-- Aether 演示测试数据 · 插入脚本（MySQL 8.4，BackEnd-Plan §6.4）
--
-- 设计说明：
--   * ID 固定 90001+ 区间，与 DataSeeder 自增数据互不冲突；全部语句幂等，
--     重复执行会先清理本区间旧数据再插入（不影响 DataSeeder 数据）
--   * 字典数据（分类/标签/问卷选项/AI 配置/管理员）由 DataSeeder 生成；
--     本脚本对缺失项（announcement 分类等）INSERT IGNORE 补插，保证独立可跑
--   * 媒体文件为真实网络素材，存放于本地存储 backend/data/storage/files/seed/
--     （不入库）；先执行 scripts/download-demo-media.sh 下载后再跑本脚本
--   * 所有字段值彼此一致：字数/时长/宽高/哈希与文件实际值一致，
--     日统计与访问日志逐条吻合，storage_ref 与全部文件引用一一对应
--   * 删除脚本见 seed-demo-data-cleanup.sql（保留分类/标签等字典数据）
-- 执行：mysql -uroot -p aether < backend/src/main/resources/db/seed-demo-data.sql
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. 幂等：清理本脚本 ID 区间旧数据（按依赖顺序）
-- ---------------------------------------------------------------------
DELETE FROM ai_chat_message WHERE id BETWEEN 90288 AND 90291;
DELETE FROM ai_chat_session WHERE id BETWEEN 90286 AND 90287;
DELETE FROM ai_generation_task WHERE id BETWEEN 90292 AND 90293;
DELETE FROM operation_log WHERE id BETWEEN 90281 AND 90285;
DELETE FROM visitor_daily_stat WHERE id BETWEEN 90263 AND 90277;
DELETE FROM visitor_log WHERE id BETWEEN 90241 AND 90262;
DELETE FROM survey WHERE id BETWEEN 90231 AND 90232;
DELETE FROM subscriber WHERE id BETWEEN 90221 AND 90224;
DELETE FROM announcement WHERE id BETWEEN 90211 AND 90215;
DELETE FROM biz_category_rel WHERE biz_id BETWEEN 90110 AND 90299;
DELETE FROM biz_tag_rel WHERE biz_id BETWEEN 90110 AND 90299;
DELETE FROM video_chapter WHERE id BETWEEN 90161 AND 90170;
DELETE FROM video WHERE id BETWEEN 90151 AND 90154;
DELETE FROM album_image WHERE id BETWEEN 90131 AND 90145;
DELETE FROM album WHERE id BETWEEN 90121 AND 90125;
DELETE FROM book_ai_task WHERE id BETWEEN 90206 AND 90209;
DELETE FROM book_chapter WHERE id BETWEEN 90195 AND 90205;
DELETE FROM book WHERE id BETWEEN 90191 AND 90193;
DELETE FROM music WHERE id BETWEEN 90181 AND 90186;
DELETE FROM music_album WHERE id BETWEEN 90171 AND 90172;
DELETE FROM article WHERE id BETWEEN 90110 AND 90120;
DELETE FROM storage_ref WHERE file_id BETWEEN 90001 AND 90013;
DELETE FROM storage_file WHERE id BETWEEN 90001 AND 90013;
-- article_style 保留（样式为可复用配置，同分类/标签待遇，不随数据删除）

-- ---------------------------------------------------------------------
-- 1. 字典补插（INSERT IGNORE：DataSeeder 已生成则跳过，未生成则补齐）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO category (id, name, slug, biz_type, sort) VALUES
(90201, '公告', 'notice', 'announcement', 0),
(90202, '动态', 'dynamic', 'announcement', 1),
(90203, '新闻', 'news', 'announcement', 2);

-- ---------------------------------------------------------------------
-- 2. storage_file：真实网络素材的存储登记（与上传链路产物同构）
--    object_key = files/seed/<文件名>，哈希/尺寸/宽高/时长为文件实际探测值
-- ---------------------------------------------------------------------
INSERT INTO storage_file (id, original_name, storage_type, object_key, size, file_md5,
                          mime_type, ext, width, height, duration, meta, status) VALUES
-- 图片（7 张，覆盖 webp/jpg/png 三种格式与横竖构图）
(90001, 'img-huaban-01.webp', 1, 'files/seed/img-huaban-01.webp', 101764,
 'c58f449b52c9b435be8f276b3140a92ddda8b6f6bc209d872b5f667fae9d7421',
 'image/webp', 'webp', 658, 444, NULL, NULL, 1),
(90002, 'img-699pic-6903-wh300.jpg', 1, 'files/seed/img-699pic-6903-wh300.jpg', 74980,
 '7c6ca5ae1e5aea71a4ed758f295aa360f15ca87d35ddd31eea33ed917f7f912e',
 'image/jpeg', 'jpg', 449, 300, NULL, NULL, 1),
(90003, 'img-699pic-9776-wh860.jpg', 1, 'files/seed/img-699pic-9776-wh860.jpg', 267313,
 'e7e1db3a971595d5432858a1ae9c27e20a08dc03ac1af2d5981090ce685f902f',
 'image/jpeg', 'jpg', 860, 573, NULL, NULL, 1),
(90004, 'img-699pic-8720-wh860.jpg', 1, 'files/seed/img-699pic-8720-wh860.jpg', 389868,
 '91d6ee4695818b591a0ee14b13692b5504b37fa94a9dc2612e72f6fc2af64927',
 'image/jpeg', 'jpg', 860, 564, NULL, NULL, 1),
(90005, 'img-699pic-4467-wh860.jpg', 1, 'files/seed/img-699pic-4467-wh860.jpg', 162321,
 '6bdf2e14c6e8efd9a6987165003b8fbd5c31bc476b3fd54365fc42447eb1842e',
 'image/jpeg', 'jpg', 860, 573, NULL, NULL, 1),
(90006, 'img-shetu66-01.png', 1, 'files/seed/img-shetu66-01.png', 1104033,
 '1e971e6466ec92b133541cf12ae00eff7ae758190fc701fed5b20fbd54d3ce5f',
 'image/png', 'png', 1024, 574, NULL, NULL, 1),
(90007, 'img-699pic-9854-wh300.jpg', 1, 'files/seed/img-699pic-9854-wh300.jpg', 65532,
 '40c4400eda28012f750df30ecd6033a80d42828aff48f839707fe5a025822b1f',
 'image/jpeg', 'jpg', 451, 300, NULL, NULL, 1),
-- 视频（3 个真实 MP4，时长 mvhd 实测；01 被两条视频记录复用，验证同文件多业务引用）
(90008, 'video-cri-01.mp4', 1, 'files/seed/video-cri-01.mp4', 71567983,
 '052254ed42efcf03fe2c02327b8837b5928404c4c799b8f9fc8381b5c86de894',
 'video/mp4', 'mp4', NULL, NULL, 136, NULL, 1),
(90009, 'video-cri-02.mp4', 1, 'files/seed/video-cri-02.mp4', 60329184,
 'b10d1d9d61872c77f92ee395894bbbbd77cd3490ae9545cc8969627e35782d37',
 'video/mp4', 'mp4', NULL, NULL, 156, NULL, 1),
(90010, 'video-cri-03.mp4', 1, 'files/seed/video-cri-03.mp4', 10285197,
 '68a09e1ec9508ad1e8d55ab40b9a317ec3dc1f89cd38cceb9f9b8a136e16aef5',
 'video/mp4', 'mp4', NULL, NULL, 85, NULL, 1),
-- 音频（3 首 SoundHelix 测试音源，时长 mp3 帧遍历实测；01/03/08 各被两首曲目复用）
(90011, 'audio-sh-01.mp3', 1, 'files/seed/audio-sh-01.mp3', 8945229,
 'cadd2666c5571e12fafdb21697b26aef6f196a5a9c28e708129870167679991d',
 'audio/mpeg', 'mp3', NULL, NULL, 373, NULL, 1),
(90012, 'audio-sh-03.mp3', 1, 'files/seed/audio-sh-03.mp3', 8258104,
 '793e6ffda56e28691998b90a55bdaf0f2bccb8950cd8a7ee9ef7421c4ebbafdb',
 'audio/mpeg', 'mp3', NULL, NULL, 344, NULL, 1),
(90013, 'audio-sh-08.mp3', 1, 'files/seed/audio-sh-08.mp3', 7807352,
 'd64c2b0682632a56eeec9eceb852853e30458ce8b84ef00b21685666c9d9bf4a',
 'audio/mpeg', 'mp3', NULL, NULL, 325, NULL, 1);

-- ---------------------------------------------------------------------
-- 3. article_style：文章独立样式（默认羊皮卷样式由 DataSeeder 生成，
--    这里补 4 个变体，覆盖无衬线/紧凑窄栏/大字/引用型，结构见 §6.3）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO article_style (id, name, style_json, is_default) VALUES
(90101, '现代无衬线',
 '{"fontFamily":"system-ui, -apple-system, sans-serif","fontSize":16,"lineHeight":1.8,"letterSpacing":0,"wordSpacing":0,"paragraphSpacing":14,"firstLineIndent":"0em","contentWidth":760,"themeColor":"#2F6FED","serif":false,"customCss":""}',
 0),
(90102, '紧凑窄栏',
 '{"fontFamily":"Noto Sans SC","fontSize":15,"lineHeight":1.7,"letterSpacing":0,"wordSpacing":0,"paragraphSpacing":10,"firstLineIndent":"2em","contentWidth":640,"themeColor":"#4A7A5C","serif":false,"customCss":"h2{border-left:4px solid #4A7A5C;padding-left:10px}"}',
 0),
(90103, '大字阅读',
 '{"fontFamily":"Noto Serif SC","fontSize":20,"lineHeight":2,"letterSpacing":1,"wordSpacing":2,"paragraphSpacing":20,"firstLineIndent":"2em","contentWidth":680,"themeColor":"#A0522D","serif":true,"customCss":""}',
 0),
(90104, '摘录引用型',
 '{"fontFamily":"Noto Serif SC","fontSize":17,"lineHeight":1.9,"letterSpacing":0,"wordSpacing":0,"paragraphSpacing":18,"firstLineIndent":"2em","contentWidth":700,"themeColor":"#6B4E3D","serif":true,"customCss":"blockquote{font-style:italic;color:#6B4E3D;border-left:3px solid #C9A86B;margin:20px 0;padding:4px 16px}"}',
 0);

-- ---------------------------------------------------------------------
-- 4. article：11 篇覆盖全类型
--    长文（多级标题/表格/代码块/引用）/短文/多图/带站内视频链接/草稿/
--    热门（is_hot+hot_order）/独立样式（90101-90104，NULL=默认羊皮卷）
--    字数与 content_html 纯文本一致（word_count 经脚本校验）
-- ---------------------------------------------------------------------

-- 4.1 长文·旅行记录（h2/h3 多级 + 装备表格 + 引用 + 列表；样式 90101 现代无衬线）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90110, '在秦岭深处寻找秋色：一场为期三天的徒步记录', 90003,
 '十月的秦岭是调色盘。我们背起行囊，沿着褒斜古道深入腹地，用三天时间记录下这场一年一度的色彩盛宴。',
 '<h2>行程规划</h2><p>路线选择褒斜古道南段：留坝—火烧店—江口镇，全程约 42 公里，累计爬升 1800 米。十月下旬海拔 1500 米以上红叶进入最佳观赏期，气温 3-15℃，需要全套防风装备。</p><table><thead><tr><th>装备</th><th>说明</th><th>重量</th></tr></thead><tbody><tr><td>冲锋衣</td><td>三层压胶，防风防雨</td><td>420g</td></tr><tr><td>睡袋</td><td>舒适温标 -5℃</td><td>980g</td></tr><tr><td>炉头</td><td>一体式气炉</td><td>76g</td></tr><tr><td>摄影器材</td><td>单反 + 24-70 镜头</td><td>2100g</td></tr></tbody></table><h2>第一天：进入山门</h2><p>清晨六点从留坝县城出发，雾气还压在山谷里。沿着溪流逆流而上，水声一路相伴。海拔每上升一百米，山色就变一个层次：先是青绿，然后是青黄交杂，最后是大片的赭红。</p><blockquote>山民说，看红叶要赶在霜降之前，一场霜打下来，叶子红得最艳，也落得最快。</blockquote><h3>火烧店的黄昏</h3><p>傍晚抵达火烧店营地，海拔 2100 米。支好帐篷时天已经黑透，抬头是久违的银河。用炉头煮了一锅面，配着火腿肠和榨菜，简单却满足。</p><h2>第二天：红叶长廊</h2><p>这一天是全程的精华段。古道两侧的枫树与黄栌连成一条长廊，阳光从叶缝里漏下来，路面上全是碎金。拍摄要点记录如下：</p><ul><li>逆光拍摄红叶，叶片透光呈半透明质感；</li><li>阴天用偏振镜压暗天空反光，提高色彩饱和度；</li><li>大景别放小光圈（f/8-f/11），前后景都清晰。</li></ul><p>午后遇到一场阵雨，雨停后整条山谷升起雾气，像有人在山间扯开了薄纱。同行的人都说，这才是秦岭真正的样子。</p><h2>第三天：出山</h2><p>最后一天是长下坡，膝盖负担重，登山杖帮了大忙。中午在江口镇吃到了热汤面，三天来第一次坐在有屋顶的房子里吃饭，恍如隔世。</p><p>回程大巴上翻看照片，九百多张里挑出三十几张能用的。红叶会年年红，但这条古道上的三天，是独一无二的。</p>',
 '## 行程规划\n\n路线选择褒斜古道南段：留坝—火烧店—江口镇，全程约 42 公里。\n\n## 第一天：进入山门\n\n清晨六点从留坝县城出发，雾气还压在山谷里。\n\n> 山民说，看红叶要赶在霜降之前。\n\n## 第二天：红叶长廊\n\n这一天是全程的精华段。\n\n## 第三天：出山\n\n最后一天是长下坡。',
 641, 356, 0, 0, 90101, 1, '2026-08-18 09:30:00');

-- 4.2 短文·随笔（纯段落无标题层级，无样式无封面，走默认羊皮卷样式）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90111, '雨夜听蝉', NULL,
 '夏末的雨夜，窗外竟还有蝉声。断断续续，像谁在深夜里敲一扇关不紧的门。',
 '<p>夏末的雨夜，窗外竟还有蝉声。断断续续，像谁在深夜里敲一扇关不紧的门。</p><p>蝉的一生大多在土里，见天日的日子不过几周。它们拼尽全力地叫，大约是知道自己时间不多。人常说蝉鸣聒噪，可今夜听来，倒像是一首挽歌。</p><p>雨声渐渐盖过了蝉声。最后一只蝉，也不知道还坚持了多久。</p>',
 '夏末的雨夜，窗外竟还有蝉声。断断续续，像谁在深夜里敲一扇关不紧的门。',
 125, 89, 0, 0, NULL, 1, '2026-08-24 21:15:00');

-- 4.3 多图文章（figure/figcaption 外链配图，测正文图片渲染；样式 90104）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90112, '四月的花事：一组春日影像', 90002,
 '整理硬盘时翻出四月拍的一批花。索性挑几张最喜欢的放出来，配上当时的心情，权当补记一场春天。',
 '<p>整理硬盘时翻出四月拍的一批花。索性挑几张最喜欢的放出来，配上当时的心情，权当补记一场春天。</p><figure><img src="https://img95.699pic.com/photo/50464/9776.jpg_wh860.jpg" loading="lazy" alt="海棠" /><figcaption>图一：院子里的海棠，开得没心没肺</figcaption></figure><p>第一张是海棠。四月上旬，院子里的海棠一夜之间全开了，粉白粉白的一树，像谁把云彩挂在了枝头。</p><figure><img src="https://img95.699pic.com/photo/50059/8720.jpg_wh860.jpg" loading="lazy" alt="樱花大道" /><figcaption>图二：樱花大道的清晨，还没有游客</figcaption></figure><p>樱花要趁早去。七点前的大道空无一人，只有环卫工人的扫帚声。花瓣落在地上，扫成一条粉色的河。</p><figure><img src="https://img95.699pic.com/photo/50465/4467.jpg_wh860.jpg" loading="lazy" alt="郁金香花田" /><figcaption>图三：植物园的郁金香，颜色浓得像油画</figcaption></figure><p>郁金香是最会摆姿势的花，整整齐齐站成方阵，每一朵都像在等着被拍。</p><figure><img src="https://img95.699pic.com/photo/50064/9854.jpg_wh300.jpg" loading="lazy" alt="路边野花" /><figcaption>图四：路边不知名的野花，反而最有生命力</figcaption></figure><p>最后一张是路边的野花。叫不出名字，也不需要名字。它们不管有没有人看，都开得认真。</p><p>春天的花事到四月末就散了。照片留着，明年春天再看。</p>',
 '整理硬盘时翻出四月拍的一批花。\n\n![海棠](https://img95.699pic.com/photo/50464/9776.jpg_wh860.jpg)\n\n第一张是海棠。\n\n![樱花](https://img95.699pic.com/photo/50059/8720.jpg_wh860.jpg)\n\n樱花要趁早去。',
 301, 233, 0, 0, 90104, 1, '2026-08-20 15:40:00');

-- 4.4 带站内视频的文章（相对链接指向视频详情/图集详情，测 preserveRelativeLinks）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90113, '一部关于迁徙的纪录片：《河流之上》', 90007,
 '把镜头对准一条河，就拍到了所有迁徙者的影子：鱼、鸟，还有人。《河流之上》的克制与野心都值得一说。',
 '<p>把镜头对准一条河，就拍到了所有迁徙者的影子：鱼、鸟，还有人。</p><p>《河流之上》是最近看过的纪录片里最安静的一部。没有解说词轰炸，也没有刻意煽情，摄影师用了七个月跟拍一条河的四季。相关片段已上传小站，<a href="/aether/videos/90151">点此在线观看</a>。</p><h2>关于镜头语言</h2><p>导演偏爱大远景，人和船在画面里小得像标点。但正是这种小，让人想起河流的尺度——它不在乎任何一条船。</p><blockquote>河从不说话，它只是流。</blockquote><h2>关于配乐</h2><p>配乐大量使用环境声：水声、风声、远处渡轮的汽笛。音乐只在两个段落出现，一次是迁徙的鸟群起飞，一次是结尾的冰层开裂。</p><p>看完之后我又去翻了站里的图集，<a href="/aether/albums/90121">秦岭秋色</a>里的几张照片，气质上和这部片子出奇地像。也许山和水本就是一回事。</p><p>延伸阅读：<a href="https://www.cri.cn" target="_blank" rel="noopener noreferrer">制片方官网</a>上有完整的幕后手记。</p>',
 '把镜头对准一条河，就拍到了所有迁徙者的影子。\n\n相关片段：[点此在线观看](/aether/videos/90151)\n\n## 关于镜头语言\n\n导演偏爱大远景。\n\n## 关于配乐\n\n配乐大量使用环境声。',
 301, 178, 0, 0, NULL, 1, '2026-08-22 20:05:00');

-- 4.5 代码长文（pre/code/kbd/mark，样式 90102 紧凑窄栏）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90114, '用 Spring Boot 4 与 Next.js 16 搭建个人站：全栈实践笔记', NULL,
 '从零搭一个个人站：后端 Spring Boot 4 + 前端 Next.js 16，外加 MySQL 与 Redis。记录关键配置与踩过的坑。',
 '<p>从零搭一个个人站，技术选型定了很久，最后落在 Java 与 TypeScript 这套组合上。这篇笔记记录关键配置与踩过的坑。</p><h2>后端：context-path 与统一返回</h2><p>全站挂在 /aether 路径下，后端配置：</p><pre><code class="language-yaml">server:\n  servlet:\n    context-path: /aether/api</code></pre><p>所有接口返回统一结构 <mark>Result</mark>，包含 code/message/data/requestId/timestamp，异常由 @RestControllerAdvice 全局兜底。</p><h2>前端：Next.js 16 的 basePath</h2><p>Next.js 16 需要按文档确认 breaking changes，配置如下：</p><pre><code class="language-js">// next.config.ts\nconst nextConfig = {\n  basePath: "/aether",\n};\nexport default nextConfig;</code></pre><p>开发时按 <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>R</kbd> 强刷可以绕过部分缓存问题。</p><h2>建库</h2><pre><code class="language-sql">CREATE DATABASE aether DEFAULT CHARSET utf8mb4;</code></pre><p>字符集务必 utf8mb4，不然 emoji 会炸。</p><h2>小结</h2><p>整套跑通之后最大的感受是：选型不重要，把每层的边界守清楚才重要。后端只管数据，前端只管呈现，中间靠一份写清楚的 API 文档。</p>',
 '从零搭一个个人站，技术选型定了很久。\n\n## 后端：context-path 与统一返回\n\n```yaml\nserver:\n  servlet:\n    context-path: /aether/api\n```\n\n## 前端：Next.js 16 的 basePath\n\n```js\nconst nextConfig = { basePath: "/aether" };\n```',
 575, 512, 0, 0, 90102, 1, '2026-08-15 10:00:00');

-- 4.6 草稿（is_published=0，前端列表不可见，管理端可见，测发布状态过滤）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90115, '未完成的思考：关于 RSS 的复兴', NULL,
 '草稿：最近看到不少人在重提 RSS。信息过载的时代，也许订阅制才是出路。还没想清楚，先记几笔。',
 '<p>最近看到不少人在重提 RSS。信息过载的时代，也许订阅制才是出路。</p><p>算法推荐的问题在于：它太了解你了，以至于你再也遇不到意料之外的东西。</p><p>RSS 的复兴大概还要等一个契机。这篇先放着，想清楚再发。</p>',
 '草稿：最近看到不少人在重提 RSS。还没想清楚，先记几笔。',
 96, 0, 0, 0, NULL, 0, NULL);

-- 4.7 读书笔记（大段引用 + 有序列表 + 分隔线；样式 90104）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90116, '重读《局外人》：荒谬与真实', NULL,
 '第三次读《局外人》。年轻时读出的是冷漠，现在读出的是一种笨拙的诚实。',
 '<p>第三次读《局外人》。年轻时读出的是冷漠，现在读出的是一种笨拙的诚实。</p><blockquote>今天，妈妈死了。也许是昨天，我不知道。</blockquote><p>开篇第一句就定了全书的调子。默尔索不是没有感情，他只是拒绝表演感情。葬礼上他不哭，因为他的悲伤不属于任何观众。</p><blockquote>我体验到这个世界如此像我，如此友爱，我觉得我过去曾经是幸福的，我现在仍然是幸福的。</blockquote><p>审判一节是全书的机关：法庭审判的不是杀人，而是他的生活方式。因为他在母亲的葬礼上没哭，所以他该死。荒谬感在此达到顶点。</p><hr /><h2>重读的三个发现</h2><ol><li>默尔索对自然光线的敏感远超常人，太阳几乎是一个角色；</li><li>玛丽与雷蒙都不是配角，他们是"正常生活"的标本；</li><li>结尾的爆发不是疯癫，而是全书唯一一次彻底的诚实。</li></ol><p>加缪说默尔索是"一个不耍花招的人"。这句话可以当作全书的墓志铭。</p>',
 '第三次读《局外人》。\n\n> 今天，妈妈死了。也许是昨天，我不知道。\n\n默尔索不是没有感情，他只是拒绝表演感情。\n\n## 重读的三个发现\n\n1. 默尔索对自然光线的敏感远超常人。',
 321, 421, 0, 0, 90104, 1, '2026-08-10 14:20:00');

-- 4.8 影评（中篇纯文字，em/strong/s 行内标记 + 定义列表）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90117, '默片时代的光与影', NULL,
 '补完一部 1927 年的默片之后，突然理解了为什么有人说电影是"光的音乐"。',
 '<p>补完一部 1927 年的默片之后，突然理解了为什么有人说电影是<em>光的音乐</em>。</p><p>没有对白，没有音效，只有钢琴伴奏和画面。可正因如此，<strong>每一个镜头都必须自己说话</strong>。</p><h2>默片的三件武器</h2><dl><dt>特写</dt><dd>一张脸就是一场戏。默片演员用表情完成今天的全部台词。</dd><dt>字幕卡</dt><dd>极简的文字插入，像诗的分行。</dd><dt>剪辑节奏</dt><dd>没有声音之后，节奏反而被推到了台前。</dd></dl><p>有人说有声电影<s>终结</s>了默片。准确地说，有声电影终结的只是默片的生产，没有终结它的语言。</p><p>今天的每一部电影里，都住着默片的鬼魂。</p>',
 '补完一部 1927 年的默片之后，突然理解了为什么有人说电影是"光的音乐"。\n\n## 默片的三件武器\n\n特写、字幕卡、剪辑节奏。',
 210, 265, 0, 0, NULL, 1, '2026-08-05 18:45:00');

-- 4.9 热门长文（is_hot=1 hot_order=100，时间线表格；样式 90103 大字阅读）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90118, '以太小站建站全记录：从零到上线的一百天', 90001,
 '一百天，从一行代码到正式上线。这篇长文完整记录 Aether 小站的建设全过程：选型、踩坑、与那些值得纪念的时刻。',
 '<p>一百天，从一行代码到正式上线。这篇长文完整记录 Aether 小站的建设全过程。</p><h2>第 1-30 天：地基</h2><p>最难的从来不是写代码，而是做决定。前后端选型、数据库、部署方式，每一项都调研了一周以上。最终确定：<strong>Java 21 + Spring Boot 4</strong> 做后端，<strong>Next.js 16</strong> 做前端，MySQL 8.4 做存储。</p><table><thead><tr><th>时间</th><th>里程碑</th></tr></thead><tbody><tr><td>第 1 天</td><td>仓库初始化，脚手架跑通</td></tr><tr><td>第 15 天</td><td>登录防护与统一返回体系完成</td></tr><tr><td>第 30 天</td><td>测试门禁体系上线，push 前自动测试</td></tr><tr><td>第 60 天</td><td>文章模块前后端全链路打通</td></tr><tr><td>第 90 天</td><td>图集与视频模块上线</td></tr><tr><td>第 100 天</td><td>部署到生产，正式对外</td></tr></tbody></table><h2>第 31-60 天：第一个闭环</h2><p>文章模块是第一个完整的业务闭环。从表结构到 CRUD，从列表页到详情页，从搜索到阅读统计。上线那天自己把每一篇测试文章都点了一遍，像验收自己家装修。</p><h2>第 61-90 天：多媒体</h2><ul><li>图集：瀑布流 + 放大镜卡片 + 防下载预览；</li><li>视频：签名 URL 播放 + 断点续传 + 关键节点跳转；</li><li>安全：EXIF 抹除、XSS 双保险、签名时效十分钟。</li></ul><h2>第 91-100 天：上线</h2><p>最后的十天全部给了部署与回归：nginx 的 path 配置、HTTPS、备份 cron、全链路回归测试。上线那天夜里十一点，第一次从公网打开自己的站，看了很久。</p><blockquote>网站会一直更新下去，就像以太，无处不在，永不停息。</blockquote>',
 '一百天，从一行代码到正式上线。\n\n## 第 1-30 天：地基\n\n最难的从来不是写代码，而是做决定。\n\n## 第 31-60 天：第一个闭环\n\n文章模块是第一个完整的业务闭环。\n\n## 第 61-90 天：多媒体\n\n## 第 91-100 天：上线',
 572, 1024, 1, 100, 90103, 1, '2026-08-01 12:00:00');

-- 4.10 热门短文（一图一文，is_hot=1 hot_order=80）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90119, '每日一图：山间晨雾', 90006,
 '今天的图来自清晨六点的山口。雾把山分成了好几层，近的浓，远的淡，像一幅没干的水墨画。',
 '<figure><img src="https://img.shetu66.com/2023/07/04/1688453333865029.png" loading="lazy" alt="山间晨雾" /><figcaption>清晨六点，山口起雾</figcaption></figure><p>今天的图来自清晨六点的山口。雾把山分成了好几层，近的浓，远的淡，像一幅没干的水墨画。</p><p>拍雾要趁早，太阳一出来，雾散得比什么都快。前后不过二十分钟，山谷就恢复了它平常的样子。好在快门已经按下了。</p>',
 '今天的图来自清晨六点的山口。\n\n![山间晨雾](https://img.shetu66.com/2023/07/04/1688453333865029.png)\n\n拍雾要趁早。',
 104, 886, 1, 80, NULL, 1, '2026-08-23 06:30:00');

-- 4.11 百科式长文（上标下标/表格/标记线，样式 90103）
INSERT INTO article (id, title, cover_file_id, summary, content_html, content_md, word_count,
                     reading_count, is_hot, hot_order, article_style_id, is_published, publish_time) VALUES
(90120, '以太的物理学简史', NULL,
 '从亚里士多德的第五元素到爱因斯坦的判决，以太概念的兴衰是物理学史上最迷人的一章。',
 '<p>从亚里士多德的第五元素到爱因斯坦的判决，以太概念的兴衰是物理学史上最迷人的一章。</p><h2>古希腊：第五元素</h2><p>亚里士多德认为，月下世界由土水火气构成，而月亮之上的天界由第五种元素构成——<mark>以太（aether）</mark>。这个词的意思是"永远奔跑的"。</p><h2>近代：光的介质</h2><p>十七世纪，惠更斯提出光的波动说。波需要介质，于是以太被请了回来：它弥漫全宇宙，坚硬到能传播每秒 3×10<sup>8</sup> 米的光速，又柔软到天体穿行无碍。</p><table><thead><tr><th>年代</th><th>人物</th><th>事件</th></tr></thead><tbody><tr><td>1678</td><td>惠更斯</td><td>波动说需要以太</td></tr><tr><td>1887</td><td>迈克尔逊-莫雷</td><td>干涉实验零结果</td></tr><tr><td>1905</td><td>爱因斯坦</td><td>狭义相对论宣告以太多余</td></tr></tbody></table><h2>判决</h2><p>迈克尔逊-莫雷实验测不到"以太风"，狭义相对论则让以太彻底失业。<s>以太死了</s>——不，准确地说，以太变成了一种哲学隐喻。</p><p>水分子式是 H<sub>2</sub>O，以太没有化学式。但它留下的遗产还在：<strong>场的概念</strong>。今天的电磁场、引力场，某种意义上就是以太的转世。</p>',
 '从亚里士多德的第五元素到爱因斯坦的判决，以太概念的兴衰是物理学史上最迷人的一章。\n\n## 古希腊：第五元素\n\n## 近代：光的介质\n\n## 判决',
 362, 198, 0, 0, 90103, 1, '2026-08-12 16:10:00');

-- ---------------------------------------------------------------------
-- 5. album / album_image：5 个图集
--    推荐（is_recommend+sort）/非推荐/空图集边界/同文件跨图集复用/
--    封面与图集内图片同文件（storage_ref.ref_count=2 保活场景）
-- ---------------------------------------------------------------------
INSERT INTO album (id, title, cover_file_id, intro, is_recommend, sort) VALUES
(90121, '秦岭秋色', 90006, '十月中旬的秦岭腹地：红叶长廊、山谷晨雾与暮色远山。', 1, 100),
(90122, '街角日常', 90005, '在街头巷尾随手拍下的小日子：花市、橱窗与斑马线。', 1, 80),
(90123, '城市夜行', 90001, '入夜之后的城市露出另一张脸：霓虹、车流与不打烊的便利店。', 0, 0),
(90124, '静物练习', 90004, '阴天室内的一组静物练习，光线柔和，适合慢慢看。', 0, 0),
(90125, '空集待补', NULL, '还没想好放什么的图集（边界用例：零图片）。', 0, 0);

INSERT INTO album_image (id, album_id, file_id, title, intro, sort) VALUES
(90131, 90121, 90006, '山谷晨雾', '清晨薄雾未散时的山谷，远近层次分明', 0),
(90132, 90121, 90003, '山道', '红叶夹道的山路，逆光下叶片透亮', 1),
(90133, 90121, 90004, NULL, NULL, 2),
(90134, 90122, 90005, '花市', '周末花市的一角，色彩挤在一起', 0),
(90135, 90122, 90002, '橱窗', '老城区的杂货铺橱窗', 1),
(90136, 90122, 90007, '斑马线', '雨后的斑马线，倒影里也有一个世界', 2),
(90137, 90122, 90001, '傍晚', '下班高峰前的最后一刻安静', 3),
(90138, 90123, 90001, '霓虹', '招牌亮起来的瞬间', 0),
(90139, 90123, 90007, '车流', '长曝光下的车灯轨迹', 1),
(90140, 90123, 90002, '便利店', '凌晨一点的便利店，城市里的灯塔', 2),
(90141, 90124, 90004, '陶罐', '窗边的旧陶罐与干花', 0),
(90142, 90124, 90005, '木桌', '木桌上的茶具，阴天散射光', 1);

-- ---------------------------------------------------------------------
-- 6. video / video_chapter：4 个视频
--    有章节/无章节（空态）/无封面（NULL）/同视频文件两条记录复用
--    duration 与 storage_file.duration 一致（mvhd 实测）
-- ---------------------------------------------------------------------
INSERT INTO video (id, title, cover_file_id, intro, file_id, duration, is_recommend) VALUES
(90151, '河流之上：纪录片的诞生', 90006, '七个月跟拍一条河的四季：从勘景到成片，纪录片的诞生过程。', 90008, 136, 1),
(90152, '山野徒步随拍', 90003, '三天两夜的秦岭徒步，手持拍摄的随行记录。', 90009, 156, 0),
(90153, '巷口的小店', NULL, '一家开了三十年的早餐店，凌晨四点开始的一天。', 90010, 85, 0),
(90154, '视频功能调试片段', NULL, '用于验证签名播放、拖动与清晰度切换的复用片段。', 90008, 136, 0);

INSERT INTO video_chapter (id, video_id, title, time_offset, sort) VALUES
(90161, 90151, '开场', 0, 0),
(90162, 90151, '勘景：第一场雪', 32, 1),
(90163, 90151, '拍摄：河面解冻', 71, 2),
(90164, 90151, '剪辑室手记', 103, 3),
(90165, 90151, '成片首映', 128, 4),
(90166, 90152, '出发', 0, 0),
(90167, 90152, '登顶', 88, 1),
(90168, 90154, '播放起点', 0, 0);

-- ---------------------------------------------------------------------
-- 7. book / book_chapter / book_ai_task：3 本书
--    本人作品/他人出版/草稿本（零章节边界）；章-节两级结构；
--    章节内容为分段排版 HTML（class 钩子 + 段首空两格 + 段间半行距，§7.2）
-- ---------------------------------------------------------------------
INSERT INTO book (id, title, author, cover_file_id, intro, ownership_type,
                  source_file_id, total_chapters, is_recommend) VALUES
(90191, '以太漂流志', 'dkb', 90006, '一艘小船与一片没有尽头的大海。关于出发、迷路与找到灯塔的三章故事。', 1, NULL, 3, 1),
(90192, '山野拾光录', '佚名', 90003, '一位山居者的四季笔记：花事、山色与炉火边的闲谈。', 2, NULL, 1, 0),
(90193, '草稿本', 'dkb', NULL, '未完成的练习作，章节待 AI 分章确认。', 1, NULL, 0, 0);

INSERT INTO book_chapter (id, book_id, title, level, parent_id, order_no, content, word_count) VALUES
(90195, 90191, '序章：起航', 1, NULL, 1,
 '<p class="indent">我在港口学会了三件事：看风向，系绳结，以及不要问海的名字。</p><p class="indent">出发那天没有送行的人。船是旧的，帆是新补的，指南针是祖父留下的。祖父说，海不会回答你的问题，它只会用浪花重复它们。</p><p class="indent">第一夜风平浪静。我躺在甲板上数星星，数到一半睡着了。梦里有人告诉我，灯塔在正北偏西，但别急着去——先学会和船相处。</p>',
 156),
(90196, 90191, '港口', 2, 90195, 2,
 '<p class="indent">港口在身后缩成一条线，最后连海鸥也不再跟着飞。我回头看了一眼，什么也没看见。</p><p class="indent">人在海上漂久了，会把岸当成一个不真实的词。只有食物变少的时候，岸才重新变得具体。</p>',
 89),
(90197, 90191, '风向', 2, 90195, 3,
 '<p class="indent">起风了。帆鼓起来的声音像有人在船头抖开一匹布。</p><p class="indent">风向是海的语言，只有听不懂的人才会觉得海沉默。</p>',
 53),
(90198, 90191, '第二章：雾海', 1, NULL, 4,
 '<p class="indent">第七天，船驶进了雾里。雾浓得像固体，船头推开它的声音，像撕开旧布。</p><p class="indent">指南针在雾里格外亮，那一点微光成了唯一的坐标。我想起祖父说过：雾里航行，信工具，别信眼睛。</p><p class="indent">雾散的那一刻毫无预兆。先是桅杆的影子，然后是海，然后是天。世界像被谁重新画了一遍。</p>',
 141),
(90199, 90191, '第三章：灯塔', 1, NULL, 5,
 '<p class="indent">第十五天夜里，我看见了灯塔。它每隔十秒亮一次，像海的心跳。</p><p class="indent">靠岸的时候天快亮了。灯塔看守人端来热茶，说我是今年第三个到访者。前两个都回去了，带着各自的答案。</p><p class="indent">我问他海的名字，他笑了笑，把问题原样还给我。那一刻我忽然明白：海没有名字，名字是出发的人给它起的。</p>',
 138),
(90200, 90192, '山中来信', 1, NULL, 1,
 '<p class="indent">山里的春天来得慢。城里的花开败了，这里的雪才刚开始化。</p><p class="indent">溪水涨起来的那天，我去林子里走了一趟。去年的落叶还铺在地上，新芽已经从底下钻出来，像一群绿色的问号。</p><p class="indent">傍晚生火做饭，烟从屋顶升上去，和山雾混在一起。有人问我山居寂不寂寞——寂寞是好的，人只有在安静的时候才听得见自己。</p>',
 134);

INSERT INTO book_ai_task (id, book_id, status, ai_result, fail_reason, confirm_time) VALUES
(90206, 90193, 1, NULL, NULL, NULL),
(90207, 90193, 2,
 '{"chapters":[{"title":"第1章 开场","level":1,"paragraphs":["段落一","段落二"]},{"title":"1.1 背景","level":2,"paragraphs":["段落三"]}],"source":"heuristic+llm"}',
 NULL, NULL),
(90208, 90192, 3,
 '{"chapters":[{"title":"山中来信","level":1,"paragraphs":["山里的春天来得慢。","溪水涨起来的那天。","傍晚生火做饭。"]}],"source":"llm"}',
 NULL, '2026-08-19 10:30:00'),
(90209, 90191, 4, NULL, 'AI 服务超时：Ollama 未响应，已按启发式结果重试仍失败', NULL);

-- ---------------------------------------------------------------------
-- 8. announcement：5 条（公告/动态/新闻三种类型，置顶与非置顶，带图与不带图）
-- ---------------------------------------------------------------------
INSERT INTO announcement (id, title, content, type, cover_file_id, is_top, publish_time) VALUES
(90211, 'Aether 小站正式上线', '<p>筹备百天，小站终于和大家见面了。</p><figure><img src="https://img95.699pic.com/photo/50059/8720.jpg_wh860.jpg" loading="lazy" alt="上线贺图" /><figcaption>新站上线，感谢每一个路过的你</figcaption></figure><p>目前已有<strong>文章、图集、视频</strong>三大模块，音乐、书籍与 AI 助手正在路上。欢迎常来坐坐。</p>', 1, NULL, 1, '2026-08-01 12:00:00'),
(90212, '关于全站内容保护的说明', '<p>本站全部图片、视频、音频仅供在线浏览与播放，<strong>不提供下载</strong>。</p><p>媒体文件采用签名 URL 访问，签名十分钟内有效；图片上传时自动抹除 EXIF 定位信息。请尊重创作，谢谢配合。</p>', 1, NULL, 0, '2026-08-03 10:00:00'),
(90213, '新增图集：秦岭秋色', '<p>新图集《秦岭秋色》已上线，收录红叶长廊、山谷晨雾与暮色远山共 3 张作品。</p>', 2, NULL, 0, '2026-08-18 10:30:00'),
(90214, '个人网站建设的那些事', '<p>从选型到上线，个人网站建设的完整记录。</p><figure><img src="https://img95.699pic.com/photo/50464/9776.jpg_wh860.jpg" loading="lazy" alt="建站配图" /><figcaption>建站笔记配图</figcaption></figure><p>详情见站内文章<a href="/aether/articles/90118">《以太小站建站全记录》</a>。</p>', 3, 90002, 0, '2026-08-20 09:00:00'),
(90215, '音乐模块开发中', '<p>音乐播放器与 AI 特效正在开发中，预计支持歌词滚动、节拍粒子与频段波形。敬请期待。</p>', 2, NULL, 0, '2026-07-25 16:20:00');

-- ---------------------------------------------------------------------
-- 9. subscriber / survey：订阅与问卷（同 IP 多邮箱、已退订、修改 2 次边界）
-- ---------------------------------------------------------------------
INSERT INTO subscriber (id, email, ip, status, subscribe_time, unsubscribe_time) VALUES
(90221, 'a@example.com', '203.0.113.1', 1, '2026-08-10 09:12:00', NULL),
(90222, 'b@example.com', '203.0.113.1', 1, '2026-08-11 14:35:00', NULL),
(90223, 'c@example.com', '198.51.100.7', 0, '2026-08-05 11:00:00', '2026-08-20 20:15:00'),
(90224, 'd@example.com', '192.0.2.55', 1, '2026-08-24 08:40:00', NULL);

-- interests 引用 survey_option 字典（子查询取 id，保证外键一致）
INSERT INTO survey (id, email, ip_hash, age_range, gender, occupation, interests,
                    update_count, submit_time) VALUES
(90231, 'a@example.com', 'a1b2c3d4e5f60718293a4b5c6d7e8f901a2b3c4d5e6f708192a3b4c5d6e7f8',
 '25-30 岁', '男', 'IT 互联网',
 JSON_ARRAY((SELECT id FROM survey_option WHERE field = 'interests' AND label = '编程'),
            (SELECT id FROM survey_option WHERE field = 'interests' AND label = '阅读'),
            (SELECT id FROM survey_option WHERE field = 'interests' AND label = '摄影')),
 0, '2026-08-10 09:20:00'),
(90232, 'b@example.com', 'f1e2d3c4b5a60718293f4e5d6c7b8a901f2e3d4c5b6a708192f3e4d5c6b7a8',
 '31-35 岁', '女', '教育',
 JSON_ARRAY((SELECT id FROM survey_option WHERE field = 'interests' AND label = '音乐'),
            (SELECT id FROM survey_option WHERE field = 'interests' AND label = '旅行')),
 2, '2026-08-11 14:50:00');

-- ---------------------------------------------------------------------
-- 10. visitor_log / visitor_daily_stat：访问日志与日统计
--     统计逐日逐省与日志条数严格吻合（visit_count=行数，unique_ip=去重 IP 数）；
--     国外 IP 仅入日志不入统计（ECharts 中国地图仅统计国内省份）
-- ---------------------------------------------------------------------
INSERT INTO visitor_log (id, ip, country, province, city, region, user_agent, device_type,
                         browser, os, visit_path, referer, visit_time) VALUES
-- 2026-08-27（国内 12 条 8 省 + 国外 2 条）
(90241, '210.74.129.10', '中国', '陕西', '西安', '中国·陕西·西安', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36', 'PC', 'Chrome', 'Windows', '/aether/', NULL, '2026-08-27 08:15:00'),
(90242, '210.74.129.10', '中国', '陕西', '西安', '中国·陕西·西安', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36', 'PC', 'Chrome', 'Windows', '/aether/articles/90110', 'https://www.heyqing.top/aether/', '2026-08-27 08:16:00'),
(90243, '210.74.129.22', '中国', '陕西', '西安', '中国·陕西·西安', 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36', 'Mobile', 'Chrome', 'Android', '/aether/articles/90118', NULL, '2026-08-27 09:40:00'),
(90244, '110.244.8.5', '中国', '北京', '北京', '中国·北京·北京', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1', 'Mobile', 'Safari', 'iOS', '/aether/albums/90121', NULL, '2026-08-27 10:02:00'),
(90245, '101.86.45.11', '中国', '上海', '上海', '中国·上海·上海', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36', 'PC', 'Chrome', 'Windows', '/aether/videos/90151', 'https://www.heyqing.top/aether/articles/90113', '2026-08-27 10:30:00'),
(90246, '101.86.45.88', '中国', '上海', '上海', '中国·上海·上海', 'Mozilla/5.0 (iPad; CPU OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1', 'Tablet', 'Safari', 'iPadOS', '/aether/', NULL, '2026-08-27 11:12:00'),
(90247, '113.90.26.31', '中国', '广东', '深圳', '中国·广东·深圳', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0', 'PC', 'Firefox', 'Windows', '/aether/articles/90114', NULL, '2026-08-27 13:05:00'),
(90248, '113.90.26.31', '中国', '广东', '深圳', '中国·广东·深圳', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0', 'PC', 'Firefox', 'Windows', '/aether/articles/90112', 'https://www.heyqing.top/aether/articles/90114', '2026-08-27 13:21:00'),
(90249, '115.192.33.7', '中国', '浙江', '杭州', '中国·浙江·杭州', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36 Edg/126.0', 'PC', 'Edge', 'Windows', '/aether/albums/90122', NULL, '2026-08-27 14:40:00'),
(90250, '118.112.77.19', '中国', '四川', '成都', '中国·四川·成都', 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36', 'Mobile', 'Chrome', 'Android', '/aether/videos/90152', NULL, '2026-08-27 15:55:00'),
(90251, '27.17.90.3', '中国', '湖北', '武汉', '中国·湖北·武汉', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36', 'PC', 'Chrome', 'Windows', '/aether/articles/90116', NULL, '2026-08-27 16:30:00'),
(90252, '121.229.66.2', '中国', '江苏', '南京', '中国·江苏·南京', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1', 'Mobile', 'Safari', 'iOS', '/aether/', NULL, '2026-08-27 17:45:00'),
(90253, '98.140.5.3', '美国', NULL, NULL, '美国', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36', 'PC', 'Chrome', 'Windows', '/aether/articles/90114', NULL, '2026-08-27 08:00:00'),
(90254, '133.11.2.9', '日本', NULL, NULL, '日本', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1', 'Mobile', 'Safari', 'iOS', '/aether/', NULL, '2026-08-27 08:30:00'),
-- 2026-08-26（5 条）
(90255, '210.74.129.33', '中国', '陕西', '西安', '中国·陕西·西安', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36', 'PC', 'Chrome', 'Windows', '/aether/', NULL, '2026-08-26 09:00:00'),
(90256, '210.74.129.77', '中国', '陕西', '西安', '中国·陕西·西安', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36 Edg/126.0', 'PC', 'Edge', 'Windows', '/aether/articles/90118', NULL, '2026-08-26 10:20:00'),
(90257, '110.244.8.66', '中国', '北京', '北京', '中国·北京·北京', 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36', 'Mobile', 'Chrome', 'Android', '/aether/articles/90110', NULL, '2026-08-26 11:00:00'),
(90258, '101.86.45.9', '中国', '上海', '上海', '中国·上海·上海', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36', 'PC', 'Chrome', 'Windows', '/aether/videos/90151', NULL, '2026-08-26 14:10:00'),
(90259, '113.90.26.90', '中国', '广东', '深圳', '中国·广东·深圳', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1', 'Mobile', 'Safari', 'iOS', '/aether/albums/90121', NULL, '2026-08-26 16:25:00'),
-- 2026-08-25（3 条）
(90260, '210.74.129.11', '中国', '陕西', '西安', '中国·陕西·西安', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36', 'PC', 'Chrome', 'Windows', '/aether/', NULL, '2026-08-25 09:10:00'),
(90261, '115.192.33.55', '中国', '浙江', '杭州', '中国·浙江·杭州', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36', 'PC', 'Chrome', 'Windows', '/aether/articles/90114', NULL, '2026-08-25 15:00:00'),
(90262, '118.112.77.44', '中国', '四川', '成都', '中国·四川·成都', 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36', 'Mobile', 'Chrome', 'Android', '/aether/albums/90122', NULL, '2026-08-25 19:30:00');

INSERT INTO visitor_daily_stat (id, stat_date, region_code, region_name, visit_count, unique_ip) VALUES
(90263, '2026-08-27', '610000', '陕西', 3, 2),
(90264, '2026-08-27', '110000', '北京', 1, 1),
(90265, '2026-08-27', '310000', '上海', 2, 2),
(90266, '2026-08-27', '440000', '广东', 2, 1),
(90267, '2026-08-27', '330000', '浙江', 1, 1),
(90268, '2026-08-27', '510000', '四川', 1, 1),
(90269, '2026-08-27', '420000', '湖北', 1, 1),
(90270, '2026-08-27', '320000', '江苏', 1, 1),
(90271, '2026-08-26', '610000', '陕西', 2, 2),
(90272, '2026-08-26', '110000', '北京', 1, 1),
(90273, '2026-08-26', '310000', '上海', 1, 1),
(90274, '2026-08-26', '440000', '广东', 1, 1),
(90275, '2026-08-25', '610000', '陕西', 1, 1),
(90276, '2026-08-25', '330000', '浙江', 1, 1),
(90277, '2026-08-25', '510000', '四川', 1, 1);

-- ---------------------------------------------------------------------
-- 11. operation_log：管理员操作日志（成功/失败/参数脱敏示例）
-- ---------------------------------------------------------------------
INSERT INTO operation_log (id, operator, module, action, method, path, params, result, ip, cost_ms, error_msg) VALUES
(90281, 'cryptex', 'article', '新增', 'POST', '/aether/api/admin/articles', '{"title":"以太小站建站全记录","password":"***","categoryIds":[1]}', 1, '127.0.0.1', 42, NULL),
(90282, 'cryptex', 'article', '修改', 'PUT', '/aether/api/admin/articles/90118', '{"title":"以太小站建站全记录（修订）","isPublished":1}', 1, '127.0.0.1', 35, NULL),
(90283, 'cryptex', 'album', '新增', 'POST', '/aether/api/admin/albums', '{"title":"秦岭秋色","intro":"十月中旬的秦岭腹地"}', 1, '127.0.0.1', 128, NULL),
(90284, 'cryptex', 'video', '删除', 'DELETE', '/aether/api/admin/videos/99999', '{}', 0, '127.0.0.1', 12, '视频不存在或已删除'),
(90285, 'cryptex', 'storage', '上传', 'POST', '/aether/api/admin/storage/upload', '{"filename":"video-cri-01.mp4","size":71567983,"chunkTotal":9}', 1, '127.0.0.1', 28451, NULL);

-- ---------------------------------------------------------------------
-- 12. ai_chat_session / ai_chat_message / ai_generation_task：AI 链路样例
-- ---------------------------------------------------------------------
INSERT INTO ai_chat_session (id, session_id, visitor_ip) VALUES
(90286, '5f1e6c2a-9b3d-4c8e-8f2a-1d7b4a3c9e01', '210.74.129.10'),
(90287, '7a2d4e8b-1c6f-4a9d-b3e5-8f0c2d6a4b02', '98.140.5.3');

INSERT INTO ai_chat_message (id, session_id, role, content, model, token_count) VALUES
(90288, '5f1e6c2a-9b3d-4c8e-8f2a-1d7b4a3c9e01', 'user', '介绍一下这个网站', NULL, NULL),
(90289, '5f1e6c2a-9b3d-4c8e-8f2a-1d7b4a3c9e01', 'assistant', '这里是 Aether 小站，一个个人博客与多媒体内容站：可以读文章、看图集、看视频，后续还会有音乐与书籍模块。', 'deepseek-chat', 87),
(90290, '7a2d4e8b-1c6f-4a9d-b3e5-8f0c2d6a4b02', 'user', 'What is the meaning of aether?', NULL, NULL),
(90291, '7a2d4e8b-1c6f-4a9d-b3e5-8f0c2d6a4b02', 'assistant', 'Aether was the classical medium of light. Here it is the name of a personal website, a medium for content itself.', 'deepseek-chat', 64);

INSERT INTO ai_generation_task (id, scene, biz_type, biz_id, provider, status, result, fail_reason, cost_ms) VALUES
(90292, 'effect', 'music', 90181, 'cloud', 2,
 '{"version":1,"layers":[{"type":"particles","bind":"amplitude","count":120}]}',
 NULL, 3821),
(90293, 'classify', 'article', 90110, 'local', 3,
 NULL, 'Ollama 未启动：connection refused', 152);

-- ---------------------------------------------------------------------
-- 12. music_album / music：2 个合集 + 6 首曲目
--     自定义合集/固定合集（认证）/独立单曲/LRC 与纯文本歌词/
--     EffectConfig（§7.3 Schema v1）/lyric_offset 正负值/音频文件跨曲目复用
-- ---------------------------------------------------------------------
INSERT INTO music_album (id, title, cover_file_id, intro, type, certification, is_recommend) VALUES
(90171, '黄昏电台', 90001, '适合傍晚循环的一组曲子：温柔的节拍、模糊的边界与一点点旧日气息。', 1, NULL, 1),
(90172, '试听精选 · 认证专辑', 90002, '固定合集示例：经过"认证"的精选专辑，用于演示认证信息展示与播放页布局。', 2, '发行方：SoundHelix 测试音源 · 认证编号 SH-TEST-2026', 1);

INSERT INTO music (id, title, artist, album_id, cover_file_id, file_id, lyric_text, lyric_offset,
                   duration, effect_config, effect_source, is_recommend) VALUES
(90181, '星轨', 'SoundHelix', 90171, NULL, 90011,
'[ti:星轨]\n[ar:SoundHelix]\n[al:黄昏电台]\n[00:00.00]星轨\n[00:12.50]夜色落下来 星子升起\n[00:20.00]沿着轨道 一圈一圈\n[00:27.50]没人看见的地方 也有轨迹\n[01:05.00]我把名字写在风里\n[01:12.50]风把它带向更远的星系\n[01:20.00]多年以后 那颗星还在不在\n[01:27.50]它记得的 又是谁的四季\n[02:10.00]夜色落下来 星子升起\n[02:17.50]沿着轨道 一圈一圈\n[02:25.00]愿你也找到 你的轨迹',
 0, 373,
 '{"version":1,"palette":["#E8C94A","#1A1B1F","#8B6F47"],"layers":[{"type":"particles","bind":"amplitude","count":120,"sizeRange":[1,6],"speed":1.2,"opacity":0.7},{"type":"wave","bind":"freqBand","band":[0,0.3],"amplitude":40,"color":"#E8C94A"},{"type":"ring","bind":"beat","amplitude":30,"color":"#8B6F47"}],"background":{"type":"gradient","from":"#0F1013","to":"#1A1B1F"},"transition":{"duration":800,"easing":"easeOutCubic"},"sandboxCode":null}',
 1, 1),
(90182, '雾中列车', 'SoundHelix', 90171, NULL, 90012,
'汽笛声穿过雾，站台空无一人。\n列车载着旧梦，开往没有名字的城。\n车窗外的风景像没洗好的底片，一帧一帧退去。\n旅人闭上眼睛，把终点交给轨道。',
 0, 344, NULL, 1, 0),
(90183, '无人灯塔', 'SoundHelix', 90171, NULL, 90013, NULL, 0, 325, NULL, 1, 0),
(90184, '旧时光的星轨', 'SoundHelix', 90172, NULL, 90011, NULL, -500, 373, NULL, 2, 0),
(90185, '雾中列车 · 现场版', 'SoundHelix', NULL, NULL, 90012,
'[ti:雾中列车 · 现场版]\n[ar:SoundHelix]\n[00:00.00]\n[00:08.00]汽笛声穿过雾\n[00:16.00]站台空无一人\n[00:24.00]列车载着旧梦\n[00:32.00]开往没有名字的城',
 800, 344,
 '{"version":1,"palette":["#5B8DEF","#0F1013"],"layers":[{"type":"flowline","bind":"amplitude","count":60,"speed":0.8,"opacity":0.5}],"background":{"type":"gradient","from":"#0F1013","to":"#1A1B1F"},"transition":{"duration":500,"easing":"easeInOutQuad"},"sandboxCode":null}',
 2, 0),
(90186, '海风练习曲', 'SoundHelix', NULL, NULL, 90013,
'海浪是节拍器，一遍一遍，数着时间。\n风把灯塔的光揉碎，洒在夜的边缘。',
 0, 325, NULL, 1, 0);

-- ---------------------------------------------------------------------
-- 13. storage_ref：全部文件引用登记（与上传链路产物同构，§8.3）
--     同一文件在业务内多处引用（封面又作图集图片）ref_count 递增为 2；
--     同一文件跨业务/跨记录引用为独立多行。覆盖全部 storage_file 引用
-- ---------------------------------------------------------------------
INSERT INTO storage_ref (id, file_id, biz_type, biz_id, ref_count) VALUES
-- 文章封面
(90301, 90003, 'article', 90110, 1),
(90302, 90002, 'article', 90112, 1),
(90303, 90007, 'article', 90113, 1),
(90304, 90001, 'article', 90118, 1),
(90305, 90006, 'article', 90119, 1),
-- 图集（封面与图片同文件 → ref_count=2）
(90306, 90006, 'album', 90121, 2),
(90307, 90003, 'album', 90121, 1),
(90308, 90004, 'album', 90121, 1),
(90309, 90005, 'album', 90122, 2),
(90310, 90002, 'album', 90122, 1),
(90311, 90007, 'album', 90122, 1),
(90312, 90001, 'album', 90122, 1),
(90313, 90001, 'album', 90123, 2),
(90314, 90007, 'album', 90123, 1),
(90315, 90002, 'album', 90123, 1),
(90316, 90004, 'album', 90124, 2),
(90317, 90005, 'album', 90124, 1),
-- 视频（文件与封面；90008 被两条视频记录复用）
(90318, 90008, 'video', 90151, 1),
(90319, 90006, 'video', 90151, 1),
(90320, 90009, 'video', 90152, 1),
(90321, 90003, 'video', 90152, 1),
(90322, 90010, 'video', 90153, 1),
(90323, 90008, 'video', 90154, 1),
-- 书籍封面
(90324, 90006, 'book', 90191, 1),
(90325, 90003, 'book', 90192, 1),
-- 公告封面
(90326, 90002, 'announcement', 90214, 1),
-- 音乐（合集封面 + 曲目音频，音频复用：90011/90012/90013 各被两首曲目引用）
(90327, 90001, 'music', 90171, 1),
(90328, 90002, 'music', 90172, 1),
(90329, 90011, 'music', 90181, 1),
(90330, 90011, 'music', 90184, 1),
(90331, 90012, 'music', 90182, 1),
(90332, 90012, 'music', 90185, 1),
(90333, 90013, 'music', 90183, 1),
(90334, 90013, 'music', 90186, 1);

-- ---------------------------------------------------------------------
-- 14. biz_category_rel / biz_tag_rel：分类标签绑定
--     分类/标签 ID 均以 (biz_type, slug) 子查询定位，与 DataSeeder 字典严格一致
-- ---------------------------------------------------------------------
INSERT INTO biz_category_rel (biz_type, biz_id, category_id) VALUES
('article', 90110, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'life')),
('article', 90111, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'essay')),
('article', 90112, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'photo')),
('article', 90113, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'movie')),
('article', 90114, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'tech')),
('article', 90115, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'essay')),
('article', 90116, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'reading')),
('article', 90117, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'movie')),
('article', 90118, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'life')),
('article', 90119, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'photo')),
('article', 90120, (SELECT id FROM category WHERE biz_type = 'article' AND slug = 'essay')),
('album', 90121, (SELECT id FROM category WHERE biz_type = 'album' AND slug = 'landscape')),
('album', 90122, (SELECT id FROM category WHERE biz_type = 'album' AND slug = 'life')),
('album', 90123, (SELECT id FROM category WHERE biz_type = 'album' AND slug = 'street')),
('album', 90124, (SELECT id FROM category WHERE biz_type = 'album' AND slug = 'portrait')),
('album', 90125, (SELECT id FROM category WHERE biz_type = 'album' AND slug = 'life')),
('video', 90151, (SELECT id FROM category WHERE biz_type = 'video' AND slug = 'record')),
('video', 90152, (SELECT id FROM category WHERE biz_type = 'video' AND slug = 'vlog')),
('video', 90153, (SELECT id FROM category WHERE biz_type = 'video' AND slug = 'record')),
('video', 90154, (SELECT id FROM category WHERE biz_type = 'video' AND slug = 'record')),
('book', 90191, (SELECT id FROM category WHERE biz_type = 'book' AND slug = 'literature')),
('book', 90192, (SELECT id FROM category WHERE biz_type = 'book' AND slug = 'literature')),
('book', 90193, (SELECT id FROM category WHERE biz_type = 'book' AND slug = 'literature')),
('music', 90181, (SELECT id FROM category WHERE biz_type = 'music' AND slug = 'pop')),
('music', 90182, (SELECT id FROM category WHERE biz_type = 'music' AND slug = 'folk')),
('music', 90183, (SELECT id FROM category WHERE biz_type = 'music' AND slug = 'electronic')),
('music', 90184, (SELECT id FROM category WHERE biz_type = 'music' AND slug = 'pop')),
('music', 90185, (SELECT id FROM category WHERE biz_type = 'music' AND slug = 'folk')),
('music', 90186, (SELECT id FROM category WHERE biz_type = 'music' AND slug = 'electronic')),
('announcement', 90211, (SELECT id FROM category WHERE biz_type = 'announcement' AND slug = 'notice')),
('announcement', 90212, (SELECT id FROM category WHERE biz_type = 'announcement' AND slug = 'notice')),
('announcement', 90213, (SELECT id FROM category WHERE biz_type = 'announcement' AND slug = 'dynamic')),
('announcement', 90214, (SELECT id FROM category WHERE biz_type = 'announcement' AND slug = 'news')),
('announcement', 90215, (SELECT id FROM category WHERE biz_type = 'announcement' AND slug = 'dynamic'));

INSERT INTO biz_tag_rel (biz_type, biz_id, tag_id) VALUES
('article', 90110, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '旅行')),
('article', 90110, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '生活随想')),
('article', 90111, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '生活随想')),
('article', 90112, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '旅行')),
('article', 90113, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '影评')),
('article', 90114, (SELECT id FROM tag WHERE biz_type = 'article' AND name = 'Java')),
('article', 90114, (SELECT id FROM tag WHERE biz_type = 'article' AND name = 'Spring')),
('article', 90114, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '前端')),
('article', 90116, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '书评')),
('article', 90117, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '影评')),
('article', 90118, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '生活随想')),
('article', 90119, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '旅行')),
('article', 90120, (SELECT id FROM tag WHERE biz_type = 'article' AND name = '生活随想')),
('album', 90121, (SELECT id FROM tag WHERE biz_type = 'album' AND name = '自然')),
('album', 90121, (SELECT id FROM tag WHERE biz_type = 'album' AND name = '胶片')),
('album', 90122, (SELECT id FROM tag WHERE biz_type = 'album' AND name = '街景')),
('album', 90123, (SELECT id FROM tag WHERE biz_type = 'album' AND name = '街景')),
('album', 90123, (SELECT id FROM tag WHERE biz_type = 'album' AND name = '黑白')),
('album', 90124, (SELECT id FROM tag WHERE biz_type = 'album' AND name = '胶片')),
('video', 90151, (SELECT id FROM tag WHERE biz_type = 'video' AND name = '剪辑')),
('video', 90152, (SELECT id FROM tag WHERE biz_type = 'video' AND name = '旅拍')),
('video', 90153, (SELECT id FROM tag WHERE biz_type = 'video' AND name = '日常')),
('video', 90154, (SELECT id FROM tag WHERE biz_type = 'video' AND name = '剪辑')),
('book', 90191, (SELECT id FROM tag WHERE biz_type = 'book' AND name = '长篇')),
('book', 90192, (SELECT id FROM tag WHERE biz_type = 'book' AND name = '短篇')),
('book', 90193, (SELECT id FROM tag WHERE biz_type = 'book' AND name = '连载')),
('music', 90181, (SELECT id FROM tag WHERE biz_type = 'music' AND name = '治愈')),
('music', 90182, (SELECT id FROM tag WHERE biz_type = 'music' AND name = '纯音乐')),
('music', 90183, (SELECT id FROM tag WHERE biz_type = 'music' AND name = '纯音乐')),
('music', 90184, (SELECT id FROM tag WHERE biz_type = 'music' AND name = '经典老歌')),
('music', 90186, (SELECT id FROM tag WHERE biz_type = 'music' AND name = '治愈'));
