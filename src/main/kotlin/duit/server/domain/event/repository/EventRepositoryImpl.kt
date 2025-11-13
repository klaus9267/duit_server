package duit.server.domain.event.repository

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import duit.server.domain.bookmark.entity.QBookmark
import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.event.dto.EventSearchFilter
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.entity.QEvent
import duit.server.domain.host.entity.QHost
import duit.server.domain.view.entity.QView
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

    override fun findWithFilter(
        filter: EventSearchFilter,
        pageable: Pageable
    ): Page<Event> {
        // 메인 쿼리 - Event 엔티티 조회
        val query = queryFactory
            .selectFrom(event)
            .join(event.host(), host).fetchJoin()
            .join(event.view(), view).fetchJoin()

        // 북마크 조건이 있는 경우에만 JOIN
        if (filter.isBookmarked && filter.userId != null) {
            query.join(event.bookmarks, bookmark)
                .on(bookmark.user().id.eq(filter.userId))
        }

        // WHERE 조건 추가
        query.where(
            isApproved(filter.isApproved),
            inEventTypes(filter.eventTypes),
            isNotFinished(filter.includeFinished),
        )

        // 정렬 조건 추가
        val orderSpecifiers = buildOrderBy(filter.sortField)
        query.orderBy(*orderSpecifiers)

        // 페이지네이션
        val events = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        // 전체 개수 조회
        val total = countTotal(filter)

        return PageImpl(events, pageable, total)
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

    private fun buildOrderBy(sortField: PaginationField?): Array<OrderSpecifier<*>> {
        val now = LocalDateTime.now()

        val isUpcoming = Expressions.cases()
            .`when`(event.startAt.goe(now)).then(0)
            .otherwise(1)

        return when (sortField) {
            // 조회수 많은순
            PaginationField.VIEW_COUNT -> arrayOf(
                isUpcoming.asc(),
                view.count.desc(),
                event.id.desc()
            )

            // 최신 등록순
            PaginationField.CREATED_AT -> arrayOf(
                isUpcoming.asc(),
                event.createdAt.desc(),
                event.id.desc()
            )

            // 행사 날짜 임박순
            PaginationField.START_DATE -> {
                val timeDiff = Expressions.numberTemplate(
                    Long::class.java,
                    "ABS(TIMESTAMPDIFF(SECOND, {0}, {1}))",
                    now,
                    event.startAt
                )

                arrayOf(
                    isUpcoming.asc(),
                    timeDiff.asc(),
                    event.id.desc()
                )
            }

            // 모집 마감 임박순
            PaginationField.RECRUITMENT_DEADLINE -> {
                val timeDiff = Expressions.numberTemplate(
                    Long::class.java,
                    "CASE WHEN {0} IS NULL THEN 999999999 ELSE TIMESTAMPDIFF(SECOND, {1}, {0}) END",
                    event.recruitmentEndAt,
                    now
                )

                arrayOf(
                    isUpcoming.asc(),
                    timeDiff.asc(),
                    event.id.desc()
                )
            }

            // 기본: 최신 등록순
            else -> arrayOf(
                isUpcoming.asc(),
                event.id.desc()
            )
        }
    }

    private fun countTotal(filter: EventSearchFilter): Long {
        val query = queryFactory
            .select(event.countDistinct())
            .from(event)
            .join(event.host(), host)

        // 북마크 조건이 있는 경우에만 JOIN
        if (filter.isBookmarked && filter.userId != null) {
            query.join(event.bookmarks, bookmark)
                .on(bookmark.user().id.eq(filter.userId))
        }

        query.where(
            isApproved(filter.isApproved),
            inEventTypes(filter.eventTypes),
            isNotFinished(filter.includeFinished),
        )

        return query.fetchOne() ?: 0L
    }
}
