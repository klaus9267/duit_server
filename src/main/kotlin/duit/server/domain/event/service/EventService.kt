package duit.server.domain.event.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.bookmark.service.BookmarkedEventIdsCache
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.event.dto.Event4CalendarRequest
import duit.server.domain.event.dto.EventCreateRequest
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.dto.EventPaginationParam
import duit.server.domain.event.dto.EventResponse
import duit.server.domain.event.dto.EventResponseV2
import duit.server.domain.event.dto.EventUpdateRequest
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.subscription.service.SubscriptionNotificationService
import duit.server.infrastructure.external.discord.DiscordService
import duit.server.infrastructure.external.file.FileStorageService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class EventService(
    private val eventRepository: EventRepository,
    private val securityUtil: SecurityUtil,
    private val discordService: DiscordService,
    private val eventHostResolver: EventHostResolver,
    private val fileStorageService: FileStorageService,
    private val eventQueryService: EventQueryService,
    private val eventCacheEvictService: EventCacheEvictService,
    private val subscriptionNotificationService: SubscriptionNotificationService,
    private val bookmarkedEventIdsCache: BookmarkedEventIdsCache,
) {
    @Transactional
    fun createEvent(
        eventRequest: EventCreateRequest,
        eventThumbnail: MultipartFile?,
        hostThumbnail: MultipartFile?,
        autoApprove: Boolean,
        hostThumbnails: List<MultipartFile> = emptyList(),
    ): EventResponseV2 {
        val hosts = eventHostResolver.resolve(eventRequest, hostThumbnail, hostThumbnails)
        val eventThumbnailUrl = eventThumbnail?.let { fileStorageService.uploadFile(it, "events") }

        val event = eventRequest.toEntity(hosts).apply {
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

        if (autoApprove) {
            subscriptionNotificationService.notifyOnEventApproved(savedEvent)
        } else {
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
            val bookmarkedEventIds = bookmarkedEventIdsCache.get(currentUserId)

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

    /**
     * 이 메서드는 트랜잭션을 열지 않는다(SUPPORTS): DB 조회는 EventQueryService 의 짧은 트랜잭션에서 끝내고, 캐시(Redis) 조회·저장과
     * 북마크 덧입히기는 커넥션을 반납한 뒤에 한다. Redis 가 멈추면 명령마다 타임아웃까지 기다리는데, 그동안 커넥션을 쥐고 있으면
     * 풀(10)이 고갈돼 Redis 를 안 쓰는 API 까지 멈춘다 (performance D-027).
     * NOT_SUPPORTED 가 아닌 이유: 호출자에 트랜잭션이 있으면(테스트) 끊지 않고 합류해야 그 트랜잭션의 데이터가 보인다.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    fun getEvents(param: EventCursorPaginationParam): CursorPageResponse<EventResponseV2> {
        val currentUserId = securityUtil.getCurrentUserIdOrNull()

        val page = if (isCacheable(param)) {
            eventQueryService.findEvents(param) // Spring @Cacheable 경유
        } else {
            eventQueryService.findEventsUncached(param, currentUserId)
        }
        return overlayBookmarks(page, currentUserId)
    }

    // 주최 필터(hostId)도 캐시한다 — 행사 상세의 "이 주최 다른 행사"로 트래픽의 ~10% (performance D-026)
    private fun isCacheable(param: EventCursorPaginationParam): Boolean =
        !param.bookmarked
            && param.searchKeyword == null
            && param.field != PaginationField.VIEW_COUNT

    /**
     * 목록 응답에 현재 사용자의 북마크 정보를 오버레이.
     * 북마크 ID 도 캐시에서 읽으므로 로그인 사용자의 캐시 히트도 DB 커넥션을 잡지 않는다 (performance D-020)
     */
    private fun overlayBookmarks(
        cached: CursorPageResponse<EventResponseV2>,
        currentUserId: Long?
    ): CursorPageResponse<EventResponseV2> {
        if (currentUserId == null || cached.content.isEmpty()) return cached

        val bookmarkedEventIds = bookmarkedEventIdsCache.get(currentUserId)

        return CursorPageResponse(
            content = cached.content.map { it.copy(isBookmarked = it.id in bookmarkedEventIds) },
            pageInfo = cached.pageInfo
        )
    }

    /** 목록과 같은 이유로 트랜잭션을 열지 않는다: 행사를 읽고 커넥션을 반납한 뒤 북마크 ID 캐시를 본다 (performance D-027) */
    @Transactional(propagation = Propagation.SUPPORTS)
    fun getEventDetail(eventId: Long): EventResponseV2 {
        val detail = eventQueryService.findEventDetail(eventId)
        val currentUserId = securityUtil.getCurrentUserIdOrNull() ?: return detail

        return detail.copy(isBookmarked = detail.id in bookmarkedEventIdsCache.get(currentUserId))
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
        val wasPending = event.status == EventStatus.PENDING

        event.updateStatus(LocalDateTime.now())

        // 캐시 무효화
        eventCacheEvictService.evictAll()

        if (wasPending && event.status != EventStatus.PENDING) {
            subscriptionNotificationService.notifyOnEventApproved(event)
        }
    }

    @Transactional
    fun updateEvent(
        eventId: Long,
        updateRequest: EventUpdateRequest,
        eventThumbnail: MultipartFile?,
        hostThumbnail: MultipartFile?,
        hostThumbnails: List<MultipartFile> = emptyList(),
    ): EventResponse = getEvent(eventId).let { event ->
        require(updateRequest.hosts == null || !updateRequest.deleteHostThumbnail) {
            "hosts 사용 시 deleteHostThumbnail 은 지원하지 않습니다. 주최 로고 삭제는 PUT /api/v1/hosts/{hostId} 를 사용하세요"
        }
        val hosts = eventHostResolver.resolve(
            source = updateRequest,
            hostThumbnail = hostThumbnail.takeUnless { updateRequest.deleteHostThumbnail },
            hostThumbnails = hostThumbnails,
            legacyThumbnailFallback = event.hosts.first().thumbnail.takeUnless { updateRequest.deleteHostThumbnail },
        )

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

        event.update(updateRequest, eventThumbnailUrl, hosts)
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
