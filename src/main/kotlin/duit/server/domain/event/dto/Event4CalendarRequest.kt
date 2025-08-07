package duit.server.domain.event.dto

import duit.server.domain.event.entity.EventType

data class Event4CalendarRequest(
    val month: Int,
    val year: Int,
    val type: EventType?,
)
