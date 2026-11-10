package duit.server.domain.event.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import duit.server.domain.bookmark.entity.QBookmark
import duit.server.domain.common.dto.pagination.EventCursor
import duit.server.domain.common.dto.pagination.PaginationField.*
import duit.server.domain.event.dto.DateRangeFilter
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.entity.*
import duit.server.domain.view.entity.QView
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class EventRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val entityManager: EntityManager
) : EventRepositoryCustom {

    private val event = QEvent.event
    private val eventHost = QEventHost.eventHost
    private val view = QView.view
    private val bookmark = QBookmark.bookmark

    private fun inEventTypes(eventTypes: List<EventType>?): BooleanExpression? {
        return if (!eventTypes.isNullOrEmpty()) {
            event.eventType.`in`(eventTypes)
        } else null
    }

    override fun findEventsForScheduler(status: EventStatus): List<Event> {
        val today = LocalDate.now().atStartOfDay()
        val tomorrow = today.plusDays(1)

        val dateCondition: BooleanExpression? = when (status) {
            EventStatus.RECRUITMENT_WAITING -> event.recruitmentStartAt.between(today, tomorrow)
            EventStatus.RECRUITING ->
                event.recruitmentEndAt.isNotNull
                    .and(event.recruitmentEndAt.between(today, tomorrow))
                    .or(event.startAt.between(today, tomorrow))

            EventStatus.EVENT_WAITING -> event.startAt.between(today, tomorrow)
            EventStatus.ACTIVE -> {
                val endAtCondition = event.endAt.isNotNull
                    .and(event.endAt.between(today, tomorrow))
                val endAtPlusOneDayCondition = event.endAt.isNull.and(
                    event.startAt.loe(today)
                )
                endAtCondition.or(endAtPlusOneDayCondition)
            }

            else -> throw IllegalArgumentException("Invalid status: $status")
        }

        return queryFactory
            .selectFrom(event)
            .where(
                event.status.ne(EventStatus.PENDING),
                event.status.eq(status),
                dateCondition
            )
            .fetch()
    }

    override fun findEvents(
        param: EventCursorPaginationParam,
        currentUserId: Long?
    ): List<Event> {
        // 커서 디코딩
        val cursor = param.cursor?.let { EventCursor.decode(it, param.field) }

        // VIEW_COUNT는 Native SQL 사용
        if (param.field == VIEW_COUNT) {
            return fetchEventsByViewCountWithCursor(param, cursor, currentUserId)
        }

        // QueryDSL 쿼리 빌드 — hosts(컬렉션)는 limit 과 함께 fetch join 하면 메모리 페이징이 되므로 batch fetch 로 로딩
        val query = queryFactory
            .selectFrom(event)
            .leftJoin(event.view(), view).fetchJoin()
            .buildWhere(param, currentUserId, cursor)
            .buildOrderBy(param)

        // size + 1 조회 (hasNext 감지용)
        return query.limit(param.size.toLong() + 1).fetch()
    }

    private fun <T> JPAQuery<T>.buildWhere(
        param: EventCursorPaginationParam,
        currentUserId: Long?,
        cursor: EventCursor?
    ): JPAQuery<T> {
        if (param.bookmarked && currentUserId != null) {
            this.join(event.bookmarks, bookmark)
                .on(bookmark.user().id.eq(currentUserId))
        }

        val conditions = mutableListOf<BooleanExpression?>(hasAnyHost())

        // status 또는 statusGroup 필터
        if (param.status != null) {
            conditions.add(event.status.eq(param.status))
        } else {
            conditions.add(event.statusGroup.eq(param.statusGroup))
        }

        // eventType 필터
        conditions.add(inEventTypes(param.types))

        // 검색 키워드 필터 (행사 제목 또는 주최자명 — 여러 주최 중 하나라도 일치)
        param.searchKeyword?.let { keyword ->
            conditions.add(
                event.title.containsIgnoreCase(keyword)
                    .or(hasHostMatching(eventHost.host().name.containsIgnoreCase(keyword)))
            )
        }

        // 주최자 ID 필터 (여러 주최 중 하나라도 일치)
        param.hostId?.let { hostId ->
            conditions.add(hasHostMatching(eventHost.host().id.eq(hostId)))
        }

        conditions.add(overlapsEventPeriod(param.eventDateRange()))
        conditions.add(overlapsRecruitmentPeriod(param.recruitmentDateRange()))

        val now = LocalDateTime.now()
        val isFinished = param.status == EventStatus.FINISHED || param.statusGroup == EventStatusGroup.FINISHED

        // 정렬 필드별 null 제거
        when (param.field) {
            RECRUITMENT_DEADLINE -> {
                conditions.add(event.recruitmentEndAt.isNotNull)
                if (isFinished) {
                    conditions.add(event.recruitmentEndAt.lt(now))
                } else {
                    conditions.add(event.recruitmentEndAt.goe(now))
                }
            }

            START_DATE -> {
                conditions.add(event.startAt.isNotNull)
                if (isFinished) {
                    conditions.add(event.startAt.lt(now))
                } else {
                    conditions.add(event.startAt.goe(now))
                }
            }

            else -> Unit
        }

        // 커서 조건 추가
        conditions.add(buildCursorCondition(cursor, param))

        return this.where(*conditions.filterNotNull().toTypedArray())
    }

    // 행사 기간 [startAt, endAt ?: startAt] 이 범위와 겹치는 행사. 인덱스를 탈 수 있게 COALESCE 대신 OR 로 푼다
    private fun overlapsEventPeriod(range: DateRangeFilter): BooleanExpression? =
        listOfNotNull(
            range.endExclusive?.let { event.startAt.lt(it) },
            range.startInclusive?.let { event.endAt.goe(it).or(event.endAt.isNull.and(event.startAt.goe(it))) },
        ).reduceOrNull(BooleanExpression::and)

    // 모집 시작이 없으면 시작은 열려 있고, 마감이 없으면 행사 시작을 마감으로 본다.
    private fun overlapsRecruitmentPeriod(range: DateRangeFilter): BooleanExpression? {
        if (range.isUnbounded) return null
        val start = event.recruitmentStartAt
        val end = event.recruitmentEndAt
        return listOfNotNull(
            range.endExclusive?.let { start.isNull.or(start.lt(it)) },
            range.startInclusive?.let { end.goe(it).or(end.isNull.and(event.startAt.goe(it))) },
        ).reduce(BooleanExpression::and)
    }

    // 주최 없는 행사 제외 (hosts inner join 시절과 동일)
    private fun hasAnyHost(): BooleanExpression =
        JPAExpressions.selectOne().from(eventHost).where(eventHost.event().eq(event)).exists()

    private fun hasHostMatching(condition: BooleanExpression): BooleanExpression =
        JPAExpressions.selectOne()
            .from(eventHost)
            .where(eventHost.event().eq(event), condition)
            .exists()

    private fun buildCursorCondition(cursor: EventCursor?, param: EventCursorPaginationParam): BooleanExpression? {
        if (cursor == null) return null

        return when (cursor) {
            is EventCursor.CreatedAtCursor -> {
                event.createdAt.lt(cursor.createdAt)
                    .or(event.createdAt.eq(cursor.createdAt).and(event.id.lt(cursor.id)))
            }

            is EventCursor.StartDateCursor -> {
                val isFinished = param.status == EventStatus.FINISHED || param.statusGroup == EventStatusGroup.FINISHED

                if (isFinished) {
                    event.startAt.lt(cursor.startAt)
                        .or(
                            event.startAt.eq(cursor.startAt)
                                .and(event.id.lt(cursor.id))
                        )
                } else {
                    event.startAt.gt(cursor.startAt)
                        .or(
                            event.startAt.eq(cursor.startAt)
                                .and(event.id.gt(cursor.id))
                        )
                }
            }

            is EventCursor.RecruitmentDeadlineCursor -> {
                val isFinished = param.status == EventStatus.FINISHED || param.statusGroup == EventStatusGroup.FINISHED

                if (isFinished) {
                    event.recruitmentEndAt.lt(cursor.recruitmentEndAt)
                        .or(
                            event.recruitmentEndAt.eq(cursor.recruitmentEndAt)
                                .and(event.id.lt(cursor.id))
                        )
                } else {
                    event.recruitmentEndAt.gt(cursor.recruitmentEndAt)
                        .or(
                            event.recruitmentEndAt.eq(cursor.recruitmentEndAt)
                                .and(event.id.gt(cursor.id))
                        )
                }
            }

            is EventCursor.IdCursor -> {
                event.id.lt(cursor.id)
            }

            else -> null
        }
    }

    private fun <T> JPAQuery<T>.buildOrderBy(param: EventCursorPaginationParam): JPAQuery<T> {
        val orderSpecifiers = when (param.field) {
            CREATED_AT, ID -> arrayOf(
                event.id.desc()
            )

            START_DATE -> {
                val isFinished = param.status == EventStatus.FINISHED || param.statusGroup == EventStatusGroup.FINISHED
                if (isFinished) {
                    arrayOf(event.startAt.desc(), event.id.desc())
                } else {
                    arrayOf(event.startAt.asc(), event.id.desc())
                }
            }

            RECRUITMENT_DEADLINE -> {
                val isFinished = param.status == EventStatus.FINISHED || param.statusGroup == EventStatusGroup.FINISHED
                if (isFinished) {
                    arrayOf(event.recruitmentEndAt.desc(), event.id.desc())
                } else {
                    arrayOf(event.recruitmentEndAt.asc(), event.id.desc())
                }
            }

            else -> arrayOf(event.createdAt.desc(), event.id.desc())
        }

        return this.orderBy(*orderSpecifiers)
    }

    private fun fetchEventsByViewCountWithCursor(
        param: EventCursorPaginationParam,
        cursor: EventCursor?,
        currentUserId: Long?
    ): List<Event> {
        val viewCountCursor = cursor as? EventCursor.ViewCountCursor

        val statusCondition = when {
            param.status != null -> "e.status = :status"
            param.statusGroup != null -> "e.status_group = :statusGroup"
            else -> throw IllegalStateException("status 또는 statusGroup 중 하나는 반드시 설정되어야 합니다")
        }

        val typesCondition = if (param.types.isNullOrEmpty()) {
            ""
        } else {
            "AND e.event_type IN (:types)"
        }

        val searchCondition = if (param.searchKeyword != null) {
            """
            AND (
                e.title LIKE CONCAT('%', :searchKeyword, '%')
                OR EXISTS (
                    SELECT 1 FROM event_hosts eh JOIN hosts h ON h.id = eh.host_id
                    WHERE eh.event_id = e.id AND h.name LIKE CONCAT('%', :searchKeyword, '%')
                )
            )
            """
        } else ""

        val hostCondition = if (param.hostId != null) {
            "AND EXISTS (SELECT 1 FROM event_hosts eh WHERE eh.event_id = e.id AND eh.host_id = :hostId)"
        } else ""

        // overlapsEventPeriod·overlapsRecruitmentPeriod 와 같은 조건
        val eventRange = param.eventDateRange()
        val eventDateCondition = listOfNotNull(
            eventRange.endExclusive?.let { "AND e.start_at < :eventUntil" },
            eventRange.startInclusive?.let { "AND (e.end_at >= :eventFrom OR (e.end_at IS NULL AND e.start_at >= :eventFrom))" },
        ).joinToString(" ")

        val recruitmentRange = param.recruitmentDateRange()
        val recruitmentDateCondition = if (recruitmentRange.isUnbounded) "" else listOfNotNull(
            recruitmentRange.endExclusive?.let { "AND (e.recruitment_start_at IS NULL OR e.recruitment_start_at < :recruitmentUntil)" },
            recruitmentRange.startInclusive?.let {
                "AND (e.recruitment_end_at >= :recruitmentFrom OR (e.recruitment_end_at IS NULL AND e.start_at >= :recruitmentFrom))"
            },
        ).joinToString(" ")


        val cursorCondition = if (viewCountCursor != null) {
            """
            AND (
                v.count < :cursorCount
                OR (v.count = :cursorCount AND e.id < :cursorId)
            )
            """
        } else ""

        val bookmarkJoin = if (param.bookmarked && currentUserId != null) {
            "JOIN bookmarks b ON b.event_id = e.id AND b.user_id = :userId"
        } else ""

        val sql = """
            SELECT e.*
            FROM views v FORCE INDEX (idx_count_event)
            JOIN events e ON e.id = v.event_id
            $bookmarkJoin
            WHERE EXISTS (SELECT 1 FROM event_hosts eh0 WHERE eh0.event_id = e.id)
              AND $statusCondition
              $typesCondition
              $searchCondition
              $hostCondition
              $eventDateCondition
              $recruitmentDateCondition
              $cursorCondition
            ORDER BY v.count DESC, v.event_id DESC
            LIMIT :limit
        """.trimIndent()

        val query = entityManager.createNativeQuery(sql, Event::class.java)

        // 파라미터 바인딩
        if (param.status != null) {
            query.setParameter("status", param.status.name)
        } else {
            query.setParameter("statusGroup", param.statusGroup!!.name)
        }

        if (!param.types.isNullOrEmpty()) {
            query.setParameter("types", param.types.map { it.name })
        }

        if (viewCountCursor != null) {
            query.setParameter("cursorCount", viewCountCursor.count)
            query.setParameter("cursorId", viewCountCursor.id)
        }

        if (param.bookmarked && currentUserId != null) {
            query.setParameter("userId", currentUserId)
        }

        param.searchKeyword?.let {
            query.setParameter("searchKeyword", it)
        }

        param.hostId?.let {
            query.setParameter("hostId", it)
        }

        eventRange.endExclusive?.let { query.setParameter("eventUntil", it) }
        eventRange.startInclusive?.let { query.setParameter("eventFrom", it) }
        recruitmentRange.endExclusive?.let { query.setParameter("recruitmentUntil", it) }
        recruitmentRange.startInclusive?.let { query.setParameter("recruitmentFrom", it) }

        query.setParameter("limit", param.size + 1)

        @Suppress("UNCHECKED_CAST")
        return query.resultList as List<Event>
    }

    override fun findEventsWithIncorrectStatus(now: LocalDateTime): List<Event> {
        return queryFactory
            .selectFrom(event)
            .where(
                event.status.ne(EventStatus.PENDING)
                    .and(
                        shouldBeFinished(now)
                            .or(shouldBeActive(now))
                            .or(shouldBeEventWaiting(now))
                            .or(shouldBeRecruiting(now))
                            .or(shouldBeRecruitmentWaiting(now))
                    )
            )
            .fetch()
    }

    /** Entity: time > (endAt ?: startAt) */
    private fun shouldBeFinished(now: LocalDateTime): BooleanExpression =
        event.endAt.isNotNull.and(event.endAt.lt(now))
            .or(event.endAt.isNull.and(event.startAt.lt(now)))
            .and(event.status.ne(EventStatus.FINISHED))

    /** Entity: startAt <= now AND endAt != null AND endAt >= now */
    private fun shouldBeActive(now: LocalDateTime): BooleanExpression =
        event.startAt.loe(now)
            .and(event.endAt.isNotNull)
            .and(event.endAt.goe(now))
            .and(event.status.ne(EventStatus.ACTIVE))

    /** Entity: (both null) OR (recruitmentEndAt <= now), 행사 시작 전 */
    private fun shouldBeEventWaiting(now: LocalDateTime): BooleanExpression =
        event.startAt.gt(now)
            .and(
                event.recruitmentStartAt.isNull.and(event.recruitmentEndAt.isNull)
                    .or(event.recruitmentEndAt.isNotNull.and(event.recruitmentEndAt.loe(now)))
            )
            .and(event.status.ne(EventStatus.EVENT_WAITING))

    /**
     * Entity isRecruiting 3가지 케이스, 행사 시작 전
     * end > now (gt) 사용 — end == now인 경우 eventWaiting 우선
     */
    private fun shouldBeRecruiting(now: LocalDateTime): BooleanExpression {
        // Case 1: 시작/종료일 모두 있는 경우
        val case1 = event.recruitmentStartAt.isNotNull
            .and(event.recruitmentStartAt.loe(now))
            .and(event.recruitmentEndAt.isNotNull)
            .and(event.recruitmentEndAt.gt(now))

        // Case 2: 종료일만 있는 경우
        val case2 = event.recruitmentStartAt.isNull
            .and(event.recruitmentEndAt.isNotNull)
            .and(event.recruitmentEndAt.gt(now))

        // Case 3: 시작일만 있는 경우
        val case3 = event.recruitmentStartAt.isNotNull
            .and(event.recruitmentStartAt.loe(now))
            .and(event.recruitmentEndAt.isNull)

        return event.startAt.gt(now)
            .and(case1.or(case2).or(case3))
            .and(event.status.ne(EventStatus.RECRUITING))
    }

    /** Entity: recruitmentStartAt > now, 행사 시작 전. gt — recruitmentStartAt == now면 recruiting 우선 */
    private fun shouldBeRecruitmentWaiting(now: LocalDateTime): BooleanExpression =
        event.startAt.gt(now)
            .and(event.recruitmentStartAt.isNotNull)
            .and(event.recruitmentStartAt.gt(now))
            .and(event.status.ne(EventStatus.RECRUITMENT_WAITING))
}
