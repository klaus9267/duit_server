package duit.server.domain.job.entity

import duit.server.infrastructure.external.job.work24.Work24CodeMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JobPosting 엔티티 단위 테스트")
class JobPostingTest {

    private fun createJobPosting(
        wantedAuthNo: String = "K123456",
        isActive: Boolean = true,
    ) = JobPosting(
        wantedAuthNo = wantedAuthNo,
        isActive = isActive,
    )

    @Nested
    @DisplayName("updateWork24Detail()")
    inner class UpdateWork24DetailTests {

        @Test
        fun `고용24 상세 필드가 JobPosting에 그대로 반영된다`() {
            val posting = createJobPosting()

            posting.updateWork24Detail(
                detail = JobPostingWork24Detail(
                    jobsNm = "간호사",
                    wantedTitle = "병동 간호사 모집",
                    relJobsNm = "간호조무사",
                    jobCont = "병동 간호 업무",
                    receiptCloseDt = "2026-04-30",
                    empTpNm = "기간의 정함이 없는 근로계약",
                    collectPsncnt = "2명",
                    salTpNm = "연봉",
                    enterTpNm = "경력",
                    eduNm = "학사",
                    workRegion = "서울특별시 강남구",
                    nearLine = "2호선 역삼역",
                    fourIns = "국민연금 고용보험 산재보험 건강보험",
                    attachFileUrl = "https://example.com/company.pdf",
                    corpAttachList = listOf("https://example.com/form.hwp", "https://example.com/form.hwp"),
                    keywordList = listOf("간호사", "병동", "간호사"),
                    jobsCd = "304000",
                    empChargerDpt = "간호부",
                    contactTelno = "02-1234-5678",
                ),
                company = Company(
                    businessNumber = "123-45-67890",
                    corpNm = "테스트병원",
                    reperNm = "홍길동",
                    busiCont = "의료서비스업",
                ),
            )

            assertEquals("K123456", posting.wantedAuthNo)
            assertEquals("간호사", posting.jobsNm)
            assertEquals("병동 간호사 모집", posting.wantedTitle)
            assertEquals("간호조무사", posting.relJobsNm)
            assertEquals("병동 간호 업무", posting.jobCont)
            assertEquals("2026-04-30", posting.receiptCloseDt)
            assertEquals("기간의 정함이 없는 근로계약", posting.empTpNm)
            assertEquals("2명", posting.collectPsncnt)
            assertEquals("연봉", posting.salTpNm)
            assertEquals("경력", posting.enterTpNm)
            assertEquals("학사", posting.eduNm)
            assertEquals("서울특별시 강남구", posting.workRegion)
            assertEquals("2호선 역삼역", posting.nearLine)
            assertEquals("국민연금 고용보험 산재보험 건강보험", posting.fourIns)
            assertEquals("https://example.com/company.pdf", posting.attachFileUrl)
            assertEquals(listOf("https://example.com/form.hwp"), posting.corpAttachList)
            assertEquals(listOf("간호사", "병동"), posting.keywordList)
            assertEquals("304000", posting.jobsCd)
            assertEquals("간호부", posting.empChargerDpt)
            assertEquals("02-1234-5678", posting.contactTelno)
            assertEquals("테스트병원", posting.company?.corpNm)
            assertEquals("123-45-67890", posting.company?.businessNumber)
            assertEquals("홍길동", posting.company?.reperNm)
            assertEquals("의료서비스업", posting.company?.busiCont)
        }
    }

    @Nested
    @DisplayName("changeCompany()")
    inner class ChangeCompanyTests {

        @Test
        fun `company를 null로 바꾸면 제거한다`() {
            val posting = createJobPosting()

            posting.changeCompany(
                Company(
                    corpNm = "기존병원",
                    reperNm = "대표자",
                )
            )
            posting.changeCompany(null)

            assertNull(posting.company)
        }
    }

    @Nested
    @DisplayName("refreshMinimumWageFlag()")
    inner class MinimumWageFlagTests {

        private val today = java.time.LocalDate.of(2026, 10, 15)

        private fun postingWith(
            salTpNm: String?,
            annualizedSalary: Long?,
            empTpCd: String? = "10",
            workdayWorkhrCont: String? = null,
        ) = createJobPosting().apply {
            updateWork24Detail(
                JobPostingWork24Detail(
                    wantedTitle = "간호사 모집",
                    salTpNm = salTpNm,
                    salaryMin = annualizedSalary,
                    empTpCd = empTpCd,
                    workdayWorkhrCont = workdayWorkhrCont,
                )
            )
            refreshMinimumWageFlag(today)
        }

        @Test
        fun `연간 환산 급여를 임금형태 금액으로 되돌려 최저임금 미달을 판정한다`() {
            assertEquals(true, postingWith("시급 10,000원", 10_000L * 2_508).belowMinimumWage)
            assertEquals(false, postingWith("시급 10,320원", 10_320L * 2_508).belowMinimumWage)
            assertEquals(true, postingWith("월급 200만원 이상", 2_000_000L * 12).belowMinimumWage)
            assertEquals(false, postingWith("연봉 3,000만원", 30_000_000L).belowMinimumWage)
        }

        @Test
        fun `임금 정보가 없거나 형태를 알 수 없으면 미달로 보지 않는다`() {
            assertEquals(false, postingWith("회사내규에 따름", null).belowMinimumWage)
            assertEquals(false, postingWith("급여 100만원", 1_000_000L).belowMinimumWage)
            assertEquals(false, postingWith("월급 100만원", 1_000_000L * 12, empTpCd = "21").belowMinimumWage)
        }

        @Test
        fun `급여 설명에서 읽은 만 단위 혼합 금액은 오탐하지 않는다`() {
            val description = "시급 1만 320원"
            val salaryMin = Work24CodeMapper.annualizeSalary(Work24CodeMapper.parseSalaryAmount(description), description)

            assertEquals(false, postingWith(description, salaryMin).belowMinimumWage)
        }

        @Test
        fun `고용24 상세 갱신 시 판정 결과도 함께 갱신된다`() {
            val posting = createJobPosting()

            posting.updateWork24Detail(
                JobPostingWork24Detail(salTpNm = "시급 9,000원", salaryMin = 9_000L * 2_508, empTpCd = "10")
            )
            assertEquals(true, posting.belowMinimumWage)

            posting.updateWork24Detail(
                JobPostingWork24Detail(salTpNm = "시급 12,000원", salaryMin = 12_000L * 2_508, empTpCd = "10")
            )
            assertEquals(false, posting.belowMinimumWage)
        }
    }
}
