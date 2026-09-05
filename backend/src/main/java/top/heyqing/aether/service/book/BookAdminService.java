package top.heyqing.aether.service.book;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.model.dto.BookSaveRequest;
import top.heyqing.aether.model.vo.BookAdminVO;

/**
 * 书籍管理端服务（BackEnd-Plan §5.2 /admin/books，需 Access Token）
 */
public interface BookAdminService {

    /**
     * 管理端分页列表（keyword 匹配书名/作者）
     */
    PageResult<BookAdminVO> list(int page, int size, String keyword);

    /**
     * 新建书籍（封面/source 文件须为已上传的正常文件并登记引用）
     */
    Long create(BookSaveRequest request);

    /**
     * 编辑书籍（换文件时旧引用解绑）
     */
    void update(Long id, BookSaveRequest request);

    /**
     * 删除书籍（章节/分章任务/文件引用一并解绑，引用归零异步物理清理）
     */
    void delete(Long id);
}
