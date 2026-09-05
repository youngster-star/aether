package top.heyqing.aether.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.BookAiTask;

/**
 * 书籍分章任务仓储（BackEnd-Plan §6.2 book_ai_task，状态机）
 *
 * <p>每本书同时至多一个有效任务：trigger 前先查最近任务，
 * status=1（解析中）拒绝重复触发，status=2 允许覆盖重跑，status=3 拒绝。</p>
 */
public interface BookAiTaskRepository extends JpaRepository<BookAiTask, Long> {

    /**
     * 书籍最近一次任务（id 最大）
     */
    Optional<BookAiTask> findFirstByBookIdOrderByIdDesc(Long bookId);
}
