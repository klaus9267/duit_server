package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventSearchFilter
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventDate
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import jooq.Tables
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EventRepositoryCustom(
    private val dsl: DSLContext
) {

    // 기존 복잡한 쿼리
    fun findEventsWithFilter(
        filter: EventSearchFilter,
        pageable: Pageable
    ): Page<Event> {
        // 기존 구현 유지 (생략)
        return PageImpl(emptyList(), pageable, 0)
    }

    fun findEventsByDateField(eventDate: EventDate): List<Event> {
        val dateCondition = buildDateCondition(eventDate)
        val e = Tables.EVENTS
        val h = Tables.HOSTS

        return dsl
            .select(
                e.ID,
                e.TITLE,
                e.START_AT,
                e.END_AT,
                e.RECRUITMENT_START_AT,
                e.RECRUITMENT_END_AT,
                e.URI,
                e.THUMBNAIL,
                e.IS_APPROVED,
                e.EVENT_TYPE,
                e.CREATED_AT,
                e.UPDATED_AT,
                // Host 정보
                h.ID,
                h.NAME,
                h.THUMBNAIL
            )
            .from(e)
            .join(h).on(h.ID.eq(e.HOST_ID))
            .where(e.IS_APPROVED.eq(true))
            .and(dateCondition)
            .orderBy(eventDate.toField())
            .fetch()
            .map { record ->
                Event(
                    id = record.get(e.ID),
                    title = record.get(e.TITLE)!!,
                    startAt = record.get(e.START_AT)!!,
                    endAt = record.get(e.END_AT),
                    recruitmentStartAt = record.get(e.RECRUITMENT_START_AT),
                    recruitmentEndAt = record.get(e.RECRUITMENT_END_AT),
                    uri = record.get(e.URI)!!,
                    thumbnail = record.get(e.THUMBNAIL),
                    isApproved = record.get(e.IS_APPROVED) ?: false,
                    eventType = EventType.valueOf(record.get(e.EVENT_TYPE)!!),
                    createdAt = record.get(e.CREATED_AT) ?: LocalDateTime.now(),
                    updatedAt = record.get(e.UPDATED_AT) ?: LocalDateTime.now(),
                    host = Host(
                        id = record.get(h.ID),
                        name = record.get(h.NAME)!!,
                        thumbnail = record.get(h.THUMBNAIL)
                    )
                )
            }
    }

    private fun buildDateCondition(dateField: EventDate): Condition {
        val tomorrow = LocalDateTime.now().plusDays(1)
        val nextDay = tomorrow.plusDays(1)

        return when (dateField) {
            EventDate.START_AT -> {
                Tables.EVENTS.START_AT.ge(tomorrow)
                    .and(Tables.EVENTS.START_AT.lt(nextDay))
                    .and(Tables.EVENTS.START_AT.isNotNull)
            }

            EventDate.RECRUITMENT_START_AT -> {
                Tables.EVENTS.RECRUITMENT_START_AT.ge(tomorrow)
                    .and(Tables.EVENTS.RECRUITMENT_START_AT.lt(nextDay))
                    .and(Tables.EVENTS.RECRUITMENT_START_AT.isNotNull)
            }

            EventDate.RECRUITMENT_END_AT -> {
                Tables.EVENTS.RECRUITMENT_END_AT.ge(tomorrow)
                    .and(Tables.EVENTS.RECRUITMENT_END_AT.lt(nextDay))
                    .and(Tables.EVENTS.RECRUITMENT_END_AT.isNotNull)
            }
        }
    }
}