package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventPaginationParam
import duit.server.domain.event.dto.EventSearchFilter
import duit.server.domain.event.entity.Event
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventRepositoryCustom {

    fun findEvents(
        filter: EventSearchFilter,
        pageable: Pageable
    ): Page<Event>
}
