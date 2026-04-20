package duit.server.infrastructure.external.job.work24

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DisplayName("Work24 API 응답 XML 파싱 테스트")
class Work24ApiResponseTest {

    private val xmlMapper = XmlMapper().apply {
        registerKotlinModule()
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @Nested
    @DisplayName("목록 API (Work24ApiResponse)")
    inner class ListApiResponseTests {

        @Test
        fun `정상 XML 응답 — 복수 항목 파싱`() {
            val xml = """
                <wantedRoot>
                    <total>2</total>
                    <startPage>1</startPage>
                    <display>100</display>
                    <wanted>
                        <wantedAuthNo>K123456</wantedAuthNo>
                        <company>테스트병원</company>
                        <busino>123-45-67890</busino>
                        <indTpNm>보건업</indTpNm>
                        <title>간호사 모집</title>
                        <salTpNm>연봉</salTpNm>
                        <minSal>30000000</minSal>
                        <maxSal>40000000</maxSal>
                        <region>서울특별시 강남구</region>
                        <minEdubg>05</minEdubg>
                        <closeDt>2025-12-31</closeDt>
                        <infoSvc>VALIDATION</infoSvc>
                        <wantedInfoUrl>https://example.com/1</wantedInfoUrl>
                        <zipCd>06123</zipCd>
                        <strtnmCd>서울특별시 강남구 테헤란로 1</strtnmCd>
                        <basicAddr>서울특별시 강남구</basicAddr>
                        <detailAddr>101동 202호</detailAddr>
                        <empTpCd>10</empTpCd>
                        <jobsCd>3040</jobsCd>
                        <smodifyDtm>202503151430</smodifyDtm>
                    </wanted>
                    <wanted>
                        <wantedAuthNo>K789012</wantedAuthNo>
                        <company>다른병원</company>
                        <title>간호조무사 모집</title>
                        <wantedInfoUrl>https://example.com/2</wantedInfoUrl>
                        <jobsCd>3040001</jobsCd>
                    </wanted>
                </wantedRoot>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24ApiResponse::class.java)

            assertEquals("2", response.total)
            assertEquals("1", response.startPage)
            assertEquals("100", response.display)
            assertEquals(2, response.wanted?.size)

            val first = response.wanted!![0]
            assertEquals("K123456", first.wantedAuthNo)
            assertEquals("테스트병원", first.company)
            assertEquals("123-45-67890", first.busino)
            assertEquals("보건업", first.indTpNm)
            assertEquals("간호사 모집", first.title)
            assertEquals("연봉", first.salTpNm)
            assertEquals("30000000", first.minSal)
            assertEquals("40000000", first.maxSal)
            assertEquals("서울특별시 강남구", first.region)
            assertEquals("05", first.minEdubg)
            assertEquals("2025-12-31", first.closeDt)
            assertEquals("VALIDATION", first.infoSvc)
            assertEquals("https://example.com/1", first.wantedInfoUrl)
            assertEquals("06123", first.zipCd)
            assertEquals("서울특별시 강남구 테헤란로 1", first.strtnmCd)
            assertEquals("서울특별시 강남구", first.basicAddr)
            assertEquals("101동 202호", first.detailAddr)
            assertEquals("10", first.empTpCd)
            assertEquals("3040", first.jobsCd)
            assertEquals("202503151430", first.smodifyDtm)

            val second = response.wanted!![1]
            assertEquals("K789012", second.wantedAuthNo)
            assertEquals("다른병원", second.company)
        }

        @Test
        fun `단일 항목 응답 파싱`() {
            val xml = """
                <wantedRoot>
                    <total>1</total>
                    <wanted>
                        <wantedAuthNo>K111111</wantedAuthNo>
                        <company>단일병원</company>
                        <title>간호사</title>
                        <jobsCd>3040</jobsCd>
                    </wanted>
                </wantedRoot>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24ApiResponse::class.java)

            assertEquals("1", response.total)
            assertEquals(1, response.wanted?.size)
            assertEquals("K111111", response.wanted!![0].wantedAuthNo)
        }

        @Test
        fun `빈 목록 응답 — wanted 없음`() {
            val xml = """
                <wantedRoot>
                    <total>0</total>
                    <startPage>1</startPage>
                    <display>100</display>
                </wantedRoot>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24ApiResponse::class.java)

            assertEquals("0", response.total)
            assertNull(response.wanted)
        }

        @Test
        fun `알 수 없는 필드가 포함되어도 정상 파싱`() {
            val xml = """
                <wantedRoot>
                    <total>1</total>
                    <unknownField>value</unknownField>
                    <wanted>
                        <wantedAuthNo>K111111</wantedAuthNo>
                        <unknownItemField>무시됨</unknownItemField>
                        <company>병원</company>
                        <title>간호사</title>
                        <jobsCd>3040</jobsCd>
                    </wanted>
                </wantedRoot>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24ApiResponse::class.java)

            assertEquals(1, response.wanted?.size)
            assertEquals("K111111", response.wanted!![0].wantedAuthNo)
        }

        @Test
        fun `모든 nullable 필드가 없어도 파싱 가능`() {
            val xml = """
                <wantedRoot>
                    <wanted>
                        <wantedAuthNo>K111111</wantedAuthNo>
                    </wanted>
                </wantedRoot>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24ApiResponse::class.java)

            assertNull(response.total)
            assertNull(response.startPage)
            assertNull(response.display)
            assertEquals(1, response.wanted?.size)

            val item = response.wanted!![0]
            assertEquals("K111111", item.wantedAuthNo)
            assertNull(item.company)
            assertNull(item.busino)
            assertNull(item.indTpNm)
            assertNull(item.title)
            assertNull(item.salTpNm)
            assertNull(item.minSal)
            assertNull(item.maxSal)
            assertNull(item.region)
            assertNull(item.minEdubg)
            assertNull(item.closeDt)
            assertNull(item.infoSvc)
            assertNull(item.wantedInfoUrl)
            assertNull(item.zipCd)
            assertNull(item.strtnmCd)
            assertNull(item.basicAddr)
            assertNull(item.detailAddr)
            assertNull(item.empTpCd)
            assertNull(item.jobsCd)
            assertNull(item.smodifyDtm)
        }
    }

    @Nested
    @DisplayName("상세 API (Work24DetailResponse)")
    inner class DetailApiResponseTests {

        @Test
        fun `정상 상세 응답 — wantedTitle 추출`() {
            val xml = """
                <wantedDtl>
                    <wantedInfo>
                        <wantedTitle>전체 간호사 채용 공고 제목입니다</wantedTitle>
                    </wantedInfo>
                </wantedDtl>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24DetailResponse::class.java)

            assertNotNull(response.wantedInfo)
            assertEquals("전체 간호사 채용 공고 제목입니다", response.wantedInfo?.wantedTitle)
        }

        @Test
        fun `wantedInfo가 없으면 null`() {
            val xml = """
                <wantedDtl>
                </wantedDtl>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24DetailResponse::class.java)

            assertNull(response.wantedInfo)
        }

        @Test
        fun `wantedTitle이 없으면 null`() {
            val xml = """
                <wantedDtl>
                    <wantedInfo>
                    </wantedInfo>
                </wantedDtl>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24DetailResponse::class.java)

            assertNotNull(response.wantedInfo)
            assertNull(response.wantedInfo?.wantedTitle)
        }

        @Test
        fun `알 수 없는 필드 무시`() {
            val xml = """
                <wantedDtl>
                    <wantedInfo>
                        <wantedTitle>간호사 채용</wantedTitle>
                        <otherField>무시됨</otherField>
                    </wantedInfo>
                    <otherSection>무시됨</otherSection>
                </wantedDtl>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24DetailResponse::class.java)

            assertEquals("간호사 채용", response.wantedInfo?.wantedTitle)
        }
    }

    @Nested
    @DisplayName("목록 API와 상세 API 구조 차이")
    inner class ListVsDetailStructureTests {

        @Test
        fun `목록 XML을 상세 클래스로 파싱하면 wantedInfo가 null`() {
            val listXml = """
                <wantedRoot>
                    <total>1</total>
                    <wanted>
                        <wantedAuthNo>K111</wantedAuthNo>
                        <title>간호사</title>
                    </wanted>
                </wantedRoot>
            """.trimIndent()

            val detailResponse = xmlMapper.readValue(listXml, Work24DetailResponse::class.java)
            assertNull(detailResponse.wantedInfo)
        }

        @Test
        fun `상세 XML을 목록 클래스로 파싱하면 wanted가 null`() {
            val detailXml = """
                <wantedDtl>
                    <wantedInfo>
                        <wantedTitle>간호사 채용</wantedTitle>
                    </wantedInfo>
                </wantedDtl>
            """.trimIndent()

            val listResponse = xmlMapper.readValue(detailXml, Work24ApiResponse::class.java)
            assertNull(listResponse.wanted)
            assertNull(listResponse.total)
        }
    }
}
