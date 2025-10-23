package duit.server.domain.host.entity

import duit.server.domain.event.entity.Event
import jakarta.persistence.*

@Entity
@Table(name = "hosts")
class Host(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true)
    val name: String,
    val thumbnail: String? = null,

    @OneToMany(mappedBy = "host", cascade = [CascadeType.ALL], orphanRemoval = true)
    val events: List<Event> = emptyList()
)