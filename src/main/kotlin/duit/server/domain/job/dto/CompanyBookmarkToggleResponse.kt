package duit.server.domain.job.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회사 북마크 토글 응답")
data class CompanyBookmarkToggleResponse(
    @Schema(description = "대상 회사 ID", example = "1")
    val companyId: Long,

    @Schema(description = "토글 후 북마크 여부", example = "true")
    val isBookmarked: Boolean,
)
