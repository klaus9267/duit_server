package duit.server.domain.event.repository

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import duit.server.domain.bookmark.entity.QBookmark
import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.dto.pagination.PaginationField.*
import duit.server.domain.event.dto.EventPaginationParamV2
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.entity.QEvent
import duit.server.domain.host.entity.QHost
import duit.server.domain.view.entity.QView
import org.apache.coyote.BadRequestException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EventRepositoryImpl(
    private val queryFactory: JPAQueryFactory
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
        val now = LocalDateTime.now()

        // 단일 쿼리로 미진행/종료 행사 개수 조회
        val (upcomingCount, total) = countEvents(param, currentUserId, now)

        val offset = pageable.offset
        val pageSize = pageable.pageSize

        val events = mutableListOf<Event>()

        // offset 위치에 따라 쿼리 분기
        when {
            offset < upcomingCount -> {
                // 미진행 행사 영역
                val upcomingEvents = fetchEvents(
                    param, currentUserId, true, now,
                    offset, pageSize.toLong()
                )
                events.addAll(upcomingEvents)

                // 페이지가 미진행 영역을 넘어가는 경우
                if (upcomingEvents.size < pageSize) {
                    val remaining = pageSize - upcomingEvents.size
                    val finishedEvents = fetchEvents(
                        param, currentUserId, false, now,
                        0, remaining.toLong()
                    )
                    events.addAll(finishedEvents)
                }
            }

            else -> {
                // 종료된 행사 영역
                val adjustedOffset = offset - upcomingCount
                val finishedEvents = fetchEvents(
                    param, currentUserId, false, now,
                    adjustedOffset, pageSize.toLong()
                )
                events.addAll(finishedEvents)
            }
        }

        return PageImpl(events, pageable, total)
    }

    private fun fetchEvents(
        param: EventPaginationParamV2,
        currentUserId: Long?,
        isUpcoming: Boolean,
        now: LocalDateTime,
        offset: Long,
        limit: Long
    ): List<Event> {
        val query = queryFactory
            .selectFrom(event)
            .join(event.host(), host).fetchJoin()
            .join(event.view(), view).fetchJoin()
            .applyFilters(param, currentUserId)
            .where(if (isUpcoming) isUpcoming(now) else isFinished(now))
            .orderBy(*buildOrderBy(param.field, isUpcoming))
            .offset(offset)
            .limit(limit)

        return query.fetch()
    }

    private fun isApproved(isApproved: Boolean): BooleanExpression {
        return event.isApproved.eq(isApproved)
    }

    private fun inEventTypes(eventTypes: List<EventType>?): BooleanExpression? {
        return if (!eventTypes.isNullOrEmpty()) {
            event.eventType.`in`(eventTypes)
        } else null
    }

    private fun isNotFinished(includeFinished: Boolean): BooleanExpression? {
        if (includeFinished) return null

        val now = LocalDateTime.now()

        // (endAt IS NOT NULL AND endAt > NOW()) OR (endAt IS NULL AND startAt > NOW())
        return event.endAt.isNotNull
            .and(event.endAt.gt(now))
            .or(
                event.endAt.isNull
                    .and(event.startAt.gt(now))
            )
    }

    private fun isUpcoming(now: LocalDateTime): BooleanExpression {
        return event.startAt.goe(now)
    }

    private fun isFinished(now: LocalDateTime): BooleanExpression {
        return event.startAt.lt(now)
    }

    private fun buildOrderBy(sortField: PaginationField?, isUpcoming: Boolean): Array<OrderSpecifier<*>> {
        return when (sortField) {
            // 조회수 많은순
            VIEW_COUNT -> arrayOf(
                view.count.desc(),
                event.id.desc()
            )

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
                if (isUpcoming) {
                    // 미진행 행사: 날짜 오름차순 (가장 가까운 행사가 먼저)
                    arrayOf(
                        event.startAt.asc(),
                        event.id.desc()
                    )
                } else {
                    // 종료 행사: 날짜 내림차순 (가장 최근 종료된 행사가 먼저)
                    arrayOf(
                        event.startAt.desc(),
                        event.id.desc()
                    )
                }
            }

            // 모집 마감 임박순
            RECRUITMENT_DEADLINE -> {
                if (isUpcoming) {
                    // 미진행 행사: 마감일 오름차순 (가장 가까운 마감일이 먼저)
                    arrayOf(
                        event.recruitmentEndAt.asc().nullsLast(),
                        event.id.desc()
                    )
                } else {
                    // 종료 행사: 마감일 내림차순 (가장 최근 마감일이 먼저)
                    arrayOf(
                        event.recruitmentEndAt.desc().nullsLast(),
                        event.id.desc()
                    )
                }
            }

            else -> {
                throw BadRequestException("지원하지 않는 정렬 형식입니다. $sortField")
            }
        }
    }

    private fun countEvents(param: EventPaginationParamV2, currentUserId: Long?, now: LocalDateTime): Pair<Long, Long> {
        val upcomingCase = Expressions.cases()
            .`when`(isUpcoming(now)).then(1L)
            .otherwise(0L)

        val finishedCase = Expressions.cases()
            .`when`(isFinished(now)).then(1L)
            .otherwise(0L)

        val result = queryFactory
            .select(upcomingCase.sum(), finishedCase.sum())
            .from(event)
            .applyFilters(param, currentUserId)
            .fetchOne()

        val upcomingCount = result?.get(0, Long::class.java) ?: 0L
        val finishedCount = result?.get(1, Long::class.java) ?: 0L

        return Pair(upcomingCount, upcomingCount + finishedCount)
    }

    private fun <T> JPAQuery<T>.applyFilters(param: EventPaginationParamV2, currentUserId: Long?): JPAQuery<T> {
        if (param.bookmarked && currentUserId != null) {
            this.join(event.bookmarks, bookmark)
                .on(bookmark.user().id.eq(currentUserId))
        }

        return this.where(
            isApproved(param.approved),
            inEventTypes(param.types),
            isNotFinished(param.includeFinished),
        )
    }
}
