package duit.server.domain.event.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.common.dto.pagination.CursorPageInfo
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.event.dto.Event4CalendarRequest
import duit.server.domain.event.dto.EventCreateRequest
import duit.server.domain.common.dto.pagination.EventCursor
import duit.server.domain.common.dto.pagination.encode
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.dto.EventPaginationParam
import duit.server.domain.event.dto.EventResponse
import duit.server.domain.event.dto.EventResponseV2
import duit.server.domain.event.dto.EventUpdateRequest
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.dto.HostRequest
import duit.server.domain.host.service.HostService
import duit.server.infrastructure.external.discord.DiscordService
import duit.server.infrastructure.external.file.FileStorageService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class EventService(
    private val eventRepository: EventRepository,
    private val securityUtil: SecurityUtil,
    private val discordService: DiscordService,
    private val hostService: HostService,
    private val fileStorageService: FileStorageService,
    private val eventQueryService: EventQueryService,
    private val eventCacheEvictService: EventCacheEvictService
) {
    @Transactional
    fun createEvent(
        eventRequest: EventCreateRequest,
        eventThumbnail: MultipartFile?,
        hostThumbnail: MultipartFile?,
        autoApprove: Boolean
    ): EventResponseV2 {
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

            if (autoApprove) {
                this.status = EventStatus.RECRUITMENT_WAITING
                this.statusGroup = EventStatusGroup.ACTIVE
            } else {
                this.status = EventStatus.PENDING
                this.statusGroup = EventStatusGroup.PENDING
            }

        }

        val savedEvent = eventRepository.save(event)

        if (!autoApprove) {
            discordService.sendNewEventNotification(savedEvent)
        }

        // 캐시 무효화
        eventCacheEvictService.evictAll()

        return EventResponseV2.from(savedEvent, false)
    }

    fun getEvent(eventId: Long): Event = eventRepository.findByIdOrThrow(eventId)

    fun getEvents(
        param: EventPaginationParam,
        isBookmarked: Boolean?,
        includeFinished: Boolean?
    ): PageResponse<EventResponse> {
        val currentUserId = securityUtil.getCurrentUserIdOrNull()

        val filter = param.toFilter(
            currentUserId = currentUserId,
            excludePending = true,
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
        val currentUserId = securityUtil.getCurrentUserIdOrNull()

        return if (isCacheable(param)) {
            // Spring @Cacheable 경유: EventQueryService.findEvents → 북마크 오버레이
            overlayBookmarks(eventQueryService.findEvents(param), currentUserId)
        } else {
            // 캐싱 불가 → DB 직접 조회
            getEventsFromDb(param, currentUserId)
        }
    }

    private fun isCacheable(param: EventCursorPaginationParam): Boolean =
        !param.bookmarked
            && param.searchKeyword == null
            && param.hostId == null
            && param.field != PaginationField.VIEW_COUNT

    private fun getEventsFromDb(
        param: EventCursorPaginationParam,
        currentUserId: Long?
    ): CursorPageResponse<EventResponseV2> {
        param.cursor?.let { EventCursor.decode(it, param.field) }

        val events = eventRepository.findEvents(param, currentUserId)

        val hasNext = events.size > param.size
        val actualEvents = if (hasNext) events.dropLast(1) else events

        val nextCursor = if (hasNext && actualEvents.isNotEmpty()) {
            EventCursor.fromEvent(actualEvents.last(), param.field).encode()
        } else null

        val eventResponses = if (currentUserId != null && actualEvents.isNotEmpty()) {
            val eventIds = actualEvents.map { it.id!! }
            val bookmarkedEventIds = eventRepository.findBookmarkedEventIds(currentUserId, eventIds).toSet()
            actualEvents.map { event ->
                EventResponseV2.from(event, event.id in bookmarkedEventIds)
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

    /**
     * 캐시된 응답에 현재 사용자의 북마크 정보를 오버레이
     */
    private fun overlayBookmarks(
        cached: CursorPageResponse<EventResponseV2>,
        currentUserId: Long?
    ): CursorPageResponse<EventResponseV2> {
        if (currentUserId == null || cached.content.isEmpty()) return cached

        val eventIds = cached.content.map { it.id }
        val bookmarkedEventIds = eventRepository.findBookmarkedEventIds(currentUserId, eventIds).toSet()

        return CursorPageResponse(
            content = cached.content.map { it.copy(isBookmarked = it.id in bookmarkedEventIds) },
            pageInfo = cached.pageInfo
        )
    }

    fun getEventDetail(eventId: Long): EventResponseV2 {
        val event = getEvent(eventId)
        val currentUserId = securityUtil.getCurrentUserIdOrNull()

        val isBookmarked = if (currentUserId != null) {
            eventRepository.findBookmarkedEventIds(currentUserId, listOf(event.id!!)).isNotEmpty()
        } else {
            false
        }

        return EventResponseV2.from(event, isBookmarked)
    }

    fun countActiveEvents(): Long = eventRepository.countActiveEvents()

    fun getEvents4Calendar(request: Event4CalendarRequest): List<EventResponseV2> {
        val currentUserId = securityUtil.getCurrentUserId()
        val start = LocalDateTime.of(request.year, request.month, 1, 0, 0)
        val end = start.plusMonths(1).minusNanos(1)
        val events = eventRepository.findEvents4Calendar(currentUserId, start, end, request.type)

        return events.map { EventResponseV2.from(it, true) }
    }

    @Transactional
    fun updateStatus(eventId: Long) {
        val event = getEvent(eventId)

        event.updateStatus(LocalDateTime.now())

        // 캐시 무효화
        eventCacheEvictService.evictAll()
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
        val saved = eventRepository.save(event)

        // 캐시 무효화
        eventCacheEvictService.evictAll()

        EventResponse.from(saved, false)
    }

    @Transactional
    fun deleteEvents(eventIds: List<Long>) {
        eventRepository.findAllByIdInAndThumbnailNotNull(eventIds)
            .forEach {
                fileStorageService.deleteFile(it.thumbnail!!)
            }
        eventRepository.deleteAllById(eventIds)

        // 캐시 무효화
        eventCacheEvictService.evictAll()
    }
}
