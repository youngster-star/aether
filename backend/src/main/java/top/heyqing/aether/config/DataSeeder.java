package top.heyqing.aether.config;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import top.heyqing.aether.model.entity.AiConfig;
import top.heyqing.aether.model.entity.Album;
import top.heyqing.aether.model.entity.AlbumImage;
import top.heyqing.aether.model.entity.Article;
import top.heyqing.aether.model.entity.ArticleStyle;
import top.heyqing.aether.model.entity.BizCategoryRel;
import top.heyqing.aether.model.entity.BizTagRel;
import top.heyqing.aether.model.entity.Category;
import top.heyqing.aether.model.entity.StorageFile;
import top.heyqing.aether.model.entity.StorageRef;
import top.heyqing.aether.model.entity.SurveyOption;
import top.heyqing.aether.model.entity.SysUser;
import top.heyqing.aether.model.entity.Tag;
import top.heyqing.aether.model.entity.Video;
import top.heyqing.aether.model.entity.VideoChapter;
import top.heyqing.aether.repository.AiConfigRepository;
import top.heyqing.aether.repository.AlbumImageRepository;
import top.heyqing.aether.repository.AlbumRepository;
import top.heyqing.aether.repository.ArticleRepository;
import top.heyqing.aether.repository.ArticleStyleRepository;
import top.heyqing.aether.repository.BizCategoryRelRepository;
import top.heyqing.aether.repository.BizTagRelRepository;
import top.heyqing.aether.repository.CategoryRepository;
import top.heyqing.aether.repository.StorageFileRepository;
import top.heyqing.aether.repository.StorageRefRepository;
import top.heyqing.aether.repository.SurveyOptionRepository;
import top.heyqing.aether.repository.SysUserRepository;
import top.heyqing.aether.repository.TagRepository;
import top.heyqing.aether.repository.VideoChapterRepository;
import top.heyqing.aether.repository.VideoRepository;
import top.heyqing.aether.util.DigestUtil;

/**
 * seed 数据初始化（BackEnd-Plan §6.4）
 *
 * <p>仅 dev/test 环境执行（@Profile 控制，prod 不跑）：
 * 管理员账户、分类/标签字典、问卷选项、AI 默认配置。全部幂等（存在即跳过）。</p>
 */
@Component
@Profile({"dev", "test", "local"})
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** 管理员登录名（固定 cryptex，BackEnd-Plan §4.1） */
    private static final String ADMIN_USERNAME = "cryptex";

    /** dev 默认密码（生产必须 AETHER_CRYPTEX 环境变量覆盖） */
    private static final String DEV_DEFAULT_PASSWORD = "heyqing2aether";

    private final SysUserRepository sysUserRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final SurveyOptionRepository surveyOptionRepository;
    private final AiConfigRepository aiConfigRepository;
    private final ArticleRepository articleRepository;
    private final ArticleStyleRepository articleStyleRepository;
    private final BizCategoryRelRepository bizCategoryRelRepository;
    private final BizTagRelRepository bizTagRelRepository;
    private final AlbumRepository albumRepository;
    private final AlbumImageRepository albumImageRepository;
    private final VideoRepository videoRepository;
    private final VideoChapterRepository videoChapterRepository;
    private final StorageFileRepository storageFileRepository;
    private final StorageRefRepository storageRefRepository;
    private final StorageProperties storageProperties;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DataSeeder(SysUserRepository sysUserRepository, CategoryRepository categoryRepository,
                      TagRepository tagRepository, SurveyOptionRepository surveyOptionRepository,
                      AiConfigRepository aiConfigRepository, ArticleRepository articleRepository,
                      ArticleStyleRepository articleStyleRepository,
                      BizCategoryRelRepository bizCategoryRelRepository,
                      BizTagRelRepository bizTagRelRepository, AlbumRepository albumRepository,
                      AlbumImageRepository albumImageRepository, VideoRepository videoRepository,
                      VideoChapterRepository videoChapterRepository, StorageFileRepository storageFileRepository,
                      StorageRefRepository storageRefRepository, StorageProperties storageProperties,
                      PasswordEncoder passwordEncoder, Environment environment) {
        this.sysUserRepository = sysUserRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.surveyOptionRepository = surveyOptionRepository;
        this.aiConfigRepository = aiConfigRepository;
        this.articleRepository = articleRepository;
        this.articleStyleRepository = articleStyleRepository;
        this.bizCategoryRelRepository = bizCategoryRelRepository;
        this.bizTagRelRepository = bizTagRelRepository;
        this.albumRepository = albumRepository;
        this.albumImageRepository = albumImageRepository;
        this.videoRepository = videoRepository;
        this.videoChapterRepository = videoChapterRepository;
        this.storageFileRepository = storageFileRepository;
        this.storageRefRepository = storageRefRepository;
        this.storageProperties = storageProperties;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedSysUser();
        seedCategories();
        seedTags();
        seedSurveyOptions();
        seedAiConfigs();
        seedArticleStyles();
        seedArticles();
        seedAlbums();
        seedVideos();
        log.info("seed 数据初始化完成");
    }

    /**
     * 管理员账户：cryptex + Argon2(AETHER_CRYPTEX)
     */
    private void seedSysUser() {
        if (sysUserRepository.findByUsername(ADMIN_USERNAME).isPresent()) {
            return;
        }
        String password = environment.getProperty("AETHER_CRYPTEX", DEV_DEFAULT_PASSWORD);
        SysUser user = new SysUser();
        user.setUsername(ADMIN_USERNAME);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(1);
        sysUserRepository.save(user);
        log.info("seed: 管理员账户已创建（username=cryptex，密码来自 AETHER_CRYPTEX，dev 默认 heyqing2aether）");
    }

    /**
     * 分类字典：按业务域全量生成（尽可能全面，BackEnd-Plan §6.4）
     */
    private void seedCategories() {
        if (categoryRepository.countByBizType("article") > 0) {
            return;
        }
        // biz_type -> (名称 -> slug)
        Map<String, Map<String, String>> data = Map.ofEntries(
                Map.entry("article", orderedMap("技术", "tech", "生活", "life", "随笔", "essay",
                        "读书笔记", "reading", "摄影", "photo", "音乐", "music", "影视", "movie")),
                Map.entry("book", orderedMap("科幻", "scifi", "奇幻", "fantasy", "推理", "mystery",
                        "文学", "literature", "历史", "history", "传记", "biography")),
                Map.entry("music", orderedMap("流行", "pop", "摇滚", "rock", "民谣", "folk",
                        "古典", "classical", "电子", "electronic", "爵士", "jazz")),
                Map.entry("album", orderedMap("风景", "landscape", "人像", "portrait", "街拍", "street",
                        "旅行", "travel", "生活", "life")),
                Map.entry("video", orderedMap("记录", "record", "教程", "tutorial", "Vlog", "vlog",
                        "影视", "movie")));
        data.forEach((bizType, names) -> {
            int sort = 0;
            for (Map.Entry<String, String> entry : names.entrySet()) {
                Category category = new Category();
                category.setName(entry.getKey());
                category.setSlug(entry.getValue());
                category.setBizType(bizType);
                category.setSort(sort++);
                categoryRepository.save(category);
            }
        });
    }

    /**
     * 标签字典：按业务域全量生成
     */
    private void seedTags() {
        if (tagRepository.countByBizType("article") > 0) {
            return;
        }
        Map<String, List<String>> data = Map.of(
                "article", List.of("Java", "Spring", "前端", "生活随想", "旅行", "美食", "书评", "影评"),
                "book", List.of("长篇", "短篇", "连载", "经典", "新作"),
                "music", List.of("治愈", "国风", "摇滚", "纯音乐", "经典老歌"),
                "album", List.of("胶片", "黑白", "街景", "自然"),
                "video", List.of("旅拍", "日常", "开箱", "剪辑"));
        data.forEach((bizType, names) -> names.forEach(name -> {
            Tag tag = new Tag();
            tag.setName(name);
            tag.setSlug(name);
            tag.setBizType(bizType);
            tagRepository.save(tag);
        }));
    }

    /**
     * 问卷选项字典：年龄段 10 档 / 性别 / 职业 / 兴趣爱好
     */
    private void seedSurveyOptions() {
        if (surveyOptionRepository.countByField("age_range") > 0) {
            return;
        }
        saveOptions("age_range", List.of("18 岁以下", "18-24 岁", "25-30 岁", "31-35 岁", "36-40 岁",
                "41-45 岁", "46-50 岁", "51-60 岁", "60 岁以上", "不愿透露"));
        saveOptions("gender", List.of("男", "女", "其他", "保密"));
        saveOptions("occupation", List.of("学生", "IT 互联网", "教育", "医疗", "金融", "制造业",
                "自由职业", "公务员", "其他"));
        saveOptions("interests", List.of("音乐", "阅读", "摄影", "编程", "旅行", "运动", "电影", "游戏", "美食", "绘画"));
    }

    /**
     * AI 默认配置：四个场景各一行（BackEnd-Plan §6.4）
     */
    private void seedAiConfigs() {
        if (aiConfigRepository.findByScene("chat").isPresent()) {
            return;
        }
        // scene -> (provider, modelName, baseUrl)
        Map<String, String[]> data = Map.of(
                "chat", new String[]{"cloud", "deepseek-chat", null},
                "effect", new String[]{"cloud", "deepseek-chat", null},
                "agent", new String[]{"local", "minicpm-v:8b", "http://localhost:11434"},
                "classify", new String[]{"local", "minicpm-v:8b", "http://localhost:11434"});
        data.forEach((scene, config) -> {
            AiConfig aiConfig = new AiConfig();
            aiConfig.setScene(scene);
            aiConfig.setProvider(config[0]);
            aiConfig.setModelName(config[1]);
            aiConfig.setBaseUrl(config[2]);
            aiConfig.setApiKey("");
            aiConfig.setTemperature(new BigDecimal("0.70"));
            aiConfig.setIsActive(1);
            aiConfigRepository.save(aiConfig);
        });
    }

    private void saveOptions(String field, List<String> labels) {
        for (int i = 0; i < labels.size(); i++) {
            SurveyOption option = new SurveyOption();
            option.setField(field);
            option.setLabel(labels.get(i));
            option.setSort(i);
            surveyOptionRepository.save(option);
        }
    }

    /**
     * 保持插入顺序的 Map（Map.ofEntries 不保证顺序，分类排序号需要稳定顺序）
     */
    private static Map<String, String> orderedMap(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /**
     * 默认文章样式（方案 D 羊皮卷基调，与 UI-Plan §2 tokens 对应，BackEnd-Plan §6.3 结构）
     */
    private void seedArticleStyles() {
        if (articleStyleRepository.count() > 0) {
            return;
        }
        ArticleStyle style = new ArticleStyle();
        style.setName("羊皮卷默认样式");
        style.setStyleJson("""
                {"fontFamily":"Noto Serif SC","fontSize":17,"lineHeight":1.9,"letterSpacing":0,\
                "wordSpacing":0,"paragraphSpacing":16,"firstLineIndent":"2em","contentWidth":720,\
                "themeColor":"#8B6F47","serif":true,"customCss":""}""");
        style.setIsDefault(1);
        articleStyleRepository.save(style);
        log.info("seed: 默认文章样式已创建（羊皮卷基调）");
    }

    /**
     * 示例文章（阶段 2 开发数据，走通主页→列表→详情→相关文章链路；上线前清理，Stage.md §1.1）
     */
    private void seedArticles() {
        if (articleRepository.count() > 0) {
            return;
        }
        seedArticle("Spring Boot 4 初探：新版本的变与不变",
                "tech", new String[]{"Java", "Spring"},
                """
                        <p>Spring Boot 4 基于 Spring Framework 7，最大的变化是 Jackson 3 与 Jakarta EE 11。</p>\
                        <h2>Jackson 3</h2>\
                        <p>包名从 com.fasterxml 迁移到 tools.jackson，序列化器接口也换了名字。</p>\
                        <h2>模块化</h2>\
                        <p>starter 体系进一步拆分，MockMvc 测试需要单独引入 webmvc-test 模块。</p>""",
                "Spring Boot 4 初探：新版本的变与不变\n\nSpring Boot 4 基于 Spring Framework 7。\n\n## Jackson 3\n\n包名从 com.fasterxml 迁移到 tools.jackson。",
                5, true, 10);
        seedArticle("离线 IP 定位：ip2region 在个人站的落地",
                "tech", new String[]{"Java"},
                """
                        <p>统计游客地域不需要在线 API，一个 11MB 的 xdb 数据文件就够了。</p>\
                        <h2>BufferCache</h2>\
                        <p>整个文件加载进内存，查询微秒级，个人站毫无压力。</p>\
                        <h2>降级规则</h2>\
                        <p>城市未知时降级到省份，ECharts 地图按省着色。</p>""",
                "离线 IP 定位：ip2region 在个人站的落地\n\n统计游客地域不需要在线 API。",
                3, false, 0);
        seedArticle("以太小站上线记",
                "life", new String[]{"生活随想"},
                """
                        <p>筹备了很久的个人网站终于上线，记录一下选型与踩坑。</p>\
                        <h2>为什么叫以太</h2>\
                        <p>以太是古典物理学假想的介质，也是我想做的东西——承载内容本身的介质。</p>\
                        <h2>技术栈</h2>\
                        <p>Java 21 + Spring Boot 4 后端，Next.js 16 前端，MySQL 8.4。</p>""",
                "以太小站上线记\n\n筹备了很久的个人网站终于上线。",
                1, true, 5);
        seedArticle("关于羊皮卷配色的想法",
                "essay", new String[]{"生活随想"},
                """
                        <p>暖纸底色、深褐文字、以太金点缀——像一卷展开的旧羊皮纸。</p>\
                        <p>暗色模式则是深空底上的同系金色，星界与纸张的对照。</p>""",
                "关于羊皮卷配色的想法\n\n暖纸底色、深褐文字、以太金点缀。",
                8, false, 0);
        log.info("seed: 示例文章 4 篇已创建（阶段 2 开发数据）");
    }

    /**
     * 创建一篇已发布文章并绑定分类标签
     */
    private void seedArticle(String title, String categorySlug, String[] tagNames, String html,
                             String markdown, int daysAgo, boolean hot, int hotOrder) {
        Article article = new Article();
        article.setTitle(title);
        String plainText = html.replaceAll("<[^>]+>", "").trim();
        article.setSummary(plainText.substring(0, Math.min(80, plainText.length())));
        article.setContentHtml(html);
        article.setContentMd(markdown);
        article.setWordCount(plainText.length());
        article.setReadingCount(hot ? 10 + hotOrder : 0);
        article.setIsHot(hot ? 1 : 0);
        article.setHotOrder(hotOrder);
        article.setIsPublished(1);
        article.setPublishTime(java.time.LocalDateTime.now().minusDays(daysAgo));
        articleRepository.save(article);

        categoryRepository.findByBizTypeOrderBySortAsc("article").stream()
                .filter(c -> c.getSlug().equals(categorySlug))
                .findFirst()
                .ifPresent(c -> {
                    BizCategoryRel rel = new BizCategoryRel();
                    rel.setBizType("article");
                    rel.setBizId(article.getId());
                    rel.setCategoryId(c.getId());
                    bizCategoryRelRepository.save(rel);
                });
        for (String tagName : tagNames) {
            tagRepository.findByBizType("article").stream()
                    .filter(t -> t.getName().equals(tagName))
                    .findFirst()
                    .ifPresent(t -> {
                        BizTagRel rel = new BizTagRel();
                        rel.setBizType("article");
                        rel.setBizId(article.getId());
                        rel.setTagId(t.getId());
                        bizTagRelRepository.save(rel);
                    });
        }
    }

    /**
     * 示例图集（阶段 3 开发数据：程序生成渐变占位图，走通列表→详情→预览链路；上线前清理）
     *
     * <p>图片文件直接写入本地存储目录并登记 storage_file + storage_ref
     * （与上传链路产物同构，签名访问与删除一致性链路对 seed 数据同样生效）。</p>
     */
    private void seedAlbums() {
        if (albumRepository.count() > 0) {
            return;
        }
        // 渐变占位图（羊皮卷系配色）
        StorageFile coverA = seedPng("album-cover-a.png", 1200, 800, 0xB49A6E, 0x6E5636);
        StorageFile coverB = seedPng("album-cover-b.png", 1200, 800, 0x8FA3B8, 0x4A5E73);
        StorageFile photo1 = seedPng("album-a-1.png", 800, 600, 0xC9A86B, 0x8C6F3F);
        StorageFile photo2 = seedPng("album-a-2.png", 600, 800, 0xA8B8A0, 0x5E6E56);
        StorageFile photo3 = seedPng("album-a-3.png", 800, 600, 0xB87E6E, 0x78443A);
        StorageFile photo4 = seedPng("album-a-4.png", 600, 800, 0x9E9EC0, 0x54546E);
        StorageFile photo5 = seedPng("album-b-1.png", 800, 600, 0xB4A08C, 0x6E5A48);
        StorageFile photo6 = seedPng("album-b-2.png", 800, 600, 0x8CB4A8, 0x48706A);

        Album albumA = new Album();
        albumA.setTitle("山野拾光");
        albumA.setCoverFileId(coverA.getId());
        albumA.setIntro("山谷、溪流与旧木屋——一组暖色调的乡野记录。");
        albumA.setIsRecommend(1);
        albumA.setSort(10);
        albumRepository.save(albumA);
        bindRef(coverA.getId(), "album", albumA.getId());
        addSeedImage(albumA.getId(), photo1, "晨雾", "清晨薄雾未散时的山谷", 0);
        addSeedImage(albumA.getId(), photo2, "溪石", "溪水漫过卵石的长曝光", 1);
        addSeedImage(albumA.getId(), photo3, "旧木屋", "林间废弃木屋的一角", 2);
        addSeedImage(albumA.getId(), photo4, "暮色", "暮色里的远山剪影", 3);

        Album albumB = new Album();
        albumB.setTitle("城市漫步");
        albumB.setCoverFileId(coverB.getId());
        albumB.setIntro("在街道与天桥之间穿行，记录城市安静的一面。");
        albumB.setIsRecommend(1);
        albumB.setSort(5);
        albumRepository.save(albumB);
        bindRef(coverB.getId(), "album", albumB.getId());
        addSeedImage(albumB.getId(), photo5, "天桥", "黄昏天桥下的车流", 0);
        addSeedImage(albumB.getId(), photo6, "巷口", "老城区巷口的午后", 1);
        log.info("seed: 示例图集 2 个已创建（阶段 3 开发数据）");
    }

    /**
     * 示例视频（阶段 3 开发数据：程序构造最小 MP4 占位——仅 ftyp+moov/mvhd 无音视频轨，
     * 可验证 MP4 内置时长解析回退路径；真实视频由站长上传替换，上线前清理）
     */
    private void seedVideos() {
        if (videoRepository.count() > 0) {
            return;
        }
        StorageFile cover = storageFileRepository.findAll().stream()
                .filter(file -> file.getExt() != null && "png".equals(file.getExt()))
                .findFirst()
                .orElse(null);
        StorageFile videoFile1 = seedMp4("seed-video-1.mp4", 300);
        StorageFile videoFile2 = seedMp4("seed-video-2.mp4", 180);

        Video video1 = new Video();
        video1.setTitle("以太小站开发记录");
        video1.setCoverFileId(cover == null ? null : cover.getId());
        video1.setIntro("阶段 1-2 开发回顾：从脚手架到文章模块上线。");
        video1.setFileId(videoFile1.getId());
        video1.setDuration(videoFile1.getDuration());
        video1.setIsRecommend(1);
        videoRepository.save(video1);
        bindRef(videoFile1.getId(), "video", video1.getId());
        if (cover != null) {
            bindRef(cover.getId(), "video", video1.getId());
        }
        addSeedChapter(video1.getId(), "开场", 0, 0);
        addSeedChapter(video1.getId(), "后端基础", 60, 1);
        addSeedChapter(video1.getId(), "文章模块", 150, 2);
        addSeedChapter(video1.getId(), "总结", 260, 3);

        Video video2 = new Video();
        video2.setTitle("羊皮卷设计手记");
        video2.setCoverFileId(cover == null ? null : cover.getId());
        video2.setIntro("设计基调定稿回顾：配色、字体与动效规范。");
        video2.setFileId(videoFile2.getId());
        video2.setDuration(videoFile2.getDuration());
        video2.setIsRecommend(0);
        videoRepository.save(video2);
        bindRef(videoFile2.getId(), "video", video2.getId());
        if (cover != null) {
            bindRef(cover.getId(), "video", video2.getId());
        }
        addSeedChapter(video2.getId(), "定稿回顾", 0, 0);
        addSeedChapter(video2.getId(), "配色 Tokens", 45, 1);
        log.info("seed: 示例视频 2 个已创建（阶段 3 开发数据）");
    }

    /**
     * 生成渐变占位 PNG 并登记 storage_file（与上传链路产物同构）
     */
    private StorageFile seedPng(String originalName, int width, int height, int fromRgb, int toRgb) {
        byte[] bytes = renderGradientPng(width, height, fromRgb, toRgb);
        String objectKey = "files/" + UUID.randomUUID().toString().replace("-", "") + ".png";
        Path target = Path.of(storageProperties.getLocalBaseDir()).toAbsolutePath().normalize()
                .resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("seed 图片写入失败: " + objectKey, e);
        }
        StorageFile file = new StorageFile();
        file.setOriginalName(originalName);
        file.setStorageType(1);
        file.setObjectKey(objectKey);
        file.setSize((long) bytes.length);
        file.setFileMd5(DigestUtil.sha256Hex(new java.io.ByteArrayInputStream(bytes)));
        file.setMimeType("image/png");
        file.setExt("png");
        file.setWidth(width);
        file.setHeight(height);
        file.setStatus(1);
        return storageFileRepository.save(file);
    }

    /**
     * 构造最小 MP4（ftyp + moov/mvhd，无音视频轨）并登记 storage_file
     *
     * <p>合法 ISO BMFF 结构：ffprobe 与内置 mvhd 解析均可读出时长；
     * 播放器无轨无法播放，仅作列表/时长链路联调占位。</p>
     */
    private StorageFile seedMp4(String originalName, int durationSeconds) {
        ByteBuffer buf = ByteBuffer.allocate(136);
        // ftyp box（20 字节）：major=isom, minor=0x200, compat=isom
        buf.putInt(20);
        buf.put("ftyp".getBytes(StandardCharsets.US_ASCII));
        buf.put("isom".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(0x200);
        buf.put("isom".getBytes(StandardCharsets.US_ASCII));
        // moov box（116 字节）
        buf.putInt(116);
        buf.put("moov".getBytes(StandardCharsets.US_ASCII));
        // mvhd box v0（108 字节）：timescale=1000, duration=durationSeconds*1000
        buf.putInt(108);
        buf.put("mvhd".getBytes(StandardCharsets.US_ASCII));
        buf.put((byte) 0); // version
        buf.put(new byte[]{0, 0, 0}); // flags
        buf.putInt(0); // creation_time
        buf.putInt(0); // modification_time
        buf.putInt(1000); // timescale
        buf.putInt(durationSeconds * 1000); // duration
        buf.putInt(0x00010000); // rate 1.0
        buf.putShort((short) 0x0100); // volume 1.0
        buf.putShort((short) 0); // reserved
        buf.put(new byte[8]); // reserved[2]
        // 单位矩阵（36 字节）
        buf.putInt(0x00010000);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0x00010000);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0);
        buf.putInt(0x40000000);
        buf.put(new byte[24]); // pre_defined[6]
        buf.putInt(2); // next_track_ID
        byte[] bytes = buf.array();

        String objectKey = "files/" + UUID.randomUUID().toString().replace("-", "") + ".mp4";
        Path target = Path.of(storageProperties.getLocalBaseDir()).toAbsolutePath().normalize()
                .resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("seed 视频写入失败: " + objectKey, e);
        }
        StorageFile file = new StorageFile();
        file.setOriginalName(originalName);
        file.setStorageType(1);
        file.setObjectKey(objectKey);
        file.setSize((long) bytes.length);
        file.setFileMd5(DigestUtil.sha256Hex(new java.io.ByteArrayInputStream(bytes)));
        file.setMimeType("video/mp4");
        file.setExt("mp4");
        file.setDuration(durationSeconds);
        file.setStatus(1);
        return storageFileRepository.save(file);
    }

    /**
     * 渲染水平双色渐变 PNG（ImageIO 内存渲染，seed 专用）
     */
    private byte[] renderGradientPng(int width, int height, int fromRgb, int toRgb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int fromR = (fromRgb >> 16) & 0xFF;
        int fromG = (fromRgb >> 8) & 0xFF;
        int fromB = fromRgb & 0xFF;
        int toR = (toRgb >> 16) & 0xFF;
        int toG = (toRgb >> 8) & 0xFF;
        int toB = toRgb & 0xFF;
        for (int y = 0; y < height; y++) {
            double ratio = (double) y / Math.max(height - 1, 1);
            int r = fromR + (int) ((toR - fromR) * ratio);
            int g = fromG + (int) ((toG - fromG) * ratio);
            int b = fromB + (int) ((toB - fromB) * ratio);
            int rgb = (r << 16) | (g << 8) | b;
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("seed 图片渲染失败", e);
        }
    }

    /**
     * 登记业务-文件引用（seed 数据与上传链路产物同构，§8.3 删除一致性同样生效）
     */
    private void bindRef(Long fileId, String bizType, Long bizId) {
        if (fileId == null) {
            return;
        }
        StorageRef ref = new StorageRef();
        ref.setFileId(fileId);
        ref.setBizType(bizType);
        ref.setBizId(bizId);
        ref.setRefCount(1);
        storageRefRepository.save(ref);
    }

    /**
     * 添加图集图片（含排序/标题/介绍）并登记引用
     */
    private void addSeedImage(Long albumId, StorageFile file, String title, String intro, int sort) {
        AlbumImage image = new AlbumImage();
        image.setAlbumId(albumId);
        image.setFileId(file.getId());
        image.setTitle(title);
        image.setIntro(intro);
        image.setSort(sort);
        albumImageRepository.save(image);
        bindRef(file.getId(), "album", albumId);
    }

    /**
     * 添加视频关键时间节点
     */
    private void addSeedChapter(Long videoId, String title, int timeOffset, int sort) {
        VideoChapter chapter = new VideoChapter();
        chapter.setVideoId(videoId);
        chapter.setTitle(title);
        chapter.setTimeOffset(timeOffset);
        chapter.setSort(sort);
        videoChapterRepository.save(chapter);
    }
}
