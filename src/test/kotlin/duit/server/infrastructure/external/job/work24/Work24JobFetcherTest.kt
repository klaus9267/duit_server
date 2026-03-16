package duit.server.infrastructure.external.job.work24

import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.SourceType
import duit.server.domain.job.entity.WorkRegion
import duit.server.infrastructure.external.job.dto.JobFetchResult
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DisplayName("Work24JobFetcher 단위 테스트")
class Work24JobFetcherTest {

    private lateinit var fetcher: Work24JobFetcher

    @BeforeEach
    fun setUp() {
        fetcher = Work24JobFetcher("test-auth-key")
    }

    // ── 리플렉션 헬퍼 ──────────────────────────────────────────────────────────────

    private fun callStripNonXmlTags(xml: String): String {
        val method = Work24JobFetcher::class.java.getDeclaredMethod("stripNonXmlTags", String::class.java)
        method.isAccessible = true
        return method.invoke(fetcher, xml) as String
    }

    private fun callParseModifyDtm(smodifyDtm: String?): LocalDateTime? {
        val method = Work24JobFetcher::class.java.getDeclaredMethod("parseModifyDtm", String::class.java)
        method.isAccessible = true
        return method.invoke(fetcher, smodifyDtm) as LocalDateTime?
    }

    private fun callToJobFetchResult(item: Work24ApiResponse.WantedItem): JobFetchResult? {
        val method = Work24JobFetcher::class.java.getDeclaredMethod(
            "toJobFetchResult", Work24ApiResponse.WantedItem::class.java
        )
        method.isAccessible = true
        return method.invoke(fetcher, item) as JobFetchResult?
    }

    private fun callEnrichTruncatedTitles(results: List<JobFetchResult>): List<JobFetchResult> {
        val method = Work24JobFetcher::class.java.getDeclaredMethod("enrichTruncatedTitles", List::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(fetcher, results) as List<JobFetchResult>
    }

    private fun createWantedItem(
        wantedAuthNo: String? = "K123456",
        company: String? = "테스트병원",
        title: String? = "간호사 모집",
        salTpNm: String? = "연봉",
        sal: String? = null,
        minSal: String? = "30000000",
        maxSal: String? = "40000000",
        region: String? = "서울특별시 강남구",
        holidayTpNm: String? = null,
        minEdubg: String? = "05",
        maxEdubg: String? = null,
        career: String? = null,
        regDt: String? = "2025-01-01",
        closeDt: String? = "2025-12-31",
        wantedInfoUrl: String? = "https://example.com/job/1",
        wantedMobileInfoUrl: String? = null,
        empTpCd: String? = "10",
        jobsCd: String? = "3040",
        smodifyDtm: String? = null,
    ) = Work24ApiResponse.WantedItem(
        wantedAuthNo = wantedAuthNo,
        company = company,
        title = title,
        salTpNm = salTpNm,
        sal = sal,
        minSal = minSal,
        maxSal = maxSal,
        region = region,
        holidayTpNm = holidayTpNm,
        minEdubg = minEdubg,
        maxEdubg = maxEdubg,
        career = career,
        regDt = regDt,
        closeDt = closeDt,
        wantedInfoUrl = wantedInfoUrl,
        wantedMobileInfoUrl = wantedMobileInfoUrl,
        empTpCd = empTpCd,
        jobsCd = jobsCd,
        smodifyDtm = smodifyDtm,
    )

    private fun createJobFetchResult(
        externalId: String = "K123456",
        title: String = "간호사 모집",
        companyName: String = "테스트병원",
    ) = JobFetchResult(
        externalId = externalId,
        title = title,
        companyName = companyName,
        jobCategory = "3040",
        location = "서울특별시 강남구",
        workRegion = WorkRegion.SEOUL,
        workDistrict = "강남구",
        employmentType = EmploymentType.FULL_TIME,
        careerMin = null,
        careerMax = null,
        educationLevel = null,
        salaryMin = 30000000L,
        salaryMax = 40000000L,
        salaryType = null,
        postingUrl = "https://example.com/job/1",
        postedAt = null,
        expiresAt = null,
        closeType = CloseType.ON_HIRE,
        isActive = true,
        workHoursPerWeek = null,
    )

    // ── authKey 검증 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("authKey 검증")
    inner class AuthKeyTests {

        @Test
        fun `authKey가 빈 문자열이면 fetchAll은 빈 리스트 반환`() {
            val blankFetcher = Work24JobFetcher("")
            val results = blankFetcher.fetchAll()
            assertTrue(results.isEmpty())
        }

        @Test
        fun `authKey가 공백이면 fetchAll은 빈 리스트 반환`() {
            val blankFetcher = Work24JobFetcher("   ")
            val results = blankFetcher.fetchAll()
            assertTrue(results.isEmpty())
        }

        @Test
        fun `authKey가 빈 문자열이면 fetchIncremental은 빈 결과 반환`() {
            val blankFetcher = Work24JobFetcher("")
            val result = blankFetcher.fetchIncremental(LocalDateTime.now())
            assertTrue(result.items.isEmpty())
            assertNull(result.latestTimestamp)
        }

        @Test
        fun `sourceType은 WORK24`() {
            assertEquals(SourceType.WORK24, fetcher.sourceType)
        }
    }

    // ── stripNonXmlTags ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("stripNonXmlTags()")
    inner class StripNonXmlTagsTests {

        @Test
        fun `화이트리스트에 있는 태그는 유지`() {
            val xml = "<wantedRoot><total>10</total><wanted><title>간호사</title></wanted></wantedRoot>"
            val result = callStripNonXmlTags(xml)
            assertEquals(xml, result)
        }

        @Test
        fun `화이트리스트에 없는 태그는 제거`() {
            val xml = "<wantedRoot><total>1</total><b>bold</b><script>alert(1)</script></wantedRoot>"
            val result = callStripNonXmlTags(xml)
            assertEquals("<wantedRoot><total>1</total>boldalert(1)</wantedRoot>", result)
        }

        @Test
        fun `HTML 태그가 섞인 응답 정리`() {
            val xml = "<wantedRoot><wanted><title><b>간호사</b> 모집</title></wanted></wantedRoot>"
            val result = callStripNonXmlTags(xml)
            assertEquals("<wantedRoot><wanted><title>간호사 모집</title></wanted></wantedRoot>", result)
        }

        @Test
        fun `상세 API 태그도 화이트리스트에 포함`() {
            val xml = "<wantedDtl><wantedInfo><wantedTitle>전체 제목</wantedTitle></wantedInfo></wantedDtl>"
            val result = callStripNonXmlTags(xml)
            assertEquals(xml, result)
        }

        @Test
        fun `닫는 태그도 화이트리스트 검증 적용`() {
            val xml = "<wantedRoot></wantedRoot>"
            val result = callStripNonXmlTags(xml)
            assertEquals(xml, result)
        }

        @Test
        fun `화이트리스트에 없는 닫는 태그 제거`() {
            val xml = "<wantedRoot><div></div></wantedRoot>"
            val result = callStripNonXmlTags(xml)
            assertEquals("<wantedRoot></wantedRoot>", result)
        }

        @Test
        fun `태그가 없는 텍스트는 그대로 반환`() {
            val text = "plain text without tags"
            val result = callStripNonXmlTags(text)
            assertEquals(text, result)
        }

        @Test
        fun `셀프 클로징 태그도 처리`() {
            val xml = "<wantedRoot><br/><hr/><total>1</total></wantedRoot>"
            val result = callStripNonXmlTags(xml)
            assertEquals("<wantedRoot><total>1</total></wantedRoot>", result)
        }

        @Test
        fun `속성이 있는 태그 처리`() {
            val xml = "<wantedRoot><a href=\"url\">link</a><title>간호사</title></wantedRoot>"
            val result = callStripNonXmlTags(xml)
            assertEquals("<wantedRoot>link<title>간호사</title></wantedRoot>", result)
        }
    }

    // ── parseModifyDtm ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseModifyDtm()")
    inner class ParseModifyDtmTests {

        @Test
        fun `정상 포맷 yyyyMMddHHmm 파싱`() {
            val result = callParseModifyDtm("202503151430")
            assertEquals(LocalDateTime.of(2025, 3, 15, 14, 30), result)
        }

        @Test
        fun `자정 시각 파싱`() {
            val result = callParseModifyDtm("202501010000")
            assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), result)
        }

        @Test
        fun `null 입력은 null 반환`() {
            val result = callParseModifyDtm(null)
            assertNull(result)
        }

        @Test
        fun `빈 문자열은 null 반환`() {
            val result = callParseModifyDtm("")
            assertNull(result)
        }

        @Test
        fun `공백 문자열은 null 반환`() {
            val result = callParseModifyDtm("   ")
            assertNull(result)
        }

        @Test
        fun `잘못된 형식은 null 반환`() {
            val result = callParseModifyDtm("2025-03-15 14:30")
            assertNull(result)
        }

        @Test
        fun `짧은 문자열은 null 반환`() {
            val result = callParseModifyDtm("2025031")
            assertNull(result)
        }
    }

    // ── toJobFetchResult ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toJobFetchResult()")
    inner class ToJobFetchResultTests {

        @Test
        fun `정상 간호 직종 항목 변환`() {
            val item = createWantedItem()
            val result = callToJobFetchResult(item)

            assertNotNull(result)
            assertEquals("K123456", result!!.externalId)
            assertEquals("간호사 모집", result.title)
            assertEquals("테스트병원", result.companyName)
            assertEquals("3040", result.jobCategory)
            assertEquals("서울특별시 강남구", result.location)
            assertEquals(WorkRegion.SEOUL, result.workRegion)
            assertEquals("강남구", result.workDistrict)
            assertEquals(EmploymentType.FULL_TIME, result.employmentType)
            assertEquals(30000000L, result.salaryMin)
            assertEquals(40000000L, result.salaryMax)
            assertEquals("https://example.com/job/1", result.postingUrl)
        }

        @Test
        fun `jobsCd가 3040으로 시작하지 않으면 null 반환`() {
            val item = createWantedItem(jobsCd = "2010")
            val result = callToJobFetchResult(item)
            assertNull(result)
        }

        @Test
        fun `jobsCd가 null이면 null 반환`() {
            val item = createWantedItem(jobsCd = null)
            val result = callToJobFetchResult(item)
            assertNull(result)
        }

        @Test
        fun `jobsCd가 3040001 등 접두사로 시작하면 변환 성공`() {
            val item = createWantedItem(jobsCd = "3040001")
            val result = callToJobFetchResult(item)
            assertNotNull(result)
        }

        @Test
        fun `wantedAuthNo가 null이면 null 반환`() {
            val item = createWantedItem(wantedAuthNo = null)
            val result = callToJobFetchResult(item)
            assertNull(result)
        }

        @Test
        fun `title이 null이면 null 반환`() {
            val item = createWantedItem(title = null)
            val result = callToJobFetchResult(item)
            assertNull(result)
        }

        @Test
        fun `company가 null이면 null 반환`() {
            val item = createWantedItem(company = null)
            val result = callToJobFetchResult(item)
            assertNull(result)
        }

        @Test
        fun `wantedInfoUrl이 null이면 null 반환`() {
            val item = createWantedItem(wantedInfoUrl = null)
            val result = callToJobFetchResult(item)
            assertNull(result)
        }

        // ── CloseType 판정 ──

        @Test
        fun `closeDt가 null이면 ON_HIRE`() {
            val item = createWantedItem(closeDt = null)
            val result = callToJobFetchResult(item)!!

            assertEquals(CloseType.ON_HIRE, result.closeType)
            assertNull(result.expiresAt)
            assertTrue(result.isActive)
        }

        @Test
        fun `closeDt가 빈 문자열이면 ON_HIRE`() {
            val item = createWantedItem(closeDt = "")
            val result = callToJobFetchResult(item)!!

            assertEquals(CloseType.ON_HIRE, result.closeType)
            assertTrue(result.isActive)
        }

        @Test
        fun `closeDt가 채용시까지이면 ON_HIRE`() {
            val item = createWantedItem(closeDt = "채용시까지")
            val result = callToJobFetchResult(item)!!

            assertEquals(CloseType.ON_HIRE, result.closeType)
            assertNull(result.expiresAt)
            assertTrue(result.isActive)
        }

        @Test
        fun `closeDt에 채용시까지 포함되면 ON_HIRE`() {
            val item = createWantedItem(closeDt = "2025-12-31(채용시까지)")
            val result = callToJobFetchResult(item)!!

            assertEquals(CloseType.ON_HIRE, result.closeType)
            assertTrue(result.isActive)
        }

        @Test
        fun `closeDt가 미래 날짜면 FIXED이고 isActive true`() {
            val futureDate = LocalDateTime.now().plusDays(30)
            val dateStr = "${futureDate.year}-${"%02d".format(futureDate.monthValue)}-${"%02d".format(futureDate.dayOfMonth)}"
            val item = createWantedItem(closeDt = dateStr)
            val result = callToJobFetchResult(item)!!

            assertEquals(CloseType.FIXED, result.closeType)
            assertNotNull(result.expiresAt)
            assertTrue(result.isActive)
        }

        @Test
        fun `closeDt가 과거 날짜면 FIXED이고 isActive false`() {
            val item = createWantedItem(closeDt = "2020-01-01")
            val result = callToJobFetchResult(item)!!

            assertEquals(CloseType.FIXED, result.closeType)
            assertNotNull(result.expiresAt)
            assertFalse(result.isActive)
        }

        @Test
        fun `closeDt 앞뒤 공백 trim 처리`() {
            val item = createWantedItem(closeDt = "  채용시까지  ")
            val result = callToJobFetchResult(item)!!

            assertEquals(CloseType.ON_HIRE, result.closeType)
        }

        // ── 선택 필드 매핑 ──

        @Test
        fun `region이 null이면 workRegion과 workDistrict도 null`() {
            val item = createWantedItem(region = null)
            val result = callToJobFetchResult(item)!!

            assertNull(result.workRegion)
            assertNull(result.workDistrict)
        }

        @Test
        fun `empTpCd가 null이면 employmentType null`() {
            val item = createWantedItem(empTpCd = null)
            val result = callToJobFetchResult(item)!!

            assertNull(result.employmentType)
        }

        @Test
        fun `minSal과 maxSal이 null이면 salaryMin과 salaryMax null`() {
            val item = createWantedItem(minSal = null, maxSal = null)
            val result = callToJobFetchResult(item)!!

            assertNull(result.salaryMin)
            assertNull(result.salaryMax)
        }

        @Test
        fun `regDt가 null이면 postedAt null`() {
            val item = createWantedItem(regDt = null)
            val result = callToJobFetchResult(item)!!

            assertNull(result.postedAt)
        }

        @Test
        fun `careerMin과 careerMax는 항상 null`() {
            val item = createWantedItem()
            val result = callToJobFetchResult(item)!!

            assertNull(result.careerMin)
            assertNull(result.careerMax)
        }

        @Test
        fun `workHoursPerWeek는 항상 null`() {
            val item = createWantedItem()
            val result = callToJobFetchResult(item)!!

            assertNull(result.workHoursPerWeek)
        }
    }

    // ── fetchAll ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fetchAll()")
    inner class FetchAllTests {

        private lateinit var spyFetcher: Work24JobFetcher

        @BeforeEach
        fun setUp() {
            spyFetcher = spyk(Work24JobFetcher("test-auth-key"), recordPrivateCalls = true)
        }

        @Test
        fun `단일 페이지 결과 수집`() {
            val apiResponse = Work24ApiResponse(
                total = "1",
                wanted = listOf(createWantedItem(title = "간호사 채용"))
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val results = spyFetcher.fetchAll()

            assertEquals(1, results.size)
            assertEquals("간호사 채용", results[0].title)
        }

        @Test
        fun `여러 페이지 결과 수집`() {
            val page1Items = (1..100).map {
                createWantedItem(wantedAuthNo = "K${it}", title = "간호사 $it")
            }
            val page2Items = listOf(
                createWantedItem(wantedAuthNo = "K101", title = "간호사 101")
            )

            every { spyFetcher["fetchPage"](1, 100) } returns Work24ApiResponse(
                total = "101", wanted = page1Items
            )
            every { spyFetcher["fetchPage"](2, 100) } returns Work24ApiResponse(
                total = "101", wanted = page2Items
            )

            val results = spyFetcher.fetchAll()

            assertEquals(101, results.size)
        }

        @Test
        fun `빈 항목 수신 시 페이징 중단`() {
            every { spyFetcher["fetchPage"](1, 100) } returns Work24ApiResponse(
                total = "100", wanted = emptyList()
            )

            val results = spyFetcher.fetchAll()

            assertTrue(results.isEmpty())
        }

        @Test
        fun `fetchPage가 null 반환 시 페이징 중단`() {
            every { spyFetcher["fetchPage"](1, 100) } returns null

            val results = spyFetcher.fetchAll()

            assertTrue(results.isEmpty())
        }

        @Test
        fun `간호 직종이 아닌 항목은 필터링`() {
            val apiResponse = Work24ApiResponse(
                total = "3",
                wanted = listOf(
                    createWantedItem(wantedAuthNo = "K1", jobsCd = "3040"),
                    createWantedItem(wantedAuthNo = "K2", jobsCd = "2010"),
                    createWantedItem(wantedAuthNo = "K3", jobsCd = "3040001"),
                )
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val results = spyFetcher.fetchAll()

            assertEquals(2, results.size)
            assertEquals("K1", results[0].externalId)
            assertEquals("K3", results[1].externalId)
        }

        @Test
        fun `필수 필드 누락 항목은 필터링`() {
            val apiResponse = Work24ApiResponse(
                total = "3",
                wanted = listOf(
                    createWantedItem(wantedAuthNo = "K1"),
                    createWantedItem(wantedAuthNo = null),
                    createWantedItem(wantedAuthNo = "K3", company = null),
                )
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val results = spyFetcher.fetchAll()

            assertEquals(1, results.size)
            assertEquals("K1", results[0].externalId)
        }

        @Test
        fun `total 이상 수집되면 페이징 중단`() {
            val apiResponse = Work24ApiResponse(
                total = "2",
                wanted = listOf(
                    createWantedItem(wantedAuthNo = "K1"),
                    createWantedItem(wantedAuthNo = "K2"),
                )
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val results = spyFetcher.fetchAll()

            assertEquals(2, results.size)
        }

        @Test
        fun `페이지 중간에 예외 발생 시 이전까지 수집된 부분 반환`() {
            val page1Items = listOf(createWantedItem(wantedAuthNo = "K1", title = "간호사 1"))

            every { spyFetcher["fetchPage"](1, 100) } returns Work24ApiResponse(
                total = "200", wanted = page1Items
            )
            every { spyFetcher["fetchPage"](2, 100) } throws RuntimeException("API 오류")

            val results = spyFetcher.fetchAll()

            assertEquals(1, results.size)
            assertEquals("K1", results[0].externalId)
        }

        @Test
        fun `total이 null이면 0으로 처리하여 첫 페이지만 수집`() {
            val apiResponse = Work24ApiResponse(
                total = null,
                wanted = listOf(createWantedItem(wantedAuthNo = "K1"))
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val results = spyFetcher.fetchAll()

            assertEquals(1, results.size)
        }

        @Test
        fun `total이 숫자가 아니면 0으로 처리`() {
            val apiResponse = Work24ApiResponse(
                total = "invalid",
                wanted = listOf(createWantedItem(wantedAuthNo = "K1"))
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val results = spyFetcher.fetchAll()

            assertEquals(1, results.size)
        }
    }

    // ── fetchIncremental ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fetchIncremental()")
    inner class FetchIncrementalTests {

        private lateinit var spyFetcher: Work24JobFetcher

        @BeforeEach
        fun setUp() {
            spyFetcher = spyk(Work24JobFetcher("test-auth-key"), recordPrivateCalls = true)
        }

        @Test
        fun `since 이후 항목만 수집하고 이전 항목에서 중단`() {
            val since = LocalDateTime.of(2025, 3, 14, 0, 0)

            val apiResponse = Work24ApiResponse(
                total = "3",
                wanted = listOf(
                    createWantedItem(wantedAuthNo = "K1", smodifyDtm = "202503151430"),
                    createWantedItem(wantedAuthNo = "K2", smodifyDtm = "202503150900"),
                    createWantedItem(wantedAuthNo = "K3", smodifyDtm = "202503130000"),
                )
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val result = spyFetcher.fetchIncremental(since)

            assertEquals(2, result.items.size)
            assertEquals("K1", result.items[0].externalId)
            assertEquals("K2", result.items[1].externalId)
        }

        @Test
        fun `latestTimestamp는 가장 최신 modifyDtm으로 설정`() {
            val since = LocalDateTime.of(2025, 3, 10, 0, 0)

            val apiResponse = Work24ApiResponse(
                total = "2",
                wanted = listOf(
                    createWantedItem(wantedAuthNo = "K1", smodifyDtm = "202503120900"),
                    createWantedItem(wantedAuthNo = "K2", smodifyDtm = "202503151430"),
                )
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val result = spyFetcher.fetchIncremental(since)

            assertEquals(LocalDateTime.of(2025, 3, 15, 14, 30), result.latestTimestamp)
        }

        @Test
        fun `smodifyDtm이 null인 항목도 수집`() {
            val since = LocalDateTime.of(2025, 3, 14, 0, 0)

            val apiResponse = Work24ApiResponse(
                total = "2",
                wanted = listOf(
                    createWantedItem(wantedAuthNo = "K1", smodifyDtm = "202503151430"),
                    createWantedItem(wantedAuthNo = "K2", smodifyDtm = null),
                )
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val result = spyFetcher.fetchIncremental(since)

            assertEquals(2, result.items.size)
        }

        @Test
        fun `smodifyDtm이 since와 같으면 중단`() {
            val since = LocalDateTime.of(2025, 3, 15, 14, 30)

            val apiResponse = Work24ApiResponse(
                total = "2",
                wanted = listOf(
                    createWantedItem(wantedAuthNo = "K1", smodifyDtm = "202503151430"),
                    createWantedItem(wantedAuthNo = "K2", smodifyDtm = "202503151430"),
                )
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val result = spyFetcher.fetchIncremental(since)

            assertTrue(result.items.isEmpty())
        }

        @Test
        fun `모든 항목이 since 이후이면 다음 페이지도 수집`() {
            val since = LocalDateTime.of(2025, 3, 10, 0, 0)

            every { spyFetcher["fetchPage"](1, 100) } returns Work24ApiResponse(
                total = "2",
                wanted = listOf(createWantedItem(wantedAuthNo = "K1", smodifyDtm = "202503151430"))
            )
            every { spyFetcher["fetchPage"](2, 100) } returns Work24ApiResponse(
                total = "2",
                wanted = emptyList()
            )

            val result = spyFetcher.fetchIncremental(since)

            assertEquals(1, result.items.size)
        }

        @Test
        fun `빈 응답이면 바로 중단`() {
            every { spyFetcher["fetchPage"](1, 100) } returns Work24ApiResponse(
                total = "0", wanted = emptyList()
            )

            val result = spyFetcher.fetchIncremental(LocalDateTime.now())

            assertTrue(result.items.isEmpty())
            assertNull(result.latestTimestamp)
        }

        @Test
        fun `예외 발생 시 이전까지 수집된 결과 반환`() {
            val since = LocalDateTime.of(2025, 3, 10, 0, 0)

            every { spyFetcher["fetchPage"](1, 100) } returns Work24ApiResponse(
                total = "200",
                wanted = listOf(createWantedItem(wantedAuthNo = "K1", smodifyDtm = "202503151430"))
            )
            every { spyFetcher["fetchPage"](2, 100) } throws RuntimeException("API 오류")

            val result = spyFetcher.fetchIncremental(since)

            assertEquals(1, result.items.size)
            assertNotNull(result.latestTimestamp)
        }

        @Test
        fun `간호 직종 아닌 항목은 필터링되지만 스캔은 계속`() {
            val since = LocalDateTime.of(2025, 3, 10, 0, 0)

            val apiResponse = Work24ApiResponse(
                total = "2",
                wanted = listOf(
                    createWantedItem(wantedAuthNo = "K1", jobsCd = "2010", smodifyDtm = "202503151430"),
                    createWantedItem(wantedAuthNo = "K2", jobsCd = "3040", smodifyDtm = "202503151400"),
                )
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val result = spyFetcher.fetchIncremental(since)

            assertEquals(1, result.items.size)
            assertEquals("K2", result.items[0].externalId)
        }

        @Test
        fun `latestTimestamp는 필터된 항목의 modifyDtm도 포함하지 않음`() {
            val since = LocalDateTime.of(2025, 3, 10, 0, 0)

            val apiResponse = Work24ApiResponse(
                total = "1",
                wanted = listOf(
                    createWantedItem(
                        wantedAuthNo = "K1",
                        jobsCd = "2010",
                        smodifyDtm = "202503151430"
                    ),
                )
            )
            every { spyFetcher["fetchPage"](1, 100) } returns apiResponse

            val result = spyFetcher.fetchIncremental(since)

            assertTrue(result.items.isEmpty())
            assertNull(result.latestTimestamp)
        }
    }

    // ── enrichTruncatedTitles ───────────────────────────────────────────────────

    @Nested
    @DisplayName("enrichTruncatedTitles()")
    inner class EnrichTruncatedTitlesTests {

        @Test
        fun `잘린 제목이 없으면 원본 그대로 반환`() {
            val results = listOf(
                createJobFetchResult(externalId = "K1", title = "간호사 채용"),
                createJobFetchResult(externalId = "K2", title = "간호조무사 채용"),
            )

            val enriched = callEnrichTruncatedTitles(results)

            assertEquals(2, enriched.size)
            assertEquals("간호사 채용", enriched[0].title)
            assertEquals("간호조무사 채용", enriched[1].title)
        }

        @Test
        fun `빈 리스트는 그대로 반환`() {
            val enriched = callEnrichTruncatedTitles(emptyList())
            assertTrue(enriched.isEmpty())
        }

        @Test
        fun `잘린 제목이 있지만 상세 API 실패 시 원본 제목 유지`() {
            val spyFetcher = spyk(Work24JobFetcher("test-auth-key"), recordPrivateCalls = true)
            every { spyFetcher["fetchDetailTitle"](any<String>()) } returns null

            val results = listOf(
                createJobFetchResult(externalId = "K1", title = "간호사 채용..."),
            )

            val method = Work24JobFetcher::class.java.getDeclaredMethod("enrichTruncatedTitles", List::class.java)
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val enriched = method.invoke(spyFetcher, results) as List<JobFetchResult>

            assertEquals(1, enriched.size)
            assertEquals("간호사 채용...", enriched[0].title)
        }

        @Test
        fun `잘린 제목 보정 성공 시 전체 제목으로 교체`() {
            val spyFetcher = spyk(Work24JobFetcher("test-auth-key"), recordPrivateCalls = true)
            every { spyFetcher["fetchDetailTitle"]("K1") } returns "전체 간호사 채용 공고 제목"

            val results = listOf(
                createJobFetchResult(externalId = "K1", title = "전체 간호사 채용..."),
            )

            val method = Work24JobFetcher::class.java.getDeclaredMethod("enrichTruncatedTitles", List::class.java)
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val enriched = method.invoke(spyFetcher, results) as List<JobFetchResult>

            assertEquals(1, enriched.size)
            assertEquals("전체 간호사 채용 공고 제목", enriched[0].title)
        }

        @Test
        fun `잘린 제목과 정상 제목이 섞여 있으면 잘린 것만 보정`() {
            val spyFetcher = spyk(Work24JobFetcher("test-auth-key"), recordPrivateCalls = true)
            every { spyFetcher["fetchDetailTitle"]("K1") } returns "전체 제목 1"

            val results = listOf(
                createJobFetchResult(externalId = "K1", title = "잘린 제목..."),
                createJobFetchResult(externalId = "K2", title = "정상 제목"),
            )

            val method = Work24JobFetcher::class.java.getDeclaredMethod("enrichTruncatedTitles", List::class.java)
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val enriched = method.invoke(spyFetcher, results) as List<JobFetchResult>

            assertEquals(2, enriched.size)
            assertEquals("전체 제목 1", enriched[0].title)
            assertEquals("정상 제목", enriched[1].title)
        }

        @Test
        fun `일부만 보정 성공하면 실패한 것은 원본 유지`() {
            val spyFetcher = spyk(Work24JobFetcher("test-auth-key"), recordPrivateCalls = true)
            every { spyFetcher["fetchDetailTitle"]("K1") } returns "전체 제목 1"
            every { spyFetcher["fetchDetailTitle"]("K2") } returns null

            val results = listOf(
                createJobFetchResult(externalId = "K1", title = "잘린 1..."),
                createJobFetchResult(externalId = "K2", title = "잘린 2..."),
            )

            val method = Work24JobFetcher::class.java.getDeclaredMethod("enrichTruncatedTitles", List::class.java)
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val enriched = method.invoke(spyFetcher, results) as List<JobFetchResult>

            assertEquals("전체 제목 1", enriched[0].title)
            assertEquals("잘린 2...", enriched[1].title)
        }
    }
}
