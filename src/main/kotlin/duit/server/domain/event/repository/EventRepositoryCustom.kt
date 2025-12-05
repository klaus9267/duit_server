package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.dto.EventPaginationParamV2
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventRepositoryCustom {

    fun findEventsForScheduler(status: EventStatus): List<Event>

    fun findEvents(param: EventCursorPaginationParam, currentUserId: Long?): List<Event>
}
