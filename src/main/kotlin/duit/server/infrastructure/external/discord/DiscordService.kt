package duit.server.infrastructure.external.discord

import duit.server.domain.event.entity.Event
import duit.server.infrastructure.external.discord.dto.DiscordEmbed
import duit.server.infrastructure.external.discord.dto.DiscordField
import duit.server.infrastructure.external.discord.dto.DiscordThumbnail
import duit.server.infrastructure.external.discord.dto.DiscordWebhookMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

@Service
class DiscordService(
    private val restClient: RestClient = RestClient.builder().build()
) {

    private val logger = LoggerFactory.getLogger(DiscordService::class.java)

    @Value("\${discord.webhook.alarm.url:}")
    private val discordAlarmWebhookUrl: String? = null

    @Value("\${discord.webhook.error-monitoring.url:}")
    private val discordErrorWebhookUrl: String? = null

    fun sendNewEventNotification(event: Event) {
        if (discordAlarmWebhookUrl.isNullOrBlank()) {
            throw IllegalStateException("Discord webhook URL이 설정되지 않았습니다")
        }

        CompletableFuture.runAsync {
            try {
                val message = createEventNotificationMessage(event)

                restClient.post()
                    .uri(discordAlarmWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message)
                    .retrieve()
                    .toBodilessEntity()
            } catch (e: Exception) {
                throw RuntimeException("Discord 알림 전송에 실패했습니다: ${event.title}", e)
            }
        }
    }

    private fun createEventNotificationMessage(event: Event): DiscordWebhookMessage {
        val description = buildString {
            append("**${event.title}**\n\n")
            append("• **시작일:** ${event.startAt.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))}\n")

            event.endAt?.let {
                append("• **종료일:** ${it.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))}\n")
            }

            append("• **주최기관:** ${event.host.name}\n")
            append("• **행사 유형:** ${event.eventType.displayName}\n")

            event.recruitmentStartAt?.let { startAt ->
                val recruitmentPeriod = if (event.recruitmentEndAt != null) {
                    "${startAt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))} ~ " +
                            "${event.recruitmentEndAt?.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))}"
                } else {
                    "${startAt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))} ~"
                }
                append("• **모집 기간:** $recruitmentPeriod\n")
            }

            append("• **행사 URL:** ${event.uri}")
        }

        val embed = DiscordEmbed(
            title = "🎉 새로운 간호 행사가 등록되었습니다!",
            description = description,
            color = 0x00ff00,
            thumbnail = event.thumbnail?.let { DiscordThumbnail(it) },
            timestamp = java.time.Instant.now().toString()
        )

        return DiscordWebhookMessage(
            username = "Duty Bot",
            embeds = listOf(embed)
        )
    }

    /**
     * 500번대 서버 에러를 Discord로 알림
     *
     * @param errorCode 에러 코드 (예: INTERNAL_SERVER_ERROR)
     * @param message 에러 메시지
     * @param path 요청 경로
     * @param traceId 추적 ID
     * @param timestamp 발생 시각
     * @param exception 예외 객체 (스택트레이스 포함)
     */
    fun sendServerErrorNotification(
        errorCode: String,
        message: String,
        path: String,
        timestamp: LocalDateTime,
        exception: Exception
    ) {
        // errorWebhookUrl이 null이면 조용히 리턴
        if (discordErrorWebhookUrl.isNullOrBlank()) {
            logger.warn("Discord error webhook URL is not configured. Skipping error notification.")
            return
        }

        CompletableFuture.runAsync {
            try {
                val stackTrace = exception.stackTraceToString()
                    .take(1000)  // Discord 메시지 길이 제한 고려
                    .let { if (it.length >= 1000) "$it..." else it }

                val discordMessage = DiscordWebhookMessage(
                    username = "DU-IT Error Monitor",
                    embeds = listOf(
                        DiscordEmbed(
                            title = "🚨 서버 에러 발생",
                            description = """
                                **에러 코드**: `$errorCode`
                                **메시지**: $message
                                **경로**: `$path`
                                **발생 시각**: ${timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}
                            """.trimIndent(),
                            color = 0xFF0000,  // 빨강
                            fields = listOf(
                                DiscordField(
                                    name = "Stack Trace",
                                    value = "```\n$stackTrace\n```",
                                    inline = false
                                )
                            ),
                            timestamp = timestamp.atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        )
                    )
                )

                restClient.post()
                    .uri(discordErrorWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(discordMessage)
                    .retrieve()
                    .toBodilessEntity()

                logger.info("Server error notification sent to Discord")
            } catch (e: Exception) {
                logger.error("Failed to send error notification to Discord", e)
            }
        }
    }

}