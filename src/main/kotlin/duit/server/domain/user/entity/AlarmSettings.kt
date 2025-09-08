package duit.server.domain.user.entity

import jakarta.persistence.Embeddable

@Embeddable
data class AlarmSettings(
    val push: Boolean = true,
    val bookmark: Boolean = true,
    val calendar: Boolean = true,
    val marketing: Boolean = true
)