package top.heyqing.aether;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import top.heyqing.aether.common.ErrorCode;
import top.heyqing.aether.exception.BusinessException;
import top.heyqing.aether.model.dto.ArticleHotRequest;
import top.heyqing.aether.model.dto.ArticleSaveRequest;
import top.heyqing.aether.model.dto.CategorySaveRequest;
import top.heyqing.aether.model.vo.ArticleAdminVO;
import top.heyqing.aether.service.article.ArticleAdminService;
import top.heyqing.aether.service.article.ArticleService;
import top.heyqing.aether.service.article.CategoryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文章模块功能测试（阶段 2 完成标准，BackEnd-Plan §5.2）
 *
 * <p>基于 seed 示例数据（4 篇文章：技术 2 篇、生活 1 篇、随笔 1 篇，热门 2 篇）验证：
 * 搜索准确（标题/简介/内容）、分类标签过滤、热门排序、阅读数 IP 24h 去重、
 * 相关文章、草稿不可公开、管理 CRUD 与 XSS sanitize、字典防呆。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ArticleFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleAdminService articleAdminService;

    @Autowired
    private CategoryService categoryService;

    @Test
    @DisplayName("搜索准确：标题/内容命中，无关关键词 0 结果")
    void searchByTitleAndContent() throws Exception {
        // 标题命中（seed「离线 IP 定位」1 篇）
        list("keyword", "ip2region").andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("离线 IP 定位：ip2region 在个人站的落地"));
        // 仅内容命中（BufferCache 只出现在正文里）
        list("keyword", "BufferCache").andExpect(jsonPath("$.data.total").value(1));
        // 简介命中（seed 简介截取自正文纯文本，「筹备了很久」在简介与正文中）
        list("keyword", "筹备了很久").andExpect(jsonPath("$.data.total").value(1));
        // 无关关键词 0 结果
        list("keyword", "量子纠缠不存在的词").andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("分类/标签过滤与最新排序")
    void filterByCategoryAndTag() throws Exception {
        // tech 分类 2 篇
        long techId = categoryService.listCategories("article").stream()
                .filter(c -> c.slug().equals("tech")).findFirst().orElseThrow().id();
        list("categoryId", String.valueOf(techId)).andExpect(jsonPath("$.data.total").value(2));
        // 标签 Spring 1 篇（seed 按标签名查询）
        var tags = articleService.listTags("article");
        long springId = tags.stream().filter(t -> t.name().equals("Spring")).findFirst().orElseThrow().id();
        list("tagId", String.valueOf(springId)).andExpect(jsonPath("$.data.total").value(1));
        // 默认 latest 排序：最新发布（daysAgo=1 的「以太小站上线记」）在最前
        list().andExpect(jsonPath("$.data.records[0].title").value("以太小站上线记"));
    }

    @Test
    @DisplayName("热门接口：hot_order 降序且仅返回热门")
    void hotListOrderedByHotOrder() throws Exception {
        mockMvc.perform(get("/v1/articles/hot").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("Spring Boot 4 初探：新版本的变与不变"))
                .andExpect(jsonPath("$.data[1].title").value("以太小站上线记"));
    }

    @Test
    @DisplayName("详情阅读数：同 IP 24h 只计 1 次，不同 IP 各自计数")
    void readingCountDedupByIp() throws Exception {
        long articleId = articleIdOf("ip2region");
        int base = articleService.pageList(new top.heyqing.aether.model.dto.ArticleQuery(1, 50, null, null,
                "ip2region", "latest")).records().get(0).readingCount();

        // 同 IP 第一次 +1，第二次不重复计
        detail(articleId, "10.9.9.1").andExpect(jsonPath("$.data.readingCount").value(base + 1));
        detail(articleId, "10.9.9.1").andExpect(jsonPath("$.data.readingCount").value(base + 1));
        // 不同 IP 再 +1
        detail(articleId, "10.9.9.2").andExpect(jsonPath("$.data.readingCount").value(base + 2));
        // 不同 IP 24h 内再次访问不重复计
        detail(articleId, "10.9.9.2").andExpect(jsonPath("$.data.readingCount").value(base + 2));
    }

    @Test
    @DisplayName("相关文章：同分类优先（tech 分类内互相关联）")
    void relatedArticlesSameCategoryFirst() throws Exception {
        long ip2regionId = articleIdOf("ip2region");
        mockMvc.perform(get("/v1/articles/" + ip2regionId + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].title").value("Spring Boot 4 初探：新版本的变与不变"));
    }

    @Test
    @DisplayName("草稿不可公开：发布前列表/详情不可见，发布后可见，script 标签被剥除")
    void draftHiddenUntilPublishedAndXssSanitized() throws Exception {
        // 创建含 XSS 的草稿
        Long draftId = articleAdminService.create(new ArticleSaveRequest(
                "阶段2功能测试草稿",
                null,
                "测试简介",
                "<p>正文内容</p><script>alert('xss')</script>",
                "阶段2功能测试草稿 md",
                null, null, null));

        // 公开列表不可见、公开详情 30001
        list("keyword", "阶段2功能测试草稿").andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/v1/articles/" + draftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30001));

        // sanitize：script 被剥除
        ArticleAdminVO draft = articleAdminService.get(draftId);
        assertFalse(draft.contentHtml().contains("<script"), "script 标签应被 sanitize 剥除");
        assertTrue(draft.contentHtml().contains("正文内容"), "正常内容应保留");

        // 发布后公开可见
        articleAdminService.publish(draftId, 1);
        list("keyword", "阶段2功能测试草稿").andExpect(jsonPath("$.data.total").value(1));

        // 编辑更新标题生效
        articleAdminService.update(draftId, new ArticleSaveRequest(
                "阶段2功能测试草稿-改", null, "测试简介",
                "<p>更新后的正文</p>", "", null, null, null));
        list("keyword", "阶段2功能测试草稿-改").andExpect(jsonPath("$.data.total").value(1));

        // 删除后不可见
        articleAdminService.delete(draftId);
        list("keyword", "阶段2功能测试草稿-改").andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("管理操作：热度设置与样式绑定生效")
    void hotAndStyleBinding() throws Exception {
        Long articleId = articleAdminService.create(new ArticleSaveRequest(
                "热度绑定测试", null, "简介", "<p>内容</p>", "", null, null, null));
        // 设置热门
        articleAdminService.setHot(articleId, new ArticleHotRequest(1, 3));
        // 绑定默认样式（seed 已建默认样式）
        long defaultStyleId = articleService.detail(
                articleIdOf("ip2region"), "10.9.9.9").style().id();
        articleAdminService.bindStyle(articleId, defaultStyleId);
        ArticleAdminVO vo = articleAdminService.get(articleId);
        assertEquals(1, vo.isHot());
        assertEquals(3, vo.hotOrder());
        assertEquals(defaultStyleId, vo.articleStyleId());
        articleAdminService.delete(articleId);
    }

    @Test
    @DisplayName("字典防呆：slug 重复拒绝、被使用分类禁止删除、未使用可删")
    void categoryGuardrails() {
        // slug 重复（seed 已有 tech）
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> categoryService.createCategory(new CategorySaveRequest("重复", "tech", "article", 99)));
        assertEquals(ErrorCode.VALIDATION_FAILED.getCode(), e1.getErrorCode().getCode());
        // 被使用的分类禁止删除（tech 被 seed 文章使用）
        long techId = categoryService.listCategories("article").stream()
                .filter(c -> c.slug().equals("tech")).findFirst().orElseThrow().id();
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> categoryService.deleteCategory(techId));
        assertEquals(ErrorCode.VALIDATION_FAILED.getCode(), e2.getErrorCode().getCode());
        // 未使用分类可正常创建后删除
        Long freshId = categoryService.createCategory(
                new CategorySaveRequest("测试分类", "test-cat-x", "article", 0));
        categoryService.deleteCategory(freshId);
        assertFalse(categoryService.listCategories("article").stream().anyMatch(c -> c.id().equals(freshId)));
    }

    /**
     * 分页列表请求（public /v1/articles；参数以 key/value 成对传入，
     * 走 param() 编码，避免中文关键词拼接 URI 报非法字符）
     */
    private org.springframework.test.web.servlet.ResultActions list(String... params) throws Exception {
        var request = get("/v1/articles");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 详情请求（X-Forwarded-For 指定 IP，验证阅读去重）
     */
    private org.springframework.test.web.servlet.ResultActions detail(long articleId, String ip) throws Exception {
        return mockMvc.perform(get("/v1/articles/" + articleId).header("X-Forwarded-For", ip))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 按标题关键词从公开列表取文章 ID（seed 数据定位用）
     */
    private long articleIdOf(String keyword) throws Exception {
        String content = mockMvc.perform(get("/v1/articles").param("keyword", keyword))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode records = objectMapper.readTree(content).path("data").path("records");
        assertEquals(1, records.size(), "关键词应唯一定位一篇 seed 文章");
        return records.get(0).path("id").asLong();
    }
}
