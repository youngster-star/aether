package top.heyqing.aether.controller.book;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import top.heyqing.aether.common.PageResult;
import top.heyqing.aether.common.Result;
import top.heyqing.aether.model.vo.BookChapterContentVO;
import top.heyqing.aether.model.vo.BookDetailVO;
import top.heyqing.aether.model.vo.BookListVO;
import top.heyqing.aether.service.book.BookService;

/**
 * 书籍公开接口（BackEnd-Plan §5.2 书籍 book 公开）
 */
@RestController
@RequestMapping("/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * 书籍分页列表（keyword 匹配书名/作者；categoryId/tagId 关联过滤）
     */
    @GetMapping
    public Result<PageResult<BookListVO>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return Result.ok(bookService.page(categoryId, tagId, keyword, page, size));
    }

    /**
     * 推荐书籍（主页推荐位）
     */
    @GetMapping("/recommend")
    public Result<List<BookListVO>> recommend(@RequestParam(defaultValue = "4") int limit) {
        return Result.ok(bookService.recommend(limit));
    }

    /**
     * 书籍详情（封面、简介、版权归属、章-节两级目录树）
     */
    @GetMapping("/{id}")
    public Result<BookDetailVO> detail(@PathVariable Long id) {
        return Result.ok(bookService.detail(id));
    }

    /**
     * 章节内容（分段排版 HTML + 全书线性序 prev/next 导航）
     */
    @GetMapping("/{id}/chapters/{chapterId}")
    public Result<BookChapterContentVO> chapterContent(@PathVariable Long id,
                                                       @PathVariable Long chapterId) {
        return Result.ok(bookService.chapterContent(id, chapterId));
    }
}
