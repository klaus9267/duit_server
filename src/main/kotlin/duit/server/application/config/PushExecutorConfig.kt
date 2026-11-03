package duit.server.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 구독 푸시(FCM) 전용 스레드 풀 (performance/decisions.md D-023).
 * 승인 트랜잭션이 커밋된 뒤 여기서 보낸다 — 요청 스레드와 DB 커넥션이 FCM 왕복을 기다리지 않게.
 */
@Configuration
class PushExecutorConfig {

    @Bean(PUSH_EXECUTOR)
    fun pushExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = POOL_SIZE
        maxPoolSize = POOL_SIZE
        queueCapacity = QUEUE_CAPACITY
        setThreadNamePrefix("push-")
        // 큐가 차면 버리지 않고 커밋한 스레드가 직접 보낸다 (느려질 뿐 유실은 없다)
        setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        // 배포로 앱이 내려갈 때 대기 중인 푸시를 보내고 종료한다
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS)
    }

    companion object {
        const val PUSH_EXECUTOR = "pushExecutor"
        private const val POOL_SIZE = 4
        private const val QUEUE_CAPACITY = 10_000
        private const val AWAIT_TERMINATION_SECONDS = 30
    }
}
