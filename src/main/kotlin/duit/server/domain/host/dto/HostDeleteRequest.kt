package duit.server.domain.host.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class HostDeleteRequest(
    @field:NotEmpty(message = "삭제할 주최 기관 ID 목록은 필수입니다")
    @field:Schema(description = "삭제할 주최 기관 ID 목록", example = "[1, 2, 3]")
    val hostIds: List<Long>
)
