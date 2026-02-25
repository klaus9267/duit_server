package duit.server.application.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class RedisConnectionValidator(
    private val redisTemplate: RedisTemplate<String, Any>
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        try {
            redisTemplate.connectionFactory?.connection?.use { connection ->
                connection.ping()
            } ?: throw IllegalStateException("RedisConnectionFactory가 null입니다")

            logger.info("Redis 연결 확인 완료")
        } catch (e: Exception) {
            throw IllegalStateException("Redis 연결 실패 — 서버를 시작할 수 없습니다", e)
        }
    }
}
