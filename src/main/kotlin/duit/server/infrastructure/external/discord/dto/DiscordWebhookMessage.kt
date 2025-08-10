package duit.server.infrastructure.external.discord.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class DiscordWebhookMessage(
    val content: String? = null,
    val username: String? = null,
    @JsonProperty("avatar_url")
    val avatarUrl: String? = null,
    val embeds: List<DiscordEmbed>? = null
)

data class DiscordEmbed(
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val color: Int? = null,
    val timestamp: String? = null,
    val thumbnail: DiscordThumbnail? = null,
    val fields: List<DiscordField>? = null,
    val footer: DiscordFooter? = null
)

data class DiscordThumbnail(
    val url: String
)

data class DiscordField(
    val name: String,
    val value: String,
    val inline: Boolean = false
)

data class DiscordFooter(
    val text: String,
    @JsonProperty("icon_url")
    val iconUrl: String? = null
)