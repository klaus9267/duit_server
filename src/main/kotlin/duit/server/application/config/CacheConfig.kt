package duit.server.application.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.event.dto.EventResponseV2
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
 *
 * 캐싱 제외 조건 (EventService.isCacheable):
 *   bookmarked=true, searchKeyword≠null, hostId≠null, field=VIEW_COUNT
 *
 * 직렬화 전략: Jackson2JsonRedisSerializer + TypeReference<CursorPageResponse<EventResponseV2>>
 *   - @class 타입 정보 불필요: 역직렬화 대상 타입을 TypeReference로 명시
 *   - 타입 소거(Type Erasure) 문제 없음: JavaType이 제네릭 정보 보존
 *   - JavaTimeModule: LocalDateTime 직렬화 지원
 */
@Configuration
@EnableCaching
@Profile("!test")
class CacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory
) {

    private fun cacheObjectMapper(): ObjectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(kotlinModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private fun valueSerializer(): Jackson2JsonRedisSerializer<CursorPageResponse<EventResponseV2>> {
        val mapper = cacheObjectMapper()
        val javaType = mapper.typeFactory.constructType(
            object : TypeReference<CursorPageResponse<EventResponseV2>>() {}
        )
        return Jackson2JsonRedisSerializer(mapper, javaType)
    }

    private fun cacheConfig(ttl: Duration): RedisCacheConfiguration =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer())
            )
            .disableCachingNullValues()

    @Bean
    fun cacheManager(): CacheManager =
        RedisCacheManager.builder(redisConnectionFactory)
            .withCacheConfiguration("events-5m", cacheConfig(Duration.ofMinutes(5)))
            .withCacheConfiguration("events-3m", cacheConfig(Duration.ofMinutes(3)))
            .build()
}
