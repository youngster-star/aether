package top.heyqing.aether.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 业务异步线程池配置
 *
 * <p>bookSplitExecutor：书籍分章任务执行（BackEnd-Plan §7.2 触发后异步跑，
 * 管理端轮询任务状态）；队列有界防任务堆积，拒绝策略由调用方捕获异常落任务
 * 失败态（CallerRuns 兜底直接在提交线程执行）。</p>
 */
@Configuration
public class ExecutorConfiguration {

    @Bean("bookSplitExecutor")
    public Executor bookSplitExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("book-split-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
