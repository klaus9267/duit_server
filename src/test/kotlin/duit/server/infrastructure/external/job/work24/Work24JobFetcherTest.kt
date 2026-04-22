package duit.server.infrastructure.external.job.work24

import duit.server.domain.job.entity.SourceType
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("Work24JobFetcher 단위 테스트")
class Work24JobFetcherTest {

    private lateinit var fetcher: Work24JobFetcher

    @BeforeEach
    fun setUp() {
        fetcher = Work24JobFetcher(authKey = "test-auth-key", listPageLimit = 0, detailLimit = 0)
    }

    private fun callStripNonXmlTags(xml: String): String {
        val method = Work24JobFetcher::class.java.getDeclaredMethod("stripNonXmlTags", String::class.java)
        method.isAccessible = true
        return method.invoke(fetcher, xml) as String
    }

    private fun listItem(
        wantedAuthNo: String? = "K123456",
        company: String? = "테스트병원",
        busino: String? = "123-45-67890",
        title: String? = "간호사 모집",
        closeDt: String? = null,
        region: String? = "서울특별시 강남구",
        jobsCd: String? = "304000",
        empTpCd: String? = "10",
        wantedInfoUrl: String? = "https://example.com/1",
    ) = Work24ApiResponse.WantedItem(
        wantedAuthNo = wantedAuthNo,
        company = company,
        busino = busino,
        title = title,
        closeDt = closeDt,
        region = region,
        jobsCd = jobsCd,
        empTpCd = empTpCd,
        wantedInfoUrl = wantedInfoUrl,
    )

    private fun detailResponse(
        wantedTitle: String = "전체 제목",
        receiptCloseDt: String? = "채용시까지",
        corpNm: String = "테스트병원 상세",
        reperNm: String? = "홍길동",
        totPsncnt: String? = "100 명",
    ) = Work24DetailResponse(
        corpInfo = Work24DetailResponse.CorpInfo(
            corpNm = corpNm,
            reperNm = reperNm,
            totPsncnt = totPsncnt,
            busiCont = "의료서비스",
        ),
        wantedInfo = Work24DetailResponse.WantedInfo(
            wantedTitle = wantedTitle,
            receiptCloseDt = receiptCloseDt,
            jobsNm = "간호사",
            empTpNm = "기간의 정함이 없는 근로계약",
            eduNm = "학력무관",
        ),
        empchargeInfo = Work24DetailResponse.EmpchargeInfo(
            contactTelno = "02-1234-5678",
        ),
    )

    @Nested
    @DisplayName("authKey 검증")
    inner class AuthKeyTests {

        @Test
        fun `authKey가 빈 문자열이면 fetchAll은 빈 리스트 반환`() {
            val blankFetcher = Work24JobFetcher(authKey = "", listPageLimit = 0, detailLimit = 0)
            assertTrue(blankFetcher.fetchAll().isEmpty())
        }

        @Test
        fun `authKey가 공백이면 fetchAll은 빈 리스트 반환`() {
            val blankFetcher = Work24JobFetcher(authKey = "   ", listPageLimit = 0, detailLimit = 0)
            assertTrue(blankFetcher.fetchAll().isEmpty())
        }

        @Test
        fun `sourceType은 WORK24`() {
            assertEquals(SourceType.WORK24, fetcher.sourceType)
        }
    }

    @Nested
    @DisplayName("stripNonXmlTags()")
    inner class StripNonXmlTagsTests {

        @Test
        fun `화이트리스트에 있는 태그는 유지`() {
            val xml = "<wantedRoot><total>10</total><wanted><title>간호사</title><infoSvc>VALIDATION</infoSvc></wanted></wantedRoot>"
            assertEquals(xml, callStripNonXmlTags(xml))
        }

        @Test
        fun `화이트리스트에 없는 태그는 제거`() {
            val xml = "<wantedRoot><total>1</total><b>bold</b><script>alert(1)</script></wantedRoot>"
            assertEquals("<wantedRoot><total>1</total>boldalert(1)</wantedRoot>", callStripNonXmlTags(xml))
        }

        @Test
        fun `상세 API corpInfo 태그도 화이트리스트에 포함`() {
            val xml = "<wantedDtl><corpInfo><corpNm>테스트</corpNm><reperNm>홍길동</reperNm></corpInfo></wantedDtl>"
            assertEquals(xml, callStripNonXmlTags(xml))
        }

        @Test
        fun `상세 API empchargeInfo 태그도 화이트리스트에 포함`() {
            val xml = "<empchargeInfo><contactTelno>02-1234-5678</contactTelno></empchargeInfo>"
            assertEquals(xml, callStripNonXmlTags(xml))
        }

        @Test
        fun `HTML 태그가 섞인 응답 정리`() {
            val xml = "<wantedRoot><wanted><title><b>간호사</b> 모집</title></wanted></wantedRoot>"
            assertEquals("<wantedRoot><wanted><title>간호사 모집</title></wanted></wantedRoot>", callStripNonXmlTags(xml))
        }

        @Test
        fun `태그가 없는 텍스트는 그대로 반환`() {
            assertEquals("plain text", callStripNonXmlTags("plain text"))
        }

        @Test
        fun `속성이 있는 태그 처리`() {
            val xml = "<wantedRoot><a href=\"url\">link</a><title>간호사</title></wantedRoot>"
            assertEquals("<wantedRoot>link<title>간호사</title></wantedRoot>", callStripNonXmlTags(xml))
        }
    }

    @Nested
    @DisplayName("fetchAll()")
    inner class FetchAllTests {

        private lateinit var spyFetcher: Work24JobFetcher

        @BeforeEach
        fun setUp() {
            spyFetcher = spyk(
                Work24JobFetcher(authKey = "test-auth-key", listPageLimit = 0, detailLimit = 0),
                recordPrivateCalls = true,
            )
        }

        @Test
        fun `목록과 상세를 모두 조회하여 병합`() {
            every { spyFetcher["fetchListPage"](1) } returns Work24ApiResponse(
                total = "1",
                wanted = listOf(listItem())
            )
            every { spyFetcher["fetchDetail"]("K123456") } returns detailResponse(wantedTitle = "수간호사 채용")

            val results = spyFetcher.fetchAll()

            assertEquals(1, results.size)
            val result = results[0]
            assertEquals("K123456", result.externalId)
            assertEquals("수간호사 채용", result.detail.wantedTitle)
            assertEquals("간호사", result.detail.jobsNm)
            assertEquals("02-1234-5678", result.detail.contactTelno)
            assertEquals("테스트병원 상세", result.company.corpNm)
            assertEquals("홍길동", result.company.reperNm)
            assertEquals(100L, result.company.totPsncnt)
            assertEquals("123-45-67890", result.company.businessNumber)
        }

        @Test
        fun `detail 조회 실패 시 해당 항목은 건너뛴다`() {
            every { spyFetcher["fetchListPage"](1) } returns Work24ApiResponse(
                total = "2",
                wanted = listOf(
                    listItem(wantedAuthNo = "K1"),
                    listItem(wantedAuthNo = "K2"),
                )
            )
            every { spyFetcher["fetchDetail"]("K1") } returns detailResponse()
            every { spyFetcher["fetchDetail"]("K2") } returns null

            val results = spyFetcher.fetchAll()

            assertEquals(1, results.size)
            assertEquals("K1", results[0].externalId)
        }

        @Test
        fun `detail 조회에서 예외가 나도 해당 항목만 건너뛴다`() {
            every { spyFetcher["fetchListPage"](1) } returns Work24ApiResponse(
                total = "2",
                wanted = listOf(
                    listItem(wantedAuthNo = "K1"),
                    listItem(wantedAuthNo = "K2"),
                )
            )
            every { spyFetcher["fetchDetail"]("K1") } throws RuntimeException("network error")
            every { spyFetcher["fetchDetail"]("K2") } returns detailResponse()

            val results = spyFetcher.fetchAll()

            assertEquals(1, results.size)
            assertEquals("K2", results[0].externalId)
        }

        @Test
        fun `빈 목록이면 detail을 조회하지 않고 빈 리스트 반환`() {
            every { spyFetcher["fetchListPage"](1) } returns Work24ApiResponse(total = "0", wanted = emptyList())

            val results = spyFetcher.fetchAll()

            assertTrue(results.isEmpty())
        }

        @Test
        fun `fetchListPage가 null이면 빈 리스트 반환`() {
            every { spyFetcher["fetchListPage"](1) } returns null

            val results = spyFetcher.fetchAll()

            assertTrue(results.isEmpty())
        }

        @Test
        fun `receiptCloseDt가 채용시까지이면 isActive true`() {
            every { spyFetcher["fetchListPage"](1) } returns Work24ApiResponse(
                total = "1", wanted = listOf(listItem())
            )
            every { spyFetcher["fetchDetail"]("K123456") } returns detailResponse(receiptCloseDt = "채용시까지")

            val results = spyFetcher.fetchAll()

            assertTrue(results[0].isActive)
        }

        @Test
        fun `receiptCloseDt가 과거 날짜면 isActive false`() {
            every { spyFetcher["fetchListPage"](1) } returns Work24ApiResponse(
                total = "1", wanted = listOf(listItem())
            )
            every { spyFetcher["fetchDetail"]("K123456") } returns detailResponse(receiptCloseDt = "2020-01-01")

            val results = spyFetcher.fetchAll()

            assertEquals(false, results[0].isActive)
        }
    }

    @Nested
    @DisplayName("listPageLimit / detailLimit 설정")
    inner class LimitTests {

        @Test
        fun `listPageLimit이 1이면 첫 페이지만 조회한다`() {
            val limited = spyk(
                Work24JobFetcher(authKey = "test-auth-key", listPageLimit = 1, detailLimit = 0),
                recordPrivateCalls = true,
            )
            val page1 = (1..100).map { listItem(wantedAuthNo = "K$it") }

            every { limited["fetchListPage"](1) } returns Work24ApiResponse(total = "200", wanted = page1)
            every { limited["fetchDetail"](any<String>()) } returns detailResponse()

            val results = limited.fetchAll()

            assertEquals(100, results.size)
        }

        @Test
        fun `detailLimit 100이면 목록이 더 많아도 100건만 상세 조회한다`() {
            val limited = spyk(
                Work24JobFetcher(authKey = "test-auth-key", listPageLimit = 0, detailLimit = 100),
                recordPrivateCalls = true,
            )
            val page1 = (1..100).map { listItem(wantedAuthNo = "K$it") }
            val page2 = (101..120).map { listItem(wantedAuthNo = "K$it") }

            every { limited["fetchListPage"](1) } returns Work24ApiResponse(total = "120", wanted = page1)
            every { limited["fetchListPage"](2) } returns Work24ApiResponse(total = "120", wanted = page2)
            every { limited["fetchDetail"](any<String>()) } returns detailResponse()

            val results = limited.fetchAll()

            assertEquals(100, results.size)
        }
    }

    @Nested
    @DisplayName("merge 결과 검증")
    inner class MergeTests {

        private lateinit var spyFetcher: Work24JobFetcher

        @BeforeEach
        fun setUp() {
            spyFetcher = spyk(
                Work24JobFetcher(authKey = "test-auth-key", listPageLimit = 0, detailLimit = 0),
                recordPrivateCalls = true,
            )
        }

        @Test
        fun `wantedAuthNo가 null인 목록 항목은 건너뛴다`() {
            every { spyFetcher["fetchListPage"](1) } returns Work24ApiResponse(
                total = "2",
                wanted = listOf(
                    listItem(wantedAuthNo = null),
                    listItem(wantedAuthNo = "K2"),
                )
            )
            every { spyFetcher["fetchDetail"]("K2") } returns detailResponse()

            val results = spyFetcher.fetchAll()

            assertEquals(1, results.size)
            assertEquals("K2", results[0].externalId)
        }

        @Test
        fun `상세에 값이 있으면 목록보다 상세값 우선`() {
            every { spyFetcher["fetchListPage"](1) } returns Work24ApiResponse(
                total = "1",
                wanted = listOf(listItem(title = "목록 제목", jobsCd = "999999"))
            )
            every { spyFetcher["fetchDetail"]("K123456") } returns Work24DetailResponse(
                wantedInfo = Work24DetailResponse.WantedInfo(
                    wantedTitle = "상세 제목",
                    jobsCd = "304002",
                ),
                corpInfo = Work24DetailResponse.CorpInfo(corpNm = "상세 회사"),
            )

            val result = spyFetcher.fetchAll()[0]

            assertEquals("상세 제목", result.detail.wantedTitle)
            assertEquals("304002", result.detail.jobsCd)
            assertEquals("상세 회사", result.company.corpNm)
        }

        @Test
        fun `상세에 값이 없으면 목록값 fallback`() {
            every { spyFetcher["fetchListPage"](1) } returns Work24ApiResponse(
                total = "1",
                wanted = listOf(listItem(
                    title = "목록 제목",
                    region = "서울",
                    wantedInfoUrl = "https://list-url",
                ))
            )
            every { spyFetcher["fetchDetail"]("K123456") } returns Work24DetailResponse(
                wantedInfo = Work24DetailResponse.WantedInfo(),
                corpInfo = Work24DetailResponse.CorpInfo(),
            )

            val result = spyFetcher.fetchAll()[0]

            assertEquals("목록 제목", result.detail.wantedTitle)
            assertEquals("서울", result.detail.workRegion)
            assertEquals("https://list-url", result.detail.dtlRecrContUrl)
        }

        @Test
        fun `자본금 0 백만원은 null로 변환`() {
            every { spyFetcher["fetchListPage"](1) } returns Work24ApiResponse(
                total = "1", wanted = listOf(listItem())
            )
            every { spyFetcher["fetchDetail"]("K123456") } returns Work24DetailResponse(
                corpInfo = Work24DetailResponse.CorpInfo(
                    corpNm = "테스트",
                    capitalAmt = "0 백만원",
                    totPsncnt = "100 명",
                ),
                wantedInfo = Work24DetailResponse.WantedInfo(),
            )

            val result = spyFetcher.fetchAll()[0]

            assertNull(result.company.capitalAmt)
            assertEquals(100L, result.company.totPsncnt)
        }
    }
}
