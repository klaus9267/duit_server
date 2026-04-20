package duit.server.domain.job.entity

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
                company = JobCompany(
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
                JobCompany(
                    corpNm = "기존병원",
                    reperNm = "대표자",
                )
            )
            posting.changeCompany(null)

            assertNull(posting.company)
        }
    }
}
