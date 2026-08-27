package top.heyqing.aether.model.vo;

/**
 * 分类视图对象（全站字典，BackEnd-Plan §5.2 /categories）
 */
public record CategoryVO(Long id, String name, String slug, Integer sort) {
}
