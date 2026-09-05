package top.heyqing.aether.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.BookChapter;

/**
 * 书籍章节仓储（BackEnd-Plan §6.2 book_chapter，章-节两级结构）
 */
public interface BookChapterRepository extends JpaRepository<BookChapter, Long> {

    /**
     * 书籍全部章节（order_no 升序 = 全书线性序）
     */
    List<BookChapter> findByBookIdOrderByOrderNoAsc(Long bookId);

    /**
     * 删除书籍全部章节（重新分章确认落库前清空）
     */
    void deleteByBookId(Long bookId);

    /**
     * 章节数（level=1 章，total_chapters 维护依据）
     */
    long countByBookIdAndLevel(Long bookId, Integer level);
}
