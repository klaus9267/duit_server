package duit.server.domain.bookmark.dto

import duit.server.domain.bookmark.entity.Bookmark

data class BookmarkResponse(
    val id: Long,
    val eventId: Long,
    val userId: Long,
) {
    companion object {
        fun from(bookmark: Bookmark) = BookmarkResponse(
            id = bookmark.id!!,
            eventId = bookmark.event.id!!,
            userId = bookmark.user.id!!
        )
    }
}