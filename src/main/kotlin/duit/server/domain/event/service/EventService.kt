package duit.server.domain.event.service

import duit.server.application.scheduler.EventAlarmScheduler
import duit.server.application.security.SecurityUtil
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.event.dto.*
import duit.server.domain.event.entity.Event
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.dto.HostRequest
import duit.server.domain.host.service.HostService
import duit.server.domain.view.service.ViewService
import duit.server.infrastructure.external.discord.DiscordService
import duit.server.infrastructure.external.file.FileStorageService
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class EventService(
    private val eventRepository: EventRepository,
    private val viewService: ViewService,
    private val securityUtil: SecurityUtil,
    private val discordService: DiscordService,
    private val hostService: HostService,
    private val eventAlarmScheduler: EventAlarmScheduler,
    private val fileStorageService: FileStorageService
) {

    @Transactional
    fun createEventFromGoogleForm(eventRequestFromGoogleForm: EventRequestFromGoogleForm): Event {
        val host = hostService.findOrCreateHost(
            HostRequest(name = eventRequestFromGoogleForm.hostName, thumbnail = eventRequestFromGoogleForm.hostThumbnail)
        )
        val event = eventRepository.save(eventRequestFromGoogleForm.toEntity(host))
        viewService.createView(event)

        discordService.sendNewEventNotification(event)

        return event
    }

    @Transactional
    fun createEvent(
        eventRequest: EventCreateRequest,
        eventThumbnail: MultipartFile?,
        hostThumbnail: MultipartFile?,
        isApproved: Boolean
    ): EventResponse {
        val eventThumbnailUrl = eventThumbnail?.let { fileStorageService.uploadFile(it, "events") }
        val hostThumbnailUrl = hostThumbnail?.let { fileStorageService.uploadFile(it, "hosts") }

        val host = hostService.findOrCreateHost(
            HostRequest(name = eventRequest.hostName, thumbnail = hostThumbnailUrl)
        )

        val event = eventRequest.toEntity(host).apply {
            thumbnail = eventThumbnailUrl
            this.isApproved = isApproved
        }

        val savedEvent = eventRepository.save(event)
        viewService.createView(savedEvent)

        return EventResponse.from(savedEvent,false)
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
        val start = LocalDateTime.of(request.year, request.month, 1, 0, 0)
        val end = start.plusMonths(1).minusNanos(1)
        val events = eventRepository.findEvents4Calendar(currentUserId, start, end, request.type)

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

    @Transactional
    fun updateEvent(
        eventId: Long,
        updateRequest: EventUpdateRequest,
        eventThumbnail: MultipartFile?,
        hostThumbnail: MultipartFile?
    ): EventResponse {
        val event = getEvent(eventId)

        // 기존 썸네일 삭제 (새 파일이 업로드되는 경우에만)
        if (eventThumbnail != null && event.thumbnail != null) {
            fileStorageService.deleteFile(event.thumbnail!!)
        }

        // 새 파일 업로드
        val eventThumbnailUrl = eventThumbnail?.let { fileStorageService.uploadFile(it, "events") }
        val hostThumbnailUrl = hostThumbnail?.let { fileStorageService.uploadFile(it, "hosts") }

        // Host 업데이트 또는 생성
        val host = hostService.findOrCreateHost(
            HostRequest(name = updateRequest.hostName, thumbnail = hostThumbnailUrl)
        )

        // Event 필드 업데이트
        event.title = updateRequest.title
        event.startAt = updateRequest.startAt
        event.endAt = updateRequest.endAt
        event.recruitmentStartAt = updateRequest.recruitmentStartAt
        event.recruitmentEndAt = updateRequest.recruitmentEndAt
        event.uri = updateRequest.uri
        event.host = host

        // 썸네일 업데이트 (새 파일이 있는 경우에만)
        if (eventThumbnailUrl != null) {
            event.thumbnail = eventThumbnailUrl
        }

        val savedEvent = eventRepository.save(event)
        return EventResponse.from(savedEvent, false)
    }
}
