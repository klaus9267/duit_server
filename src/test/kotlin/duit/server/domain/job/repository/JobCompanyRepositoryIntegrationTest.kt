package duit.server.domain.job.repository

import duit.server.domain.job.entity.Company
import duit.server.domain.job.entity.JobPosting
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("JobCompanyRepository 통합 테스트")
class JobCompanyRepositoryIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var jobCompanyRepository: JobCompanyRepository

    private val now = LocalDateTime.of(2026, 10, 15, 12, 0)

    private fun companyWithPosting(
        name: String,
        businessNumber: String? = "123-45-${System.nanoTime() % 100000}",
        checkedAt: LocalDateTime? = null,
        postingActive: Boolean = true,
    ): Company {
        val company = TestFixtures.company(corpNm = name, businessNumber = businessNumber).apply {
            wageDefaulterCheckedAt = checkedAt
            wageDefaulter = checkedAt?.let { false }
        }
        entityManager.persist(company)
        entityManager.persist(
            JobPosting(wantedAuthNo = "K-$name", isActive = postingActive).apply { changeCompany(company) }
        )
        return company
    }

    @Test
    fun `활성 공고가 있고 미확인이거나 24시간 지난 회사만 미확인 우선으로 조회한다`() {
        val stale = companyWithPosting("오래전확인", checkedAt = now.minusHours(30))
        val unchecked = companyWithPosting("미확인")
        companyWithPosting("최근확인", checkedAt = now.minusHours(2))
        companyWithPosting("비활성공고", postingActive = false)
        companyWithPosting("사업자번호없음", businessNumber = null)
        entityManager.persist(TestFixtures.company(corpNm = "공고없음"))
        entityManager.flush()
        entityManager.clear()

        val targets = jobCompanyRepository.findWageDefaulterCheckTargets(now.minusHours(24))

        assertThat(targets.map { it.id }).containsExactly(unchecked.id, stale.id)
    }

    @Test
    fun `명단공개 여부와 확인 시각을 저장한다`() {
        val company = companyWithPosting("갱신대상")
        entityManager.flush()

        val updated = jobCompanyRepository.updateWageDefaulter(company.id!!, true, now)

        assertThat(updated).isEqualTo(1)
        jobCompanyRepository.findById(company.id!!).orElseThrow().also {
            assertThat(it.wageDefaulter).isTrue()
            assertThat(it.wageDefaulterCheckedAt).isEqualTo(now)
        }
    }
}
