package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventAlarmInfo
import duit.server.domain.event.dto.EventSearchFilter
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventDate
import jooq.Tables.EVENTS
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

    /**
     * 알림 스케줄링에 필요한 최소한의 이벤트 정보 조회 (ID + 대상 시간)
     */
    fun findEventAlarmInfoByDateField(eventDate: EventDate): List<EventAlarmInfo> {
        val dateCondition = buildDateCondition(eventDate)
        val targetField = eventDate.toField()

        return dsl
            .select(
                EVENTS.ID,
                targetField
            )
            .from(EVENTS)
            .where(EVENTS.IS_APPROVED.eq(true))
            .and(dateCondition)
            .orderBy(targetField)
            .fetch()
            .map { record ->
                EventAlarmInfo(
                    id = record.get(EVENTS.ID),
                    targetDateTime = record.get(targetField)
                )
            }
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