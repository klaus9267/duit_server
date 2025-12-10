package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import java.time.LocalDateTime

interface EventRepositoryCustom {

    fun findEventsForScheduler(status: EventStatus): List<Event>

    fun findEvents(param: EventCursorPaginationParam, currentUserId: Long?): List<Event>

    fun findEventsWithIncorrectStatus(now: LocalDateTime): List<Event>
}
