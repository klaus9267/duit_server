package duit.server.domain.bookmark.repository

import duit.server.domain.bookmark.entity.Bookmark
import duit.server.domain.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BookmarkRepository : JpaRepository<Bookmark, Long> {
    fun findByEventIdAndUserId(eventId: Long, userId: Long): Bookmark?
    fun findByUserId(userId: Long, pageable: Pageable): Page<Bookmark>
    
    @Query(
        """
        SELECT DISTINCT u
        FROM User u
        JOIN Bookmark b ON b.user.id = u.id
        WHERE b.event.id = :eventId
          AND u.deviceToken IS NOT NULL 
          AND u.deviceToken != ''
          AND u.alarmSettings.push = true
          AND (
            u.alarmSettings.bookmark = true 
            OR (u.alarmSettings.calendar = true AND b.isAddedToCalendar = true)
          )
        """
    )
    fun findEligibleUsersForAlarms(eventId: Long): List<User>
}