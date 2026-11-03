package duit.server.domain.event.service

import duit.server.domain.common.dto.pagination.CursorPageInfo
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.common.dto.pagination.EventCursor
import duit.server.domain.common.dto.pagination.encode
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.dto.EventResponseV2
import duit.server.domain.event.repository.EventRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 행사 목록 캐싱 전담 서비스
 *
 * Spring AOP 프록시 제약으로 인해 @Cacheable은 반드시 외부 빈에서 호출되어야 한다.
 * EventService에서 직접 호출하면 프록시를 우회하므로 별도 서비스로 분리.
 *
 * 캐싱 대상: bookmarked=false, searchKeyword=null, field≠VIEW_COUNT (주최 필터 포함 — performance D-026)
 * - events-5m: CREATED_AT / ID 정렬
 * - events-3m: START_DATE / RECRUITMENT_DEADLINE 정렬
 *
 * 행사 조회의 DB 부분은 모두 여기서 짧은 트랜잭션으로 끝낸다. 북마크 표시(Redis)는 호출자가 트랜잭션 밖에서 덧입힌다 —
 * 트랜잭션 안에서 Redis 를 기다리면 그동안 커넥션도 묶이기 때문이다 (performance D-027).
 */
@Service
@Transactional(readOnly = true)
class EventQueryService(
    private val eventRepository: EventRepository
) {

    /**
     * 캐시 가능한 행사 목록 조회 (북마크 정보 미포함)
     *
     * cacheResolver = "eventCacheResolver" 로 필드별 TTL 선택
     * key = param.cacheKey 로 field/status/types/size/cursor 조합 식별
     */
    @Cacheable(cacheResolver = "eventCacheResolver", key = "#param.cacheKey()")
    fun findEvents(param: EventCursorPaginationParam): CursorPageResponse<EventResponseV2> = findPage(param, null)

    /** 캐시를 안 타는 목록 (북마크 필터·검색·조회수 정렬). 북마크 필터에만 사용자 ID 가 쓰인다 */
    fun findEventsUncached(param: EventCursorPaginationParam, currentUserId: Long?): CursorPageResponse<EventResponseV2> =
        findPage(param, currentUserId)

    /** 행사 상세 — 행사·주최·조회수를 SQL 1개로 (performance D-026) */
    fun findEventDetail(eventId: Long): EventResponseV2 =
        eventRepository.findWithHostById(eventId)?.let { EventResponseV2.from(it) }
            ?: throw EntityNotFoundException("Event을(를) 찾을 수 없습니다: $eventId")

    private fun findPage(param: EventCursorPaginationParam, currentUserId: Long?): CursorPageResponse<EventResponseV2> {
        param.cursor?.let { EventCursor.decode(it, param.field) }

        val events = eventRepository.findEvents(param, currentUserId)

        val hasNext = events.size > param.size
        val actualEvents = if (hasNext) events.dropLast(1) else events

        val nextCursor = if (hasNext && actualEvents.isNotEmpty()) {
            EventCursor.fromEvent(actualEvents.last(), param.field).encode()
        } else null

        return CursorPageResponse(
            content = actualEvents.map { EventResponseV2.from(it) },
            pageInfo = CursorPageInfo(
                hasNext = hasNext,
                nextCursor = nextCursor,
                pageSize = actualEvents.size
            )
        )
    }
}
