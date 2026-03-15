package duit.server.domain.job.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "job_sync_states")
class JobSyncState(
    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val sourceType: SourceType,

    /** 증분 수집 워터마크. 이 시각 이후 변경된 항목만 수집. */
    @Column(nullable = false)
    var lastSyncedAt: LocalDateTime,

    /** 마지막 전체 동기화 시각. */
    var lastFullSyncAt: LocalDateTime? = null,
)
