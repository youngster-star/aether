-- =====================================================================
-- Aether 生产库建库脚本（MySQL 8.4，BackEnd-Plan §6.2）
-- 生产环境 ddl-auto=none，表结构以本脚本为准；dev/test 用 H2 create-drop 自动建表。
-- 所有字段 COMMENT 完整中文注释；变更表结构必须先同步 BackEnd-Plan §6.2 再改本文件。
-- =====================================================================

-- ========== 用户与游客 ==========

-- 管理员表（单账户）
CREATE TABLE `sys_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(50)  NOT NULL                COMMENT '登录名（固定 cryptex）',
  `password_hash`   VARCHAR(255) NOT NULL                COMMENT '密码哈希（Argon2，密码即 cryptex，dev 默认 heyqing2aether）',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 正常 0 禁用',
  `last_login_time` DATETIME     NULL                    COMMENT '最后登录时间',
  `last_login_ip`   VARCHAR(45)  NULL                    COMMENT '最后登录 IP',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员表（单账户，用户名 cryptex）';

-- 游客访问日志表
CREATE TABLE `visitor_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ip`          VARCHAR(45)  NOT NULL                COMMENT '访问 IP（IPv4/IPv6）',
  `country`     VARCHAR(50)  NULL                    COMMENT '国家（ip2region 解析）',
  `province`    VARCHAR(50)  NULL                    COMMENT '省份（ip2region 解析）',
  `city`        VARCHAR(50)  NULL                    COMMENT '城市（ip2region 解析）',
  `region`      VARCHAR(100) NULL                    COMMENT '完整地域串，如：中国·陕西·西安',
  `user_agent`  VARCHAR(500) NULL                    COMMENT '原始 User-Agent',
  `device_type` VARCHAR(20)  NULL                    COMMENT '设备类型：PC/Mobile/Tablet',
  `browser`     VARCHAR(50)  NULL                    COMMENT '浏览器名称',
  `os`          VARCHAR(50)  NULL                    COMMENT '操作系统',
  `visit_path`  VARCHAR(255) NULL                    COMMENT '访问路径',
  `referer`     VARCHAR(500) NULL                    COMMENT '来源页面',
  `visit_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_ip_time` (`ip`, `visit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='游客访问日志表（IP 定位分析）';

-- 游客日统计表（地域聚合，ECharts 地图数据源）
CREATE TABLE `visitor_daily_stat` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date`   DATE         NOT NULL                COMMENT '统计日期',
  `region_code` VARCHAR(20)  NOT NULL                COMMENT '行政区划代码（ECharts 地图匹配用）',
  `region_name` VARCHAR(100) NULL                    COMMENT '地区名称',
  `visit_count` INT          NOT NULL DEFAULT 0      COMMENT '访问次数',
  `unique_ip`   INT          NOT NULL DEFAULT 0      COMMENT '独立 IP 数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_region` (`stat_date`, `region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='游客日统计表（按地域聚合）';

-- ========== 文章 ==========

-- 文章表
CREATE TABLE `article` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`            VARCHAR(200) NOT NULL                COMMENT '文章标题',
  `cover_file_id`    BIGINT       NULL                    COMMENT '封面文件 ID（storage_file.id）',
  `summary`          VARCHAR(500) NULL                    COMMENT '文章简介',
  `content_html`     MEDIUMTEXT   NOT NULL                COMMENT 'HTML 内容（sanitize 后）',
  `content_md`       MEDIUMTEXT   NOT NULL                COMMENT 'Markdown 源内容（编辑回显用）',
  `word_count`       INT          NOT NULL DEFAULT 0      COMMENT '字数',
  `reading_count`    INT          NOT NULL DEFAULT 0      COMMENT '阅读次数（IP 24 小时去重）',
  `is_hot`           TINYINT      NOT NULL DEFAULT 0      COMMENT '是否热门：1 是 0 否',
  `hot_order`        INT          NOT NULL DEFAULT 0      COMMENT '热度排序（越大越靠前）',
  `article_style_id` BIGINT       NULL                    COMMENT '文章样式 ID（NULL=默认样式）',
  `is_published`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否发布：1 已发布 0 草稿',
  `publish_time`     DATETIME     NULL                    COMMENT '发布时间',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pub_time` (`is_published`, `publish_time`),
  KEY `idx_hot` (`hot_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章表（博客主体，支持独立样式与自定义热度）';

-- 文章独立样式表
CREATE TABLE `article_style` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50) NOT NULL                COMMENT '样式名称',
  `style_json`  JSON        NOT NULL                COMMENT '样式配置（字体/字号/行高/段间距/首行缩进/主题色等，见 §6.3）',
  `is_default`  TINYINT     NOT NULL DEFAULT 0      COMMENT '是否默认样式：1 是 0 否',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章独立样式表（可被多篇文章复用）';

-- 分类表（全站共用）
CREATE TABLE `category` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50) NOT NULL                COMMENT '分类名称',
  `slug`        VARCHAR(80) NOT NULL                COMMENT '唯一标识（URL 用）',
  `biz_type`    VARCHAR(20) NOT NULL                COMMENT '业务域：article/book/music/album/video/announcement',
  `sort`        INT         NOT NULL DEFAULT 0      COMMENT '排序号',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_slug` (`biz_type`, `slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类表（全站共用，biz_type 区分业务域，seed 生成全量初始数据）';

-- 标签表（全站共用）
CREATE TABLE `tag` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50) NOT NULL                COMMENT '标签名称',
  `slug`        VARCHAR(80) NOT NULL                COMMENT '唯一标识',
  `biz_type`    VARCHAR(20) NOT NULL                COMMENT '业务域：article/book/music/album/video/announcement',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_slug` (`biz_type`, `slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表（全站共用，seed 生成全量初始数据）';

-- 业务-分类关联表
CREATE TABLE `biz_category_rel` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type`    VARCHAR(20) NOT NULL                COMMENT '业务域（与 category.biz_type 对应）',
  `biz_id`      BIGINT      NOT NULL                COMMENT '业务记录 ID',
  `category_id` BIGINT      NOT NULL                COMMENT '分类 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_cat` (`biz_type`, `biz_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务-分类关联表（多对多）';

-- 业务-标签关联表
CREATE TABLE `biz_tag_rel` (
  `id`       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` VARCHAR(20) NOT NULL                COMMENT '业务域',
  `biz_id`   BIGINT      NOT NULL                COMMENT '业务记录 ID',
  `tag_id`   BIGINT      NOT NULL                COMMENT '标签 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_tag` (`biz_type`, `biz_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务-标签关联表（多对多）';

-- ========== 图集与视频 ==========

-- 图集表
CREATE TABLE `album` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(200) NOT NULL                COMMENT '图集标题',
  `cover_file_id` BIGINT       NULL                    COMMENT '封面文件 ID',
  `intro`         VARCHAR(500) NULL                    COMMENT '图集介绍',
  `is_recommend`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否推荐：1 是 0 否',
  `sort`          INT          NOT NULL DEFAULT 0      COMMENT '推荐排序号',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图集表';

-- 图集图片表
CREATE TABLE `album_image` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `album_id`    BIGINT       NOT NULL                COMMENT '所属图集 ID',
  `file_id`     BIGINT       NOT NULL                COMMENT '图片文件 ID（宽高大小等参数存 storage_file）',
  `title`       VARCHAR(200) NULL                    COMMENT '图片标题（可选）',
  `intro`       VARCHAR(500) NULL                    COMMENT '图片介绍（可选）',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序号',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_album` (`album_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图集图片表';

-- 视频表
CREATE TABLE `video` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(200) NOT NULL                COMMENT '视频标题',
  `cover_file_id` BIGINT       NULL                    COMMENT '封面文件 ID',
  `intro`         VARCHAR(500) NULL                    COMMENT '视频介绍',
  `file_id`       BIGINT       NOT NULL                COMMENT '视频文件 ID',
  `duration`      INT          NOT NULL DEFAULT 0      COMMENT '时长（秒，上传合并后自动探测）',
  `is_recommend`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否推荐：1 是 0 否',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频表';

-- 视频关键时间节点表
CREATE TABLE `video_chapter` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `video_id`    BIGINT       NOT NULL                COMMENT '所属视频 ID',
  `title`       VARCHAR(200) NOT NULL                COMMENT '节点标题',
  `time_offset` INT          NOT NULL                COMMENT '时间偏移（秒）',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序号',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_video` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频关键时间节点表';

-- ========== 音乐 ==========

-- 音乐合集表
CREATE TABLE `music_album` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(200) NOT NULL                COMMENT '合集标题',
  `cover_file_id` BIGINT       NULL                    COMMENT '封面文件 ID',
  `intro`         VARCHAR(500) NULL                    COMMENT '合集介绍',
  `type`          TINYINT      NOT NULL                COMMENT '类型：1 自定义合集 2 固定合集（专辑）',
  `certification` VARCHAR(200) NULL                    COMMENT '认证信息（固定合集必填，如发行方/认证编号）',
  `is_recommend`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否推荐：1 是 0 否',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='音乐合集表（自定义合集与固定合集）';

-- 音乐表
CREATE TABLE `music` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(200) NOT NULL                COMMENT '歌曲名称',
  `artist`        VARCHAR(100) NULL                    COMMENT '歌手',
  `album_id`      BIGINT       NULL                    COMMENT '所属合集 ID（可独立单曲）',
  `cover_file_id` BIGINT       NULL                    COMMENT '封面文件 ID',
  `file_id`       BIGINT       NOT NULL                COMMENT '音频文件 ID',
  `lyric_text`    TEXT         NULL                    COMMENT '歌词文本（LRC 带时间轴 / 纯文本）',
  `lyric_offset`  INT          NOT NULL DEFAULT 0      COMMENT '歌词全局偏移（毫秒，正负可调，用于解决歌词不同步）',
  `duration`      INT          NOT NULL DEFAULT 0      COMMENT '时长（秒，上传后自动检测）',
  `effect_config` JSON         NULL                    COMMENT 'AI 特效配置（EffectConfig，见 §7.3）',
  `effect_source` TINYINT      NOT NULL DEFAULT 1      COMMENT '特效来源：1 AI 生成 2 手工调整',
  `is_recommend`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否推荐：1 是 0 否',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_album` (`album_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='音乐表（含 AI 特效配置）';

-- ========== 书籍 ==========

-- 书籍表
CREATE TABLE `book` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`          VARCHAR(200)  NOT NULL                COMMENT '书名',
  `author`         VARCHAR(100)  NULL                    COMMENT '作者',
  `cover_file_id`  BIGINT        NULL                    COMMENT '封面文件 ID',
  `intro`          VARCHAR(1000) NULL                    COMMENT '简介',
  `ownership_type` TINYINT       NOT NULL                COMMENT '版权归属：1 本人 2 他人出版',
  `source_file_id` BIGINT        NULL                    COMMENT '源文件 ID（txt）',
  `total_chapters` INT           NOT NULL DEFAULT 0      COMMENT '总章节数',
  `is_recommend`   TINYINT       NOT NULL DEFAULT 0      COMMENT '是否推荐：1 是 0 否',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍表（含出版图书与本人作品）';

-- 书籍章节表
CREATE TABLE `book_chapter` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `book_id`    BIGINT       NOT NULL                COMMENT '所属书籍 ID',
  `title`      VARCHAR(200) NOT NULL                COMMENT '章节标题',
  `level`      TINYINT      NOT NULL                COMMENT '层级：1 章 2 节',
  `parent_id`  BIGINT       NULL                    COMMENT '父章节 ID（节挂章）',
  `order_no`   INT          NOT NULL                COMMENT '排序号（全书顺序）',
  `content`    MEDIUMTEXT   NOT NULL                COMMENT '章节内容（分段排版后的 HTML：段首空两格、段间半行距）',
  `word_count` INT          NOT NULL DEFAULT 0      COMMENT '字数',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_order` (`book_id`, `order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍章节表（章-节两级结构）';

-- 书籍分章任务表
CREATE TABLE `book_ai_task` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `book_id`      BIGINT       NOT NULL                COMMENT '所属书籍 ID',
  `status`       TINYINT      NOT NULL                COMMENT '状态：1 解析中 2 待确认 3 已完成 4 失败',
  `ai_result`    JSON         NULL                    COMMENT 'AI 分章建议（章节标题+段落+偏移）',
  `fail_reason`  VARCHAR(500) NULL                    COMMENT '失败原因',
  `confirm_time` DATETIME     NULL                    COMMENT '用户确认时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_book` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='书籍 AI 分章任务表（状态机）';

-- ========== 存储 ==========

-- 文件元数据表（本地与 OSS 统一记录）
CREATE TABLE `storage_file` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `original_name` VARCHAR(255)  NOT NULL                COMMENT '原始文件名',
  `storage_type`  TINYINT       NOT NULL                COMMENT '存储类型：1 本地 2 OSS',
  `object_key`    VARCHAR(500)  NOT NULL                COMMENT '存储对象键（本地=相对路径，OSS=objectKey）',
  `size`          BIGINT        NOT NULL                COMMENT '文件大小（字节）',
  `file_md5`      CHAR(64)      NOT NULL                COMMENT '文件 SHA-256 哈希（秒传依据）',
  `mime_type`     VARCHAR(100)  NULL                    COMMENT 'MIME 类型',
  `ext`           VARCHAR(20)   NULL                    COMMENT '扩展名',
  `width`         INT           NULL                    COMMENT '图片宽度（像素）',
  `height`        INT           NULL                    COMMENT '图片高度（像素）',
  `duration`      INT           NULL                    COMMENT '音视频时长（秒）',
  `meta`          JSON          NULL                    COMMENT '其他元数据（编码/帧率/色域等）',
  `status`        TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1 正常 0 已删除（延迟清理）',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_md5` (`file_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件元数据表（本地与 OSS 统一记录）';

-- 分片记录表
CREATE TABLE `storage_chunk` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `upload_id`    VARCHAR(64) NOT NULL                COMMENT '上传会话 ID（UUID）',
  `file_md5`     CHAR(64)    NOT NULL                COMMENT '文件 SHA-256',
  `chunk_index`  INT         NOT NULL                COMMENT '分片序号（从 0）',
  `chunk_total`  INT         NOT NULL                COMMENT '总分片数',
  `size`         BIGINT      NOT NULL                COMMENT '分片大小（字节）',
  `storage_type` TINYINT     NOT NULL                COMMENT '存储类型（与上传时选择一致）',
  `status`       TINYINT     NOT NULL DEFAULT 1      COMMENT '状态：1 已上传 2 已合并 0 失效',
  `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_upload_chunk` (`upload_id`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分片上传进度表（断点续传依据）';

-- 业务-文件引用表（删除一致性核心）
CREATE TABLE `storage_ref` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_id`     BIGINT      NOT NULL                COMMENT '文件 ID',
  `biz_type`    VARCHAR(20) NOT NULL                COMMENT '业务域：article/album/video/music/book/announcement',
  `biz_id`      BIGINT      NOT NULL                COMMENT '业务记录 ID',
  `ref_count`   INT         NOT NULL DEFAULT 1      COMMENT '引用次数（同一文件多处引用时递增）',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_biz` (`file_id`, `biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务-文件引用表（删除一致性核心）';

-- ========== 公告与订阅 ==========

-- 公告表
CREATE TABLE `announcement` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`         VARCHAR(200) NOT NULL                COMMENT '公告标题',
  `content`       TEXT         NOT NULL                COMMENT '公告内容（文字+图片，sanitize 后 HTML）',
  `type`          TINYINT      NOT NULL                COMMENT '类型：1 公告 2 动态 3 新闻',
  `cover_file_id` BIGINT       NULL                    COMMENT '封面文件 ID（可选）',
  `is_top`        TINYINT      NOT NULL DEFAULT 0      COMMENT '是否置顶：1 是 0 否',
  `publish_time`  DATETIME     NULL                    COMMENT '发布时间',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_pub` (`type`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告表（公告/动态/新闻）';

-- 订阅者表
CREATE TABLE `subscriber` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `email`            VARCHAR(200) NOT NULL                COMMENT '订阅邮箱',
  `ip`               VARCHAR(45)  NOT NULL                COMMENT '订阅时 IP',
  `status`           TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 订阅中 0 已退订',
  `subscribe_time`   DATETIME     NULL                    COMMENT '订阅时间',
  `unsubscribe_time` DATETIME     NULL                    COMMENT '退订时间',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ip` (`ip`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订阅者表（一个 IP 可提交多个邮箱）';

-- 问卷表
CREATE TABLE `survey` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `email`        VARCHAR(200) NOT NULL                COMMENT '邮箱（与订阅对应）',
  `ip_hash`      CHAR(64)     NOT NULL                COMMENT 'IP 加盐哈希（隐私保护，唯一约束）',
  `age_range`    VARCHAR(20)  NULL                    COMMENT '年龄段',
  `gender`       VARCHAR(10)  NULL                    COMMENT '性别',
  `occupation`   VARCHAR(50)  NULL                    COMMENT '职业',
  `interests`    JSON         NULL                    COMMENT '兴趣爱好（survey_option id 列表）',
  `update_count` INT          NOT NULL DEFAULT 0      COMMENT '修改次数（允许最多 2 次）',
  `submit_time`  DATETIME     NULL                    COMMENT '提交时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ip_hash` (`ip_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问卷表（IP 限 1 份、最多修改 2 次）';

-- 问卷选项字典表
CREATE TABLE `survey_option` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `field`       VARCHAR(30) NOT NULL                COMMENT '字段名：age_range/gender/occupation/interests',
  `label`       VARCHAR(50) NOT NULL                COMMENT '选项文本',
  `sort`        INT         NOT NULL DEFAULT 0      COMMENT '排序号',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_field` (`field`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问卷选项字典表（seed 全量生成：年龄段 10 档/性别/职业/兴趣爱好）';

-- ========== 日志 ==========

-- 操作日志表
CREATE TABLE `operation_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator`    VARCHAR(50)  NULL                    COMMENT '操作人',
  `module`      VARCHAR(50)  NULL                    COMMENT '模块',
  `action`      VARCHAR(50)  NULL                    COMMENT '操作类型（新增/修改/删除/登录/上传/生成…）',
  `method`      VARCHAR(200) NULL                    COMMENT '请求方法',
  `path`        VARCHAR(255) NULL                    COMMENT '请求路径',
  `params`      JSON         NULL                    COMMENT '请求参数（脱敏后：password/token 等字段替换为 ***）',
  `result`      TINYINT      NULL                    COMMENT '结果：1 成功 0 失败',
  `ip`          VARCHAR(45)  NULL                    COMMENT '操作 IP',
  `cost_ms`     INT          NULL                    COMMENT '耗时（毫秒）',
  `error_msg`   VARCHAR(500) NULL                    COMMENT '失败原因',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_time` (`create_time`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员操作日志表';

-- ========== AI ==========

-- AI 配置表
CREATE TABLE `ai_config` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene`       VARCHAR(30)   NOT NULL                COMMENT '场景：chat/effect/agent/classify',
  `provider`    VARCHAR(20)   NOT NULL                COMMENT '厂商：local（Ollama）/cloud（DeepSeek）',
  `model_name`  VARCHAR(100)  NOT NULL                COMMENT '模型名称',
  `base_url`    VARCHAR(255)  NULL                    COMMENT '接口地址（local 默认 http://ollama:11434）',
  `api_key`     VARCHAR(500)  NULL                    COMMENT 'API 密钥（加密存储）',
  `temperature` DECIMAL(3,2)  NOT NULL DEFAULT 0.70   COMMENT '采样温度',
  `is_active`   TINYINT       NOT NULL DEFAULT 1      COMMENT '是否启用：1 是 0 否',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene` (`scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 配置表（管理端可切换厂商/模型）';

-- AI 对话会话表
CREATE TABLE `ai_chat_session` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`  VARCHAR(64) NOT NULL                COMMENT '会话 ID（前端生成 UUID）',
  `visitor_ip`  VARCHAR(45) NULL                    COMMENT '访问 IP',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 对话会话表';

-- AI 对话消息表
CREATE TABLE `ai_chat_message` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`  VARCHAR(64) NOT NULL                COMMENT '会话 ID',
  `role`        VARCHAR(20) NOT NULL                COMMENT '角色：user/assistant',
  `content`     TEXT        NOT NULL                COMMENT '消息内容',
  `model`       VARCHAR(100) NULL                   COMMENT '使用的模型名称',
  `token_count` INT         NULL                    COMMENT '消耗 token 数',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 对话消息表';

-- AI 生成任务表
CREATE TABLE `ai_generation_task` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene`       VARCHAR(30)  NOT NULL                COMMENT '场景：effect/classify/split',
  `biz_type`    VARCHAR(20)  NULL                    COMMENT '关联业务域',
  `biz_id`      BIGINT       NULL                    COMMENT '关联业务 ID',
  `provider`    VARCHAR(20)  NULL                    COMMENT '实际使用的厂商',
  `status`      TINYINT      NOT NULL                COMMENT '状态：1 处理中 2 成功 3 失败',
  `result`      JSON         NULL                    COMMENT '生成结果',
  `fail_reason` VARCHAR(500) NULL                    COMMENT '失败原因',
  `cost_ms`     INT          NULL                    COMMENT '耗时（毫秒）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 生成任务表（特效/分类推荐/分章统一任务记录）';
