package duit.server.domain.event.service

import duit.server.application.scheduler.EventAlarmScheduler
import duit.server.application.security.SecurityUtil
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.event.dto.Event4CalendarRequest
import duit.server.domain.event.dto.EventPaginationParam
import duit.server.domain.event.dto.EventRequest
import duit.server.domain.event.dto.EventResponse
import duit.server.domain.event.entity.Event
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.dto.HostRequest
import duit.server.domain.host.service.HostService
import duit.server.domain.view.service.ViewService
import duit.server.infrastructure.external.discord.DiscordService
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class EventService(
    private val eventRepository: EventRepository,
    private val viewService: ViewService,
    private val securityUtil: SecurityUtil,
    private val discordService: DiscordService,
    private val hostService: HostService,
    private val eventAlarmScheduler: EventAlarmScheduler
) {

    @Transactional
    fun createEvent(eventRequest: EventRequest): Event {
        val host = hostService.findOrCreateHost(
            HostRequest(name = eventRequest.hostName, thumbnail = eventRequest.hostThumbnail)
        )
        val event = eventRepository.save(eventRequest.toEntity(host))
        viewService.createView(event)

        discordService.sendNewEventNotification(event)

        return event
    }

    @Transactional
    fun createEvent4Admin(eventRequest: EventRequest): Event {
        val host = hostService.findOrCreateHost(
            HostRequest(name = eventRequest.hostName, thumbnail = null)
        )
        val event = eventRequest.toEntity(host)
        event.isApproved = true
        val savedEvent = eventRepository.save(event)

        viewService.createView(event)

        return savedEvent
    }

    fun getEvent(eventId: Long): Event =
        eventRepository.findById(eventId)
            .orElseThrow { EntityNotFoundException("이벤트를 찾을 수 없습니다: $eventId") }

    fun getEvents(
        param: EventPaginationParam,
        isApproved: Boolean?,
        isBookmarked: Boolean?,
        includeFinished: Boolean?
    ): PageResponse<EventResponse> {
        val currentUserId = try {
            securityUtil.getCurrentUserId()
        } catch (e: Exception) {
            null // 비로그인 사용자
        }

        val filter = param.toFilter(
            currentUserId = currentUserId,
            isApproved = isApproved ?: true,
            isBookmarked = isBookmarked ?: false,
            includeFinished = includeFinished ?: false
        )
        val pageable = PageRequest.of(
            param.page ?: 0,
            param.size ?: 10
        )
        val events = eventRepository.findWithFilter(filter, pageable)

        // 인증된 사용자의 경우 북마크 정보 포함

        val eventResponses = if (currentUserId != null) {
            val eventIds = events.content.map { it.id!! }
            val bookmarkedEventIds = eventRepository.findBookmarkedEventIds(currentUserId, eventIds).toSet()

            events.content.map { event ->
                EventResponse.from(event, bookmarkedEventIds.contains(event.id))
            }
        } else {
            events.content.map { EventResponse.from(it) }
        }

        return PageResponse(
            content = eventResponses,
            pageInfo = PageInfo.from(events)
        )
    }


    fun getEvents4Calendar(request: Event4CalendarRequest): List<EventResponse> {
        val currentUserId = securityUtil.getCurrentUserId()
        val startDate = LocalDate.of(request.year, request.month, 1)
        val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())
        val events = eventRepository.findEvents4Calendar(currentUserId, startDate, endDate, request.type)

        // 모든 캘린더 이벤트는 북마크된 이벤트이므로 true로 설정
        return events.map { EventResponse.from(it, true) }
    }

    @Transactional
    fun deleteEvent(eventId: Long) = eventRepository.deleteById(eventId)

    /**
     * 행사 승인
     */
    @Transactional
    fun approveEvent(eventId: Long) {
        val event = getEvent(eventId)

        if (event.isApproved) {
            throw IllegalStateException("이미 승인된 행사입니다: $eventId")
        }

        event.isApproved = true
        eventRepository.save(event)
    }
}
