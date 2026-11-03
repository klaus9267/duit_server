package duit.server.application.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import duit.server.domain.bookmark.service.BookmarkedEventIdsCache
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.event.dto.EventResponseV2
import duit.server.domain.host.dto.HostResponse
import duit.server.domain.host.service.HostService
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/**
 * Spring Cache 설정 (Redis 캐시)
 * - events-5m: CREATED_AT / ID 정렬 (TTL 5분)
 * - events-3m: START_DATE / RECRUITMENT_DEADLINE 정렬 (TTL 3분)
 * - hosts: 주최기관 전체 목록 (TTL 10분, 주최기관 생성·수정·삭제 시 비움 — HostService)
 * - bookmarked-event-ids: 사용자별 북마크한 행사 ID 목록 (TTL 10분, 토글 커밋 뒤 무효화 — BookmarkedEventIdsCache)
 *
 * 캐싱 제외 조건 (EventService.isCacheable):
 *   bookmarked=true, searchKeyword≠null, hostId≠null, field=VIEW_COUNT
 *
 * 직렬화 전략: Jackson2JsonRedisSerializer + TypeReference<CursorPageResponse<EventResponseV2>>
 *   - @class 타입 정보 불필요: 역직렬화 대상 타입을 TypeReference로 명시
 *   - 타입 소거(Type Erasure) 문제 없음: JavaType이 제네릭 정보 보존
 *   - JavaTimeModule: LocalDateTime 직렬화 지원
 *
 * 모든 캐시는 서킷 브레이커로 감싼다 — Redis 가 멈추면 일정 시간 캐시를 건너뛴다 (CacheCircuitBreakerConfig, performance D-027)
 *
 * 키 prefix 의 [CACHE_SCHEMA_VERSION]: EventResponseV2 구조가 바뀌면 올린다.
 *   blue-green 전환 중 구/신 서버가 같은 Redis 를 쓰므로, 버전이 다르면 서로의 캐시를 읽지 않아 역직렬화 오류가 나지 않는다.
 *   (v2: 행사 다중 주최 — hosts 필드 추가)
 */
@Configuration
@EnableCaching
@Profile("!test")
class CacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
    private val cacheCircuitBreaker: CircuitBreaker,
) {

    private fun cacheObjectMapper(): ObjectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(kotlinModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private fun <T> valueSerializer(type: TypeReference<T>): Jackson2JsonRedisSerializer<T> {
        val mapper = cacheObjectMapper()
        return Jackson2JsonRedisSerializer(mapper, mapper.typeFactory.constructType(type))
    }

    private fun cacheConfig(ttl: Duration, type: TypeReference<*> = EVENT_PAGE): RedisCacheConfiguration =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .prefixCacheNameWith("$CACHE_SCHEMA_VERSION:")
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer(type))
            )
            .disableCachingNullValues()

    @Bean
    fun cacheManager(): CacheManager {
        val redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
            .withCacheConfiguration("events-5m", cacheConfig(Duration.ofMinutes(5)))
            .withCacheConfiguration("events-3m", cacheConfig(Duration.ofMinutes(3)))
            // 무효화는 토글 커밋 뒤에 한다. TTL 은 무효화를 놓친 경우(커밋과 무효화 사이 동시 적재 등)의 상한
            .withCacheConfiguration(BookmarkedEventIdsCache.CACHE_NAME, cacheConfig(Duration.ofMinutes(10), EVENT_IDS))
            .withCacheConfiguration(HostService.HOSTS_CACHE, cacheConfig(Duration.ofMinutes(10), HOST_PAGE))
            .build()
            // 빈이 아니라서 Spring 이 초기화해 주지 않는다. 안 하면 위 캐시별 설정(TTL·직렬화)이 안 실리고 기본값 캐시가 생긴다
            .apply { afterPropertiesSet() }
        return CircuitBreakingCacheManager(redisCacheManager, cacheCircuitBreaker)
    }

    companion object {
        const val CACHE_SCHEMA_VERSION = "v2"
        private val EVENT_PAGE = object : TypeReference<CursorPageResponse<EventResponseV2>>() {}
        private val EVENT_IDS = object : TypeReference<List<Long>>() {}
        private val HOST_PAGE = object : TypeReference<PageResponse<HostResponse>>() {}
    }
}
