package duit.server.domain.view.entity

import duit.server.domain.event.entity.Event
import jakarta.persistence.*

@Entity
@Table(name = "views", indexes = [
    Index(name = "idx_count_event",columnList = "count, event_id")
])
class View(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var count: Int = 0,

    @OneToOne(fetch = FetchType.LAZY)
    val event: Event
) {
    fun increaseCount() = count++
}