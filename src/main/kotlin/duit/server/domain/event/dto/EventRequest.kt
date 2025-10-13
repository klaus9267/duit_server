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

        private fun parseDate(date: String): java.time.LocalDate {
            val trimmed = date.trim()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            return java.time.LocalDate.parse(trimmed, formatter)
        }

        private fun parseTime(time: String): java.time.LocalTime {
            val trimmed = time.trim()
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            return java.time.LocalTime.parse(trimmed, formatter)
        }

        private fun combineDateAndTime(date: String?, time: String?): LocalDateTime? {
            if (date.isNullOrBlank()) return null

            val parsedDate = parseDate(date)
            val parsedTime = if (time.isNullOrBlank()) {
                java.time.LocalTime.of(0, 0) // 시간이 없으면 00:00
            } else {
                parseTime(time)
            }

            return LocalDateTime.of(parsedDate, parsedTime)
        }

        fun from(
            formData: Map<String, String>,
            eventThumbnail: FileInfo?,
            hostThumbnail: FileInfo? = null
        ): EventRequest {
            val startAt = combineDateAndTime(
                formData["행사 시작 날짜"],
                formData["행사 시작 시간"]
            ) ?: throw IllegalArgumentException("행사 시작 날짜는 필수입니다")

            val endAt = combineDateAndTime(
                formData["행사 종료 날짜"],
                formData["행사 종료 시간"]
            )

            val recruitmentStartAt = combineDateAndTime(
                formData["모집 시작 날짜"],
                formData["모집 시작 시간"]
            )

            val recruitmentEndAt = combineDateAndTime(
                formData["모집 종료 날짜"],
                formData["모집 종료 시간"]
            )

            return EventRequest(
                title = formData.getValue("행사 제목"),
                startAt = startAt,
                endAt = endAt,
                uri = parseAndValidateUrl(formData.getValue("행사 정보 상세 정보 페이지 주소")),
                eventType = EventType.of(formData.getValue("행사 종류")),
                recruitmentStartAt = recruitmentStartAt,
                recruitmentEndAt = recruitmentEndAt,
                eventThumbnail = eventThumbnail?.directDownloadUrl,
                hostName = formData["주최 기관명"]!!,
                hostThumbnail = hostThumbnail?.directDownloadUrl,
            )
        }
    }
}

