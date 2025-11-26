package duit.server.domain.event.repository

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import duit.server.domain.bookmark.entity.QBookmark
import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.dto.pagination.PaginationField.*
import duit.server.domain.event.dto.EventPaginationParamV2
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.entity.QEvent
import duit.server.domain.host.entity.QHost
import duit.server.domain.view.entity.QView
import jakarta.persistence.EntityManager
import org.apache.coyote.BadRequestException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.util.StopWatch

@Repository
class EventRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val entityManager: EntityManager
) : EventRepositoryCustom {

    private val event = QEvent.event
    private val host = QHost.host
    private val view = QView.view
    private val bookmark = QBookmark.bookmark

    override fun findEvents(
        param: EventPaginationParamV2,
        currentUserId: Long?,
        pageable: Pageable
    ): Page<Event> {
        // param.status 기반으로 단순 조회
        val offset = pageable.offset
        val pageSize = pageable.pageSize

        val watch = StopWatch()
        watch.start()

        val events = fetchEvents(param, currentUserId, offset, pageSize.toLong())

        watch.stop()
        println(watch.prettyPrint())

        watch.start()
        // 전체 개수 조회
        val total = countEvents(param, currentUserId)
        watch.stop()
        println(watch.prettyPrint())

        return PageImpl(events, pageable, total)
    }

    private fun fetchEvents(
        param: EventPaginationParamV2,
        currentUserId: Long?,
        offset: Long,
        limit: Long
    ): List<Event> {
        if (param.field == VIEW_COUNT) {
            return fetchEventsByViewCount(param, currentUserId, offset, limit)
        }

        return queryFactory
            .selectFrom(event)
            .join(event.host(), host).fetchJoin()
            .join(event.view(), view).fetchJoin()
            .applyFilters(param, currentUserId)
            .orderBy(*buildOrderBy(param.field, param.status))
            .offset(offset)
            .limit(limit)
            .fetch()
    }

    private fun fetchEventsByViewCount(
        param: EventPaginationParamV2,
        currentUserId: Long?,
        offset: Long,
        limit: Long
    ): List<Event> {
        // param.status 사용 (단일 값)
        val statusCondition = "e.status = '${param.status.name}'"

        val bookmarkJoin = if (param.bookmarked && currentUserId != null) {
            "JOIN bookmarks b ON b.event_id = e.id AND b.user_id = :userId"
        } else ""

        val eventTypeCondition = if (!param.types.isNullOrEmpty()) {
            "AND e.event_type IN (:types)"
        } else ""

        val sql = """
            SELECT e.*
            FROM views v
            JOIN events e ON v.event_id = e.id
            JOIN hosts h ON e.host_id = h.id
            $bookmarkJoin
            WHERE $statusCondition
              $eventTypeCondition
            ORDER BY v.count DESC, v.event_id DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val query = entityManager.createNativeQuery(sql, Event::class.java)
            .setParameter("limit", limit.toInt())
            .setParameter("offset", offset.toInt())

        if (param.bookmarked && currentUserId != null) {
            query.setParameter("userId", currentUserId)
        }

        if (!param.types.isNullOrEmpty()) {
            query.setParameter("types", param.types.map { it.name })
        }

        @Suppress("UNCHECKED_CAST")
        return query.resultList as List<Event>
    }

    private fun inEventTypes(eventTypes: List<EventType>?): BooleanExpression? {
        return if (!eventTypes.isNullOrEmpty()) {
            event.eventType.`in`(eventTypes)
        } else null
    }

    private fun buildOrderBy(sortField: PaginationField?, status: EventStatus): Array<OrderSpecifier<*>> {
        val isFinishedOnly = (status == EventStatus.FINISHED)

        return when (sortField) {
            // 최신 등록순
            CREATED_AT -> arrayOf(
                event.createdAt.desc(),
                event.id.desc()
            )

            // ID 정렬
            ID -> arrayOf(
                event.id.desc()
            )

            // 행사 날짜 임박순
            START_DATE -> {
                // ACTIVE, RECRUITING: 오름차순 (가까운 날짜 먼저)
                // FINISHED: 내림차순 (최근 종료 먼저)
                if (isFinishedOnly) {
                    arrayOf(
                        event.startAt.desc(),
                        event.id.desc()
                    )
                } else {
                    arrayOf(
                        event.startAt.asc(),
                        event.id.desc()
                    )
                }
            }

            // 모집 마감 임박순
            RECRUITMENT_DEADLINE -> {
                // ACTIVE, RECRUITING: 오름차순 (가까운 마감일 먼저)
                // FINISHED: 내림차순 (최근 마감일 먼저)
                if (isFinishedOnly) {
                    arrayOf(
                        event.recruitmentEndAt.desc(),
                        event.id.desc()
                    )
                } else {
                    arrayOf(
                        event.recruitmentEndAt.asc(),
                        event.id.desc()
                    )
                }
            }

            else -> {
                throw BadRequestException("지원하지 않는 정렬 형식입니다. $sortField")
            }
        }
    }

    private fun countEvents(param: EventPaginationParamV2, currentUserId: Long?): Long {
        val query = queryFactory
            .select(event.count())
            .from(event)
            .applyFilters(param, currentUserId)

        return query.fetchOne() ?: 0L
    }

    private fun <T> JPAQuery<T>.applyFilters(param: EventPaginationParamV2, currentUserId: Long?): JPAQuery<T> {
        if (param.bookmarked && currentUserId != null) {
            this.join(event.bookmarks, bookmark)
                .on(bookmark.user().id.eq(currentUserId))
        }

        val conditions = mutableListOf<BooleanExpression?>(
            event.status.eq(param.status),
            inEventTypes(param.types)
        )

        when (param.field) {
            RECRUITMENT_DEADLINE -> {
                conditions.add(event.recruitmentEndAt.isNotNull)
            }

            START_DATE -> {
                conditions.add(event.startAt.isNotNull)
            }

            else -> Unit
        }

        return this.where(*conditions.filterNotNull().toTypedArray())
    }
}
