package duit.server.application.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/**
 * Redis 캐시 전용 ObjectMapper 팩토리
 * KotlinModule + JavaTimeModule만 등록하며,
 * 역직렬화 시 TypeReference로 제네릭 타입 정보를 제공
 */
object CacheConfig {

    fun createCacheObjectMapper(): ObjectMapper =
        ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}