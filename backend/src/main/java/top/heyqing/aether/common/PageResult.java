package top.heyqing.aether.common;

import java.util.List;

/**
 * 分页统一结构（BackEnd-Plan §3.1）
 *
 * <p>作为 {@link Result#data()} 的分页负载使用。</p>
 *
 * @param <T> 记录类型
 */
public record PageResult<T>(List<T> records, long total, int page, int size) {

    /**
     * 构建分页结果
     *
     * @param records 当前页记录
     * @param total   总记录数
     * @param page    页码（从 1 开始）
     * @param size    每页大小
     */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        return new PageResult<>(records, total, page, size);
    }
}
