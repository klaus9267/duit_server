package duit.server.domain.event.service

import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.event.dto.Event4CalendarRequest
import duit.server.domain.event.dto.EventPaginationParam
import duit.server.domain.event.dto.EventRequest
import duit.server.domain.event.dto.EventResponse
import duit.server.domain.event.entity.Event
import duit.server.domain.event.exception.EventNotFoundException
import duit.server.domain.event.repository.EventRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EventService(
    private val eventRepository: EventRepository
) {

    fun createEvent(eventRequest: EventRequest) = eventRepository.save(eventRequest.toEntity())

    fun getEvent(eventId: Long) =
        eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException(eventId) }

    fun getEvents(param: EventPaginationParam, isApproved: Boolean?): PageResponse<EventResponse> {
        val events =
            eventRepository.findWithFilter(param.type, param.hostId, isApproved ?: true, param.toPageable())
                .map { EventResponse.from(it) }

        return PageResponse(
            content = events.content,
            pageInfo = PageInfo.from(events)
        )
    }

    fun getEvents4Calendar(request: Event4CalendarRequest): List<Event> {
        val startDate = LocalDate.of(request.year, request.month, 1)
        val endDate = LocalDate.of(request.year, request.month, startDate.dayOfMonth)
        return eventRepository.findEvents4Calendar(startDate, endDate, request.type)
    }
}
