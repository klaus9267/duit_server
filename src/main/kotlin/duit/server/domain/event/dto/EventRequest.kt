package duit.server.domain.event.dto

import duit.server.infrastructure.external.webhook.dto.FileInfo
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class EventRequest(
    val title: String,
    val startAt: LocalDate,
    val endAt: LocalDate?,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val uri: String,
    val thumbnail: String?,
    val eventType: EventType,
    val host: Host
) {
    fun toEntity() = Event(
        title = title,
        startAt = startAt,
        endAt = endAt,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        uri = uri,
        thumbnail = thumbnail,
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

        fun parseDate(date: String): LocalDate? {
            if (date.isBlank()) return null

            val trimmed = date.trim()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val parsedDate = LocalDate.parse(trimmed, formatter)

            if (parsedDate.isBefore(LocalDate.now())) {
                throw IllegalArgumentException("입력된 날짜는 미래여야 합니다. 입력값: $parsedDate")
            }

            return parsedDate
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

        fun from(formData: Map<String, String>, fileInfo: FileInfo?, host: Host) = EventRequest(
            title = formData.getValue("행사 제목"),
            startAt = parseDate(formData.getValue("행사 시작 날짜"))!!,
            endAt = formData["행사 종료 날짜"]?.let { parseDate(it) },
            uri = parseAndValidateUrl(formData.getValue("행사 정보 상세 정보 페이지 주소")),
            eventType = EventType.of(formData.getValue("행사 종류")),
            recruitmentStartAt = formData["모집 시작 날짜"]?.let { parseTime(it) },
            recruitmentEndAt = formData["모집 종료 날짜"]?.let { parseTime(it) },
            thumbnail = fileInfo?.directDownloadUrl,
            host = host,
        )
    }
}

