package top.heyqing.aether.config;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import top.heyqing.aether.model.entity.AiConfig;
import top.heyqing.aether.model.entity.Category;
import top.heyqing.aether.model.entity.SurveyOption;
import top.heyqing.aether.model.entity.SysUser;
import top.heyqing.aether.model.entity.Tag;
import top.heyqing.aether.repository.AiConfigRepository;
import top.heyqing.aether.repository.CategoryRepository;
import top.heyqing.aether.repository.SurveyOptionRepository;
import top.heyqing.aether.repository.SysUserRepository;
import top.heyqing.aether.repository.TagRepository;

/**
 * seed 数据初始化（BackEnd-Plan §6.4）
 *
 * <p>仅 dev/test 环境执行（@Profile 控制，prod 不跑）：
 * 管理员账户、分类/标签字典、问卷选项、AI 默认配置。全部幂等（存在即跳过）。</p>
 */
@Component
@Profile({"dev", "test"})
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
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DataSeeder(SysUserRepository sysUserRepository, CategoryRepository categoryRepository,
                      TagRepository tagRepository, SurveyOptionRepository surveyOptionRepository,
                      AiConfigRepository aiConfigRepository, PasswordEncoder passwordEncoder,
                      Environment environment) {
        this.sysUserRepository = sysUserRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.surveyOptionRepository = surveyOptionRepository;
        this.aiConfigRepository = aiConfigRepository;
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
}
