package duit.server.domain.job.repository

import duit.server.domain.job.entity.JobPosting
import duit.server.support.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("JobPostingRepository 통합 테스트")
class JobPostingRepositoryIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var jobPostingRepository: JobPostingRepository

    @Test
    fun `active snapshot에서 누락된 활성 공고만 비활성화한다`() {
        val retained = jobPostingRepository.save(JobPosting(wantedAuthNo = "K-RETAINED"))
        val missing = jobPostingRepository.save(JobPosting(wantedAuthNo = "K-MISSING"))
        val alreadyInactive = jobPostingRepository.save(
            JobPosting(wantedAuthNo = "K-INACTIVE", isActive = false)
        )
        val reconciledAt = LocalDateTime.now().plusMinutes(1).withNano(0)

        assertThat(
            jobPostingRepository.countMissingActivePostings(
                activeExternalIds = setOf(retained.wantedAuthNo),
                snapshotStartedAt = reconciledAt,
            )
        ).isEqualTo(1)

        val deactivated = jobPostingRepository.deactivateMissingActivePostings(
            activeExternalIds = setOf(retained.wantedAuthNo),
            snapshotStartedAt = reconciledAt,
        )

        assertThat(deactivated).isEqualTo(1)
        assertThat(jobPostingRepository.findById(retained.id!!).orElseThrow().isActive).isTrue()
        jobPostingRepository.findById(missing.id!!).orElseThrow().also {
            assertThat(it.isActive).isFalse()
            assertThat(it.updatedAt).isEqualTo(reconciledAt)
        }
        jobPostingRepository.findById(alreadyInactive.id!!).orElseThrow().also {
            assertThat(it.isActive).isFalse()
            assertThat(it.updatedAt).isNotEqualTo(reconciledAt)
        }
    }

    @Test
    fun `snapshot 시작 후 갱신된 공고는 오래된 snapshot으로 비활성화하지 않는다`() {
        val retained = jobPostingRepository.save(JobPosting(wantedAuthNo = "K-RETAINED-NEWER"))
        val concurrentlyUpdated = jobPostingRepository.save(JobPosting(wantedAuthNo = "K-UPDATED-NEWER"))
        entityManager.flush()

        val snapshotStartedAt = LocalDateTime.now().withNano(0)
        val newerUpdatedAt = snapshotStartedAt.plusMinutes(1)
        entityManager.createQuery(
            "UPDATE JobPosting jobPosting SET jobPosting.updatedAt = :updatedAt WHERE jobPosting.id = :id"
        )
            .setParameter("updatedAt", newerUpdatedAt)
            .setParameter("id", concurrentlyUpdated.id)
            .executeUpdate()
        entityManager.clear()

        assertThat(
            jobPostingRepository.countMissingActivePostings(
                activeExternalIds = setOf(retained.wantedAuthNo),
                snapshotStartedAt = snapshotStartedAt,
            )
        ).isZero()

        val deactivated = jobPostingRepository.deactivateMissingActivePostings(
            activeExternalIds = setOf(retained.wantedAuthNo),
            snapshotStartedAt = snapshotStartedAt,
        )

        assertThat(deactivated).isZero()
        jobPostingRepository.findById(concurrentlyUpdated.id!!).orElseThrow().also {
            assertThat(it.isActive).isTrue()
            assertThat(it.updatedAt).isEqualTo(newerUpdatedAt)
        }
    }
}
