package duit.server.domain.event.dto

import java.time.LocalDateTime

data class EventAlarmInfo(
    val id: Long,
    val targetDateTime: LocalDateTime
)
