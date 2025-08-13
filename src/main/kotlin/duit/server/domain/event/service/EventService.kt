package duit.server.domain.event.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.event.dto.Event4CalendarRequest
import duit.server.domain.event.dto.EventPaginationParam
import duit.server.domain.event.dto.EventRequest
import duit.server.domain.event.dto.EventResponse
import duit.server.domain.event.entity.Event
import jakarta.persistence.EntityNotFoundException
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.view.service.ViewService
import duit.server.infrastructure.external.discord.DiscordService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class EventService(
    private val eventRepository: EventRepository,
    private val viewService: ViewService,
    private val securityUtil: SecurityUtil,
    private val discordService: DiscordService
) {

    @Transactional
    fun createEvent(eventRequest: EventRequest): Event {
        val event = eventRepository.save(eventRequest.toEntity())
        viewService.createView(event)
        
        discordService.sendNewEventNotification(event)
        
        return event
    }

    fun getEvent(eventId: Long): Event =
        eventRepository.findById(eventId)
            .orElseThrow { EntityNotFoundException("이벤트를 찾을 수 없습니다: $eventId") }

    fun getEvents(param: EventPaginationParam, isApproved: Boolean?): PageResponse<EventResponse> {
        val events =
            eventRepository.findWithFilter(param.type, param.hostId, isApproved ?: true, param.toPageable())
                .map { EventResponse.from(it) }

        return PageResponse(
            content = events.content,
            pageInfo = PageInfo.from(events)
        )
    }

    fun getEvents4Calendar(request: Event4CalendarRequest): List<EventResponse> {
        val currentUserId = securityUtil.getCurrentUserId()
        val startDate = LocalDate.of(request.year, request.month, 1)
        val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())
        return eventRepository.findEvents4Calendar(currentUserId, startDate, endDate, request.type)
            .map { EventResponse.from(it) }
    }
}
