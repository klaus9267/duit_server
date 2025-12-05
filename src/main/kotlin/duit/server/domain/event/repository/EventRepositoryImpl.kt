package duit.server.domain.event.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import duit.server.domain.bookmark.entity.QBookmark
import duit.server.domain.common.dto.pagination.EventCursor
import duit.server.domain.common.dto.pagination.PaginationField.*
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.entity.*
import duit.server.domain.host.entity.QHost
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
    private val host = QHost.host
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
            EventStatus.RECRUITING -> event.recruitmentEndAt.between(today, tomorrow)
            EventStatus.EVENT_WAITING -> event.startAt.between(today, tomorrow)
            EventStatus.ACTIVE -> {
                val endAtCondition = event.endAt.between(today, tomorrow)
                val endAtPlusOneDayCondition = event.endAt.isNull.and(
                    event.startAt.between(
                        today.minusDays(1),
                        tomorrow.minusDays(1)
                    ) as BooleanExpression
                )
                endAtCondition.or(endAtPlusOneDayCondition)
            }

            else -> throw IllegalArgumentException("Invalid status: $status")
        }

        return queryFactory
            .selectFrom(event)
            .where(
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

        // QueryDSL 쿼리 빌드
        val query = queryFactory
            .selectFrom(event)
            .join(event.host(), host).fetchJoin()
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

        val conditions = mutableListOf<BooleanExpression?>()

        // status 또는 statusGroup 필터
        when {
            param.status != null -> conditions.add(event.status.eq(param.status))
            param.statusGroup != null -> conditions.add(event.statusGroup.eq(param.statusGroup))
        }

        // eventType 필터
        conditions.add(inEventTypes(param.types))
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

    private fun buildCursorCondition(cursor: EventCursor?, param: EventCursorPaginationParam): BooleanExpression? {
        if (cursor == null) return null

        return when (cursor) {
            is EventCursor.CreatedAtCursor -> {
                event.createdAt.loe(cursor.createdAt)
                    .or(event.createdAt.eq(cursor.createdAt).and(event.id.loe(cursor.id)))
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
                    event.startAt.goe(cursor.startAt)
                        .or(
                            event.startAt.eq(cursor.startAt)
                                .and(event.id.goe(cursor.id))
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
                    event.recruitmentEndAt.goe(cursor.recruitmentEndAt)
                        .or(
                            event.recruitmentEndAt.eq(cursor.recruitmentEndAt)
                                .and(event.id.goe(cursor.id))
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
            else -> Unit
        }

        val typesCondition = if (param.types.isNullOrEmpty()) {
            ""
        } else {
            "AND e.event_type IN (:types)"
        }


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
            JOIN hosts h ON e.host_id = h.id
            $bookmarkJoin
            WHERE $statusCondition
              $typesCondition
              $cursorCondition
            ORDER BY v.count DESC, v.event_id DESC
            LIMIT :limit
        """.trimIndent()

        val query = entityManager.createNativeQuery(sql, Event::class.java)

        // 파라미터 바인딩
        when {
            param.status != null -> query.setParameter("status", param.status.name)
            param.statusGroup != null -> query.setParameter("statusGroup", param.statusGroup.name)
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

        query.setParameter("limit", param.size + 1)

        @Suppress("UNCHECKED_CAST")
        return query.resultList as List<Event>
    }
}
