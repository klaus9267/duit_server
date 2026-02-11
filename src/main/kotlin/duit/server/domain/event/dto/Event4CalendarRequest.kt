package duit.server.domain.event.dto

import duit.server.domain.event.entity.EventType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class Event4CalendarRequest(
    @field:Min(value = 1, message = "월은 1 이상이어야 합니다")
    @field:Max(value = 12, message = "월은 12 이하여야 합니다")
    val month: Int,

    @field:Min(value = 1900, message = "연도는 1900 이상이어야 합니다")
    @field:Max(value = 2100, message = "연도는 2100 이하여야 합니다")
    val year: Int,

    val type: EventType?,
)
