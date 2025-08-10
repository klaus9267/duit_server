package duit.server.infrastructure.external.discord.exception

import duit.server.domain.common.exception.DomainException

class DiscordWebhookUrlNotConfiguredException : 
    DomainException("Discord webhook URL is not configured")

class DiscordNotificationFailedException(val eventTitle: String, cause: Throwable) : 
    DomainException("Failed to send Discord notification for event: $eventTitle", cause)