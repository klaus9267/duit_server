package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventSearchFilter
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventDate
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

        return dsl
            .select()
            .from(Tables.EVENTS)
            .join(Tables.HOSTS).on(Tables.HOSTS.ID.eq(Tables.EVENTS.HOST_ID))
            .where(Tables.EVENTS.IS_APPROVED.eq(true))
            .and(dateCondition)
            .orderBy(eventDate.toField())
            .fetchInto(Event::class.java)
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