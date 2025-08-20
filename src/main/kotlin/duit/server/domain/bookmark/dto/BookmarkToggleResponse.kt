package duit.server.domain.bookmark.dto

data class BookmarkToggleResponse(
    val eventId: Long,
    val isBookmarked: Boolean
)