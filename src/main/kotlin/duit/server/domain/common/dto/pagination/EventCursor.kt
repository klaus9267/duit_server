package duit.server.domain.common.dto.pagination

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import duit.server.domain.event.entity.Event
import java.time.LocalDateTime
import java.util.Base64

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = EventCursor.CreatedAtCursor::class, name = "CREATED_AT"),
    JsonSubTypes.Type(value = EventCursor.StartDateCursor::class, name = "START_DATE"),
    JsonSubTypes.Type(value = EventCursor.RecruitmentDeadlineCursor::class, name = "RECRUITMENT_DEADLINE"),
    JsonSubTypes.Type(value = EventCursor.ViewCountCursor::class, name = "VIEW_COUNT"),
    JsonSubTypes.Type(value = EventCursor.IdCursor::class, name = "ID")
)
sealed interface EventCursor {

    data class CreatedAtCursor(
        val createdAt: LocalDateTime,
        val id: Long
    ) : EventCursor

    data class StartDateCursor(
        val startAt: LocalDateTime,
        val id: Long
    ) : EventCursor

    data class RecruitmentDeadlineCursor(
        val recruitmentEndAt: LocalDateTime,
        val id: Long
    ) : EventCursor

    data class ViewCountCursor(
        val count: Long,
        val id: Long
    ) : EventCursor

    data class IdCursor(
        val id: Long
    ) : EventCursor

    companion object {
        internal val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())

        /**
         * Base64로 인코딩된 커서 문자열을 디코딩하여 EventCursor 객체로 변환
         *
         * @param encoded Base64로 인코딩된 커서 문자열
         * @param field 정렬 필드 (커서 타입 결정용)
         * @return 디코딩된 EventCursor 객체
         * @throws IllegalArgumentException 잘못된 커서 형식 또는 디코딩 실패 시
         */
        fun decode(encoded: String, field: PaginationField): EventCursor {
            return try {
                val decoded = String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
                val cursorType = when (field) {
                    PaginationField.CREATED_AT -> CreatedAtCursor::class.java
                    PaginationField.START_DATE -> StartDateCursor::class.java
                    PaginationField.RECRUITMENT_DEADLINE -> RecruitmentDeadlineCursor::class.java
                    PaginationField.VIEW_COUNT -> ViewCountCursor::class.java
                    PaginationField.ID -> IdCursor::class.java
                    else -> throw IllegalArgumentException("Unsupported pagination field: $field")
                }
                objectMapper.readValue(decoded, cursorType)
            } catch (e: Exception) {
                throw IllegalArgumentException("유효하지 않은 커서입니다: ${e.message}", e)
            }
        }

        /**
         * Event 엔티티로부터 정렬 필드에 맞는 EventCursor 객체를 생성
         *
         * @param event Event 엔티티
         * @param field 정렬 필드
         * @return 생성된 EventCursor 객체
         * @throws IllegalArgumentException 지원하지 않는 정렬 필드이거나 필수 값이 null인 경우
         */
        fun fromEvent(event: Event, field: PaginationField): EventCursor {
            val id = event.id ?: throw IllegalArgumentException("Event ID must not be null")

            return when (field) {
                PaginationField.CREATED_AT -> CreatedAtCursor(
                    createdAt = event.createdAt,
                    id = id
                )
                PaginationField.START_DATE -> StartDateCursor(
                    startAt = event.startAt,
                    id = id
                )
                PaginationField.RECRUITMENT_DEADLINE -> RecruitmentDeadlineCursor(
                    recruitmentEndAt = event.recruitmentEndAt
                        ?: throw IllegalArgumentException("recruitmentEndAt must not be null for RECRUITMENT_DEADLINE sort"),
                    id = id
                )
                PaginationField.VIEW_COUNT -> ViewCountCursor(
                    count = event.view?.count?.toLong() ?: 0L,
                    id = id
                )
                PaginationField.ID -> IdCursor(id = id)
                else -> throw IllegalArgumentException("Unsupported pagination field: $field")
            }
        }
    }
}

/**
 * EventCursor를 Base64로 인코딩된 문자열로 변환
 */
fun EventCursor.encode(): String {
    val json = EventCursor.objectMapper.writeValueAsString(this)
    return Base64.getUrlEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
}
