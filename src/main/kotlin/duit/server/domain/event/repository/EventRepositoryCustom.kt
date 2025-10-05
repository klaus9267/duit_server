package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventSearchFilter
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventDate
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import jooq.Tables.EVENTS
import jooq.Tables.HOSTS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
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

        return dsl
            .select(
                EVENTS.ID,
                EVENTS.TITLE,
                EVENTS.START_AT,
                EVENTS.END_AT,
                EVENTS.RECRUITMENT_START_AT,
                EVENTS.RECRUITMENT_END_AT,
                EVENTS.URI,
                EVENTS.THUMBNAIL,
                EVENTS.IS_APPROVED,
                EVENTS.EVENT_TYPE,
                EVENTS.CREATED_AT,
                EVENTS.UPDATED_AT,
                HOSTS.ID,
                HOSTS.NAME,
                HOSTS.THUMBNAIL,
            )
            .from(EVENTS)
            .join(HOSTS).on(HOSTS.ID.eq(EVENTS.HOST_ID))
            .where(EVENTS.IS_APPROVED.eq(true))
            .and(dateCondition)
            .orderBy(eventDate.toField())
            .fetch()
            .map { mapToEvent(it) }
    }

    private fun mapToEvent(record: Record): Event {
        val host = Host(
            id = record.get(HOSTS.ID),
            name = record.get(HOSTS.NAME),
            thumbnail = record.get(HOSTS.THUMBNAIL)
        )

        return Event(
            id = record.get(EVENTS.ID),
            title = record.get(EVENTS.TITLE),
            startAt = record.get(EVENTS.START_AT),
            endAt = record.get(EVENTS.END_AT),
            recruitmentStartAt = record.get(EVENTS.RECRUITMENT_START_AT),
            recruitmentEndAt = record.get(EVENTS.RECRUITMENT_END_AT),
            uri = record.get(EVENTS.URI),
            thumbnail = record.get(EVENTS.THUMBNAIL),
            isApproved = record.get(EVENTS.IS_APPROVED),
            eventType = EventType.valueOf(record.get(EVENTS.EVENT_TYPE).name),
            createdAt = record.get(EVENTS.CREATED_AT),
            updatedAt = record.get(EVENTS.UPDATED_AT),
            host = host
        )
    }

    private fun buildDateCondition(dateField: EventDate): Condition {
        val tomorrow = LocalDateTime.now().plusDays(1)
        val nextDay = tomorrow.plusDays(1)

        return when (dateField) {
            EventDate.START_AT -> {
                EVENTS.START_AT.ge(tomorrow)
                    .and(EVENTS.START_AT.lt(nextDay))
                    .and(EVENTS.START_AT.isNotNull)
            }

            EventDate.RECRUITMENT_START_AT -> {
                EVENTS.RECRUITMENT_START_AT.ge(tomorrow)
                    .and(EVENTS.RECRUITMENT_START_AT.lt(nextDay))
                    .and(EVENTS.RECRUITMENT_START_AT.isNotNull)
            }

            EventDate.RECRUITMENT_END_AT -> {
                EVENTS.RECRUITMENT_END_AT.ge(tomorrow)
                    .and(EVENTS.RECRUITMENT_END_AT.lt(nextDay))
                    .and(EVENTS.RECRUITMENT_END_AT.isNotNull)
            }
        }
    }
}