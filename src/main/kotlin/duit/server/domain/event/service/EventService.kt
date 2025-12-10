package duit.server.domain.event.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.common.dto.pagination.*
import duit.server.domain.event.dto.*
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
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
    private val fileStorageService: FileStorageService
) {
    @Transactional
    fun createEvent(
        eventRequest: EventCreateRequest,
        eventThumbnail: MultipartFile?,
        hostThumbnail: MultipartFile?,
        isApproved: Boolean
    ): EventResponse {
        val eventThumbnailUrl = eventThumbnail?.let { fileStorageService.uploadFile(it, "events") }
        val hostThumbnailUrl = hostThumbnail?.let { fileStorageService.uploadFile(it, "hosts") }

        val host = when {
            eventRequest.hostId != null -> hostService.getHost(eventRequest.hostId)
            eventRequest.hostName != null -> hostService.findOrCreateHost(
                HostRequest(name = eventRequest.hostName, thumbnail = hostThumbnailUrl)
            )

            else -> throw IllegalArgumentException("hostId 또는 hostName 중 하나는 필수입니다")
        }

        val event = eventRequest.toEntity(host).apply {
            thumbnail = eventThumbnailUrl
            this.isApproved = isApproved

            if (isApproved) {
                this.status = EventStatus.RECRUITMENT_WAITING
                this.statusGroup = EventStatusGroup.ACTIVE
            } else {
                this.status = EventStatus.PENDING
                this.statusGroup = EventStatusGroup.PENDING
            }
        }

        return eventRepository.save(event).also { viewService.createView(it) }
            .let {
                if (!isApproved) {
                    discordService.sendNewEventNotification(it)
                }
                EventResponse.from(it, false)
            }
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
        val currentUserId = securityUtil.getCurrentUserIdOrNull()

        val filter = param.toFilter(
            currentUserId = currentUserId,
            isApproved = isApproved ?: true,
            isBookmarked = isBookmarked ?: false,
            includeFinished = includeFinished ?: false
        )
        val pageable = PageRequest.of(param.page, param.size)
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

    fun getEvents(param: EventCursorPaginationParam): CursorPageResponse<EventResponseV2> {
        val currentUserId = if (param.bookmarked) securityUtil.getCurrentUserId() else null

        // size + 1 조회 (hasNext 감지용)
        val events = eventRepository.findEvents(param, currentUserId)

        // hasNext 감지 및 실제 이벤트 분리
        val hasNext = events.size > param.size
        val actualEvents = if (hasNext) events.dropLast(1) else events

        // nextCursor 생성 (hasNext가 true이고 actualEvents가 있을 때만)
        val nextCursor = if (hasNext && actualEvents.isNotEmpty()) {
            EventCursor.fromEvent(actualEvents.last(), param.field).encode()
        } else null

        // 북마크 정보 로드 (로그인한 경우)
        val eventResponses = if (currentUserId != null && actualEvents.isNotEmpty()) {
            val eventIds = actualEvents.map { it.id!! }
            val bookmarkedEventIds = eventRepository.findBookmarkedEventIds(currentUserId, eventIds).toSet()

            actualEvents.map { event ->
                EventResponseV2.from(event, bookmarkedEventIds.contains(event.id))
            }
        } else {
            actualEvents.map { EventResponseV2.from(it) }
        }

        return CursorPageResponse(
            content = eventResponses,
            pageInfo = CursorPageInfo(
                hasNext = hasNext,
                nextCursor = nextCursor,
                pageSize = actualEvents.size
            )
        )
    }

    fun countActiveEvents(): Long {
        return eventRepository.countActiveEvents()
    }

    fun getEvents4Calendar(request: Event4CalendarRequest): List<EventResponse> {
        val currentUserId = securityUtil.getCurrentUserId()
        val start = LocalDateTime.of(request.year, request.month, 1, 0, 0)
        val end = start.plusMonths(1).minusNanos(1)
        val events = eventRepository.findEvents4Calendar(currentUserId, start, end, request.type)

        return events.map { EventResponse.from(it, true) }
    }

    @Transactional
    fun updateStatus(eventId: Long, newStatus: EventStatus? = null) {
        val event = getEvent(eventId)

        if (newStatus == null) {
            event.isApproved = true
            event.updateStatus()
        } else {
            event.updateStatus(newStatus)
        }
    }

    @Transactional
    fun updateEvent(
        eventId: Long,
        updateRequest: EventUpdateRequest,
        eventThumbnail: MultipartFile?,
        hostThumbnail: MultipartFile?
    ): EventResponse = getEvent(eventId).let { event ->
        // 행사 썸네일 처리
        val eventThumbnailUrl = when {
            updateRequest.deleteEventThumbnail -> {
                event.thumbnail?.let { fileStorageService.deleteFile(it) }
                null
            }

            eventThumbnail != null -> {
                event.thumbnail?.let { fileStorageService.deleteFile(it) }
                fileStorageService.uploadFile(eventThumbnail, "events")
            }

            else -> event.thumbnail
        }

        // Host 처리: hostId가 있으면 기존 사용, 없으면 생성/수정
        val host = when {
            updateRequest.hostId != null -> hostService.getHost(updateRequest.hostId)
            updateRequest.hostName != null -> {
                // Host 썸네일 처리
                val hostThumbnailUrl = when {
                    updateRequest.deleteHostThumbnail -> null
                    hostThumbnail != null -> fileStorageService.uploadFile(hostThumbnail, "hosts")
                    else -> event.host.thumbnail
                }
                hostService.findOrCreateHost(
                    HostRequest(name = updateRequest.hostName, thumbnail = hostThumbnailUrl)
                )
            }

            else -> throw IllegalArgumentException("hostId 또는 hostName 중 하나는 필수입니다")
        }

        event.update(updateRequest, eventThumbnailUrl, host)
        EventResponse.from(eventRepository.save(event), false)
    }

    @Transactional
    fun deleteEvent(eventId: Long) = eventRepository.deleteById(eventId)

    @Transactional
    fun deleteEvents(eventIds: List<Long>) {
        eventRepository.findAllByIdInAndThumbnailNotNull(eventIds)
            .forEach {
                fileStorageService.deleteFile(it.thumbnail!!)
            }
        eventRepository.deleteAllById(eventIds)
    }


}