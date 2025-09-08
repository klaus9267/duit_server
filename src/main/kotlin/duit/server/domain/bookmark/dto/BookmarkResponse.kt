package duit.server.domain.bookmark.dto

import duit.server.domain.bookmark.entity.Bookmark
import java.time.LocalDateTime

data class BookmarkResponse(
    val id: Long,
    val eventId: Long,
    val userId: Long,
    val isAddedToCalendar: Boolean,
) {
    companion object {
        fun from(bookmark: Bookmark) = BookmarkResponse(
            id = bookmark.id!!,
            eventId = bookmark.event.id!!,
            userId = bookmark.user.id!!,
            isAddedToCalendar = bookmark.isAddedToCalendar,
        )
    }
}