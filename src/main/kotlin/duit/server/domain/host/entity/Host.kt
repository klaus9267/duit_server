package duit.server.domain.host.entity

import duit.server.domain.event.entity.EventHost
import jakarta.persistence.*

@Entity
@Table(name = "hosts")
class Host(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true)
    var name: String,
    var thumbnail: String? = null,

    @OneToMany(mappedBy = "host")
    val eventHosts: List<EventHost> = emptyList()
)