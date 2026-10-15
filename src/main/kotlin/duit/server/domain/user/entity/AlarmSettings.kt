package duit.server.domain.user.entity

import jakarta.persistence.Embeddable

@Embeddable
data class AlarmSettings(
    val push: Boolean? = true,
    val bookmark: Boolean? = true,
    val calendar: Boolean? = true,
    // 광고성 정보 수신은 명시적 동의(opt-in)가 있어야 하므로 기본값은 미동의
    val marketing: Boolean? = false
)