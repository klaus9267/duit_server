package duit.server.infrastructure.external.discord

import duit.server.domain.event.entity.Event
import duit.server.infrastructure.external.discord.dto.DiscordEmbed
import duit.server.infrastructure.external.discord.dto.DiscordThumbnail
import duit.server.infrastructure.external.discord.dto.DiscordWebhookMessage
import duit.server.infrastructure.external.discord.exception.DiscordNotificationFailedException
import duit.server.infrastructure.external.discord.exception.DiscordWebhookUrlNotConfiguredException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture

@Service
class DiscordService(
    private val restClient: RestClient = RestClient.builder().build()
) {

    @Value("\${discord.webhook.url}")
    private val discordWebhookUrl: String? = null

    fun sendNewEventNotification(event: Event) {
        if (discordWebhookUrl.isNullOrBlank()) {
            throw DiscordWebhookUrlNotConfiguredException()
        }

        CompletableFuture.runAsync {
            try {
                val message = createEventNotificationMessage(event)

                restClient.post()
                    .uri(discordWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message)
                    .retrieve()
                    .toBodilessEntity()
            } catch (e: Exception) {
                throw DiscordNotificationFailedException(event.title, e)
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
}