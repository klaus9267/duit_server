package duit.server.application.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DataAccessException
import java.time.Duration

/**
 * 캐시(Redis) 서킷 브레이커 설정 (performance/decisions.md D-027). 운영·테스트 캐시 매니저가 함께 쓴다.
 *
 * - 최근 20번 중 절반 이상 실패(최소 10번)면 연다. 장애 시작 후 첫 1초(명령 타임아웃) 동안 쌓인 실패로 열린다.
 * - 열린 뒤 10초마다 3번만 시험한다. 그 3번은 Redis 가 아직 멈춰 있으면 타임아웃만큼 기다린다.
 * - 실패로 세는 것은 Spring 이 변환한 Redis 예외(DataAccessException: 타임아웃·연결 실패)뿐이다. 직렬화 오류는 Redis 장애가 아니다.
 * - 느린 호출 기준은 두지 않는다. CPU 포화로 Redis 호출이 느려진 것을 장애로 보고 열면 캐시가 꺼져 DB 로 몰리고,
 *   과부하가 더 커진다(양의 되먹임).
 * - 열린 동안 거절은 요청마다 일어나므로 예외 스택 트레이스를 만들지 않는다.
 * 상태·호출 수는 resilience4j_circuitbreaker_* 지표로 Prometheus 에 나간다.
 */
@Configuration
class CacheCircuitBreakerConfig {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun cacheCircuitBreaker(meterRegistry: MeterRegistry): CircuitBreaker {
        val config = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(SLIDING_WINDOW_SIZE)
            .minimumNumberOfCalls(MINIMUM_CALLS)
            .failureRateThreshold(FAILURE_RATE_THRESHOLD)
            .waitDurationInOpenState(WAIT_IN_OPEN)
            .permittedNumberOfCallsInHalfOpenState(HALF_OPEN_CALLS)
            .recordExceptions(DataAccessException::class.java)
            .writableStackTraceEnabled(false)
            .build()
        val registry = CircuitBreakerRegistry.of(config)
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry)

        return registry.circuitBreaker(NAME).apply {
            eventPublisher.onStateTransition { logger.warn("캐시(Redis) 서킷 브레이커 {}", it.stateTransition) }
        }
    }

    companion object {
        const val NAME = "redis-cache"
        private const val SLIDING_WINDOW_SIZE = 20
        private const val MINIMUM_CALLS = 10
        private const val FAILURE_RATE_THRESHOLD = 50f
        private const val HALF_OPEN_CALLS = 3
        private val WAIT_IN_OPEN = Duration.ofSeconds(10)
    }
}
