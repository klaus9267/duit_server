package duit.server.domain.bookmark.repository

import duit.server.domain.bookmark.entity.Bookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface BookmarkRepository : JpaRepository<Bookmark, Long> {
    fun findByEventIdAndUserId(eventId: Long, userId: Long): Bookmark?
    fun findByUserId(userId: Long, pageable: Pageable): Page<Bookmark>
}