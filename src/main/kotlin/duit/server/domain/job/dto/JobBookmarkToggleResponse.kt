package duit.server.domain.job.dto

data class JobBookmarkToggleResponse(
    val jobPostingId: Long,
    val isBookmarked: Boolean
)
