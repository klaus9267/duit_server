package duit.server.domain.event.repository

import duit.server.domain.event.entity.Event
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface EventRepository : JpaRepository<Event, Long> {
    fun findByIsApproved(isApproved: Boolean, pageable: Pageable): Page<Event>
}