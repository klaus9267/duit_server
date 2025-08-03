package duit.server.domain.host.entity

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
)