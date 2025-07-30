package duit.server.domain.event.service

import duit.server.application.controller.dto.event.EventRequest
import duit.server.application.controller.dto.event.EventResponse
import duit.server.application.controller.dto.pagination.PageInfo
import duit.server.application.controller.dto.pagination.PageResponse
import duit.server.application.controller.dto.pagination.PaginationParam
import duit.server.domain.event.repository.EventRepository
import org.springframework.stereotype.Service

@Service
class EventService(private val eventRepository: EventRepository) {

    fun createEvent(eventRequest: EventRequest) = eventRepository.save(eventRequest.toEntity())

    fun getPendingEvents(param: PaginationParam): PageResponse<EventResponse> {
        val events = eventRepository.findByIsApproved(false, param.toPageable())
            .map { EventResponse.from(it) }

        return PageResponse(
            content = events.content,
            pageInfo = PageInfo.from(events)
        )
    }
}