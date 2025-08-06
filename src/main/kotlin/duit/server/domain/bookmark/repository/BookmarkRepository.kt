package duit.server.domain.bookmark.repository

import duit.server.domain.bookmark.entity.Bookmark
import org.springframework.data.jpa.repository.JpaRepository

interface BookmarkRepository : JpaRepository<Bookmark, Long> {
    fun findByEventIdAndUserId(eventId: Long, userId: Long): Bookmark?
}