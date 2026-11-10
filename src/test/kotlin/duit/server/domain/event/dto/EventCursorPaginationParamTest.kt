package duit.server.domain.event.dto

import duit.server.domain.common.dto.pagination.PaginationField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("EventCursorPaginationParam 날짜 범위 필터")
class EventCursorPaginationParamTest {

    private val nov1: LocalDate = LocalDate.of(2026, 11, 1)
    private val nov30: LocalDate = LocalDate.of(2026, 11, 30)

    @Test
    @DisplayName("날짜 필터가 없으면 캐시 키가 이전과 같다")
    fun cacheKeyWithoutDateFilterIsUnchanged() {
        assertEquals("created_at:sg:active:all:10:first", EventCursorPaginationParam().cacheKey())
        assertEquals("start_date:h3:sg:active:all:20:first", EventCursorPaginationParam(field = PaginationField.START_DATE, size = 20, hostId = 3).cacheKey())
    }

    @Test
    @DisplayName("날짜 필터는 캐시 키에 들어가고, 범위가 다르면 키도 다르다")
    fun cacheKeyIncludesDateFilters() {
        val eventFiltered = EventCursorPaginationParam(eventDateFrom = nov1, eventDateTo = nov30)
        val recruitmentFiltered = EventCursorPaginationParam(recruitmentDateFrom = nov1)

        assertEquals("created_at:e2026-11-01~2026-11-30:sg:active:all:10:first", eventFiltered.cacheKey())
        assertEquals("created_at:r2026-11-01~:sg:active:all:10:first", recruitmentFiltered.cacheKey())
        assertNotEquals(eventFiltered.cacheKey(), EventCursorPaginationParam(eventDateFrom = nov1).cacheKey())
    }

    @Test
    @DisplayName("범위 시작이 끝보다 늦으면 IllegalArgumentException, 같은 날은 허용")
    fun rejectsReversedRange() {
        assertThrows<IllegalArgumentException> { EventCursorPaginationParam(eventDateFrom = nov30, eventDateTo = nov1) }
        assertThrows<IllegalArgumentException> { EventCursorPaginationParam(recruitmentDateFrom = nov30, recruitmentDateTo = nov1) }
        assertDoesNotThrow { EventCursorPaginationParam(eventDateFrom = nov1, eventDateTo = nov1) }
    }

    @Test
    @DisplayName("범위는 양 끝 날짜를 하루 단위로 포함한다")
    fun rangeBoundsCoverWholeDays() {
        val range = EventCursorPaginationParam(eventDateFrom = nov1, eventDateTo = nov30).eventDateRange()

        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), range.startInclusive)
        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0), range.endExclusive)
    }
}
