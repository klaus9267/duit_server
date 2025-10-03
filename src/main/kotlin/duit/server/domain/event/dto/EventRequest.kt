package duit.server.domain.event.dto

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import duit.server.infrastructure.external.webhook.dto.FileInfo
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotNull
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class EventRequest(
    @field:NotNull
    val title: String,

    @field:Future
    @field:NotNull
    val startAt: LocalDateTime,

    @field:Future
    val endAt: LocalDateTime?,

    @field:Future
    @field:Schema(example = "9999-01-01T01:00:59")
    val recruitmentStartAt: LocalDateTime?,

    @field:Future
    @field:Schema(example = "9999-01-01T01:30:59")
    val recruitmentEndAt: LocalDateTime?,

    @field:NotNull
    val uri: String,

    @Hidden
    val eventThumbnail: String?,

    @field:NotNull
    val eventType: EventType,

    @field:NotNull
    val hostName: String,

    @Hidden
    val hostThumbnail: String?
) {
    fun toEntity(host: Host) = Event(
        title = title,
        startAt = startAt,
        endAt = endAt,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        uri = uri,
        thumbnail = eventThumbnail,
        eventType = eventType,
        host = host
    )


    companion object {
        fun parseAndValidateUrl(url: String): String {
            val trimmed = url.trim()

            val uri = URI(trimmed)

            if (uri.host.isNullOrBlank() || uri.scheme !in listOf("http", "https")) {
                throw IllegalArgumentException("잘못된 URI입니다: $uri")
            }

            return uri.toString()
        }

        fun parseTime(time: String): LocalDateTime? {
            if (time.isBlank()) return null

            val trimmed = time.trim()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val parsedTime = LocalDateTime.parse(trimmed, formatter)

            if (parsedTime.isBefore(LocalDateTime.now())) {
                throw IllegalArgumentException("입력된 시간은 미래여야 합니다. 입력값: $parsedTime")
            }

            return parsedTime
        }

        fun from(formData: Map<String, String>, eventThumbnail: FileInfo?, hostThumbnail: FileInfo? = null) = EventRequest(
            title = formData.getValue("행사 제목"),
            startAt = formData["행사 시작 날짜"]?.let { parseTime(it) }!!,
            endAt = formData["행사 종료 날짜"]?.let { parseTime(it) },
            uri = parseAndValidateUrl(formData.getValue("행사 정보 상세 정보 페이지 주소")),
            eventType = EventType.of(formData.getValue("행사 종류")),
            recruitmentStartAt = formData["모집 시작 날짜"]?.let { parseTime(it) },
            recruitmentEndAt = formData["모집 종료 날짜"]?.let { parseTime(it) },
            eventThumbnail = eventThumbnail?.directDownloadUrl,
            hostName = formData["주최 기관명"]!!,
            hostThumbnail = hostThumbnail?.directDownloadUrl,
        )
    }
}

