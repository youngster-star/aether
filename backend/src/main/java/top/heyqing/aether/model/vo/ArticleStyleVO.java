package top.heyqing.aether.model.vo;

/**
 * 文章样式视图对象（BackEnd-Plan §6.2 article_style）
 */
public record ArticleStyleVO(Long id, String name, String styleJson, Integer isDefault) {
}
