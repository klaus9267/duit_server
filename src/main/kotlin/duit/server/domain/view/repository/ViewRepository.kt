package duit.server.domain.view.repository

import duit.server.domain.view.entity.View
import org.springframework.data.jpa.repository.JpaRepository

interface ViewRepository : JpaRepository<View, Long> {
    fun findByEventId(eventId: Long): View?
}