package duit.server.domain.event.entity

import jooq.Tables.EVENTS

enum class EventDate {
    START_AT,
    RECRUITMENT_START_AT,
    RECRUITMENT_END_AT;

    fun toField() = when (this) {
        START_AT -> EVENTS.START_AT
        RECRUITMENT_START_AT -> EVENTS.RECRUITMENT_START_AT
        RECRUITMENT_END_AT -> EVENTS.RECRUITMENT_END_AT
    }
}