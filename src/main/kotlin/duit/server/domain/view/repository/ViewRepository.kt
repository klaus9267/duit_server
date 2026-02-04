package duit.server.domain.view.repository

import duit.server.domain.view.entity.View
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ViewRepository : JpaRepository<View, Long> {
    fun findByEventId(eventId: Long): View?

    @Modifying
    @Query("UPDATE View v SET v.count = v.count + 1 WHERE v.event.id = :eventId")
    fun incrementCount(@Param("eventId") eventId: Long): Int
}