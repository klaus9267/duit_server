package duit.server.domain.event.service

import duit.server.domain.common.dto.pagination.CursorPageInfo
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.common.dto.pagination.EventCursor
import duit.server.domain.common.dto.pagination.encode
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.dto.EventResponseV2
import duit.server.domain.event.repository.EventRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 행사 목록 캐싱 전담 서비스
 *
 * Spring AOP 프록시 제약으로 인해 @Cacheable은 반드시 외부 빈에서 호출되어야 한다.
 * EventService에서 직접 호출하면 프록시를 우회하므로 별도 서비스로 분리.
 *
 * 캐싱 대상: bookmarked=false, searchKeyword=null, hostId=null, field≠VIEW_COUNT
 * - events-5m: CREATED_AT / ID 정렬
 * - events-3m: START_DATE / RECRUITMENT_DEADLINE 정렬
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
    fun findEvents(param: EventCursorPaginationParam): CursorPageResponse<EventResponseV2> {
        param.cursor?.let { EventCursor.decode(it, param.field) }

        val events = eventRepository.findEvents(param, null)

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
