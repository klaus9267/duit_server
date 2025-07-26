package duit.server.domain.user.entity

import duit.server.domain.bookmark.entity.Bookrmark
import duit.server.domain.organizer.entity.Host
import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val email: String,
    val loginId: String,
    val password: String,
    val nickname: String,
    val providerType: ProviderType,
    val providerId: String,
    val allowPushAlarm: Boolean,
    val allowMarketingAlarm: Boolean,
) {
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookrmarks: MutableList<Bookrmark> = mutableListOf()

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val host: Host? = null
}