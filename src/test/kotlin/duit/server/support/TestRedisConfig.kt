package duit.server.support

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations

/**
 * 테스트 환경에서 Redis 연결 없이 Mock RedisTemplate 제공
 * 실제 Redis 서버 없이도 테스트 실행 가능
 */
@TestConfiguration
class TestRedisConfig {

    @Bean
    @Primary
    fun redisConnectionFactory(): RedisConnectionFactory {
        return Mockito.mock(RedisConnectionFactory::class.java)
    }

    @Bean
    @Primary
    @Suppress("UNCHECKED_CAST")
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = Mockito.mock(RedisTemplate::class.java) as RedisTemplate<String, Any>
        val valueOps = Mockito.mock(ValueOperations::class.java) as ValueOperations<String, Any>

        Mockito.`when`(template.opsForValue()).thenReturn(valueOps)
        Mockito.`when`(template.hasKey(Mockito.anyString())).thenReturn(false)

        return template
    }
}