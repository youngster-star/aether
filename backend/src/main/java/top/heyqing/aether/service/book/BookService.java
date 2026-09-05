package top.heyqing.aether.service.book;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.model.vo.BookChapterContentVO;
import top.heyqing.aether.model.vo.BookDetailVO;
import top.heyqing.aether.model.vo.BookListVO;

/**
 * 书籍公开服务（BackEnd-Plan §5.2 书籍 book 公开接口）
 */
public interface BookService {

    /**
     * 书籍分页（keyword 匹配书名/作者；categoryId/tagId 经关联表过滤）
     */
    PageResult<BookListVO> page(Long categoryId, Long tagId, String keyword, int page, int size);

    /**
     * 推荐书籍（is_recommend=1，最多 limit 条）
     */
    java.util.List<BookListVO> recommend(int limit);

    /**
     * 书籍详情（封面、简介、版权归属、章-节两级目录树）
     */
    BookDetailVO detail(Long id);

    /**
     * 章节内容（排版 HTML + 全书线性序 prev/next 导航）
     */
    BookChapterContentVO chapterContent(Long bookId, Long chapterId);
}
