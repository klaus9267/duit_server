package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventPaginationParamV2
import duit.server.domain.event.entity.Event
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventRepositoryCustom {

    fun findEvents(
        param: EventPaginationParamV2,
        currentUserId:Long?,
        pageable: Pageable
    ): Page<Event>
}
