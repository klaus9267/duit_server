package duit.server.domain.event.dto

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 날짜 범위 필터. 양 끝 날짜를 하루 단위로 포함하고, 비어 있는 쪽은 열려 있다.
 * 조회 조건은 [startInclusive] 이상, [endExclusive] 미만이다.
 */
data class DateRangeFilter(val from: LocalDate?, val to: LocalDate?) {
    val isUnbounded: Boolean get() = from == null && to == null
    val startInclusive: LocalDateTime? get() = from?.atStartOfDay()
    val endExclusive: LocalDateTime? get() = to?.plusDays(1)?.atStartOfDay()

    fun cacheKey(): String = "${from ?: ""}~${to ?: ""}"
}
