package duit.server.domain.host.dto

import io.swagger.v3.oas.annotations.media.Schema

data class HostUpdateRequest(
    @field:Schema(description = "주최 기관명", example = "단국대학교 IT 대학")
    val name: String,

    @field:Schema(description = "썸네일 삭제 여부", example = "false", defaultValue = "false")
    val deleteThumbnail: Boolean = false
)
