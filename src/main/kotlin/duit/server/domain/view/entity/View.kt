package duit.server.domain.view.entity

import duit.server.domain.event.entity.Event
import jakarta.persistence.*

@Entity
@Table(name = "views")
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