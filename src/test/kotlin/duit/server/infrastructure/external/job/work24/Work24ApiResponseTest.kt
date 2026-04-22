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
    }

    @Nested
    @DisplayName("상세 API (Work24DetailResponse)")
    inner class DetailApiResponseTests {

        @Test
        fun `정상 상세 응답 — corpInfo와 wantedInfo와 empchargeInfo 모두 파싱`() {
            val xml = """
                <wantedDtl>
                    <wantedAuthNo>K130112604210072</wantedAuthNo>
                    <corpInfo>
                        <corpNm>해운대한빛요양병원</corpNm>
                        <reperNm>장미화</reperNm>
                        <totPsncnt>100 명</totPsncnt>
                        <capitalAmt>0 백만원</capitalAmt>
                        <yrSalesAmt>0 백만원</yrSalesAmt>
                        <indTpCdNm>그 외 기타 보건업</indTpCdNm>
                        <busiCont>노령환자 입원진료</busiCont>
                        <corpAddr>48034 부산광역시 해운대구 반여로 156</corpAddr>
                        <homePg></homePg>
                        <busiSize></busiSize>
                    </corpInfo>
                    <wantedInfo>
                        <jobsNm>일반 간호사(304002)</jobsNm>
                        <wantedTitle>수간호사 구인합니다(일반병동)</wantedTitle>
                        <relJobsNm>간호사 </relJobsNm>
                        <jobCont>병동 간호 업무</jobCont>
                        <receiptCloseDt>채용시까지</receiptCloseDt>
                        <empTpNm>기간의 정함이 없는 근로계약</empTpNm>
                        <collectPsncnt>1</collectPsncnt>
                        <salTpNm>연봉39,000,000원 이상,</salTpNm>
                        <enterTpNm>경력 (최소5년) 우대</enterTpNm>
                        <eduNm>학력무관</eduNm>
                        <certificate>간호사</certificate>
                        <mltsvcExcHope>비희망</mltsvcExcHope>
                        <selMthd>서류,면접</selMthd>
                        <rcptMthd>방문,팩스,고용24,이메일</rcptMthd>
                        <submitDoc>이력서,경력증명서</submitDoc>
                        <workRegion>(48034) 부산광역시 해운대구 반여로 156</workRegion>
                        <workdayWorkhrCont>평일 : (근무시간) 7시 ~ 16시</workdayWorkhrCont>
                        <fourIns>국민연금 고용보험 산재보험 의료보험</fourIns>
                        <retirepay>퇴직연금</retirepay>
                        <dtlRecrContUrl>https://www.work24.go.kr/detail?wantedAuthNo=K130112604210072</dtlRecrContUrl>
                        <jobsCd>304002</jobsCd>
                        <minEdubgIcd>00</minEdubgIcd>
                        <maxEdubgIcd>00</maxEdubgIcd>
                        <regionCd>26350</regionCd>
                        <empTpCd>10</empTpCd>
                        <enterTpCd>E</enterTpCd>
                        <salTpCd>Y</salTpCd>
                    </wantedInfo>
                    <empchargeInfo>
                        <empChargerDpt></empChargerDpt>
                        <contactTelno>051-929-2000</contactTelno>
                        <chargerFaxNo>051-929-2002</chargerFaxNo>
                    </empchargeInfo>
                </wantedDtl>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24DetailResponse::class.java)

            assertEquals("K130112604210072", response.wantedAuthNo)

            val corp = response.corpInfo
            assertNotNull(corp)
            assertEquals("해운대한빛요양병원", corp.corpNm)
            assertEquals("장미화", corp.reperNm)
            assertEquals("100 명", corp.totPsncnt)
            assertEquals("0 백만원", corp.capitalAmt)
            assertEquals("그 외 기타 보건업", corp.indTpCdNm)
            assertEquals("노령환자 입원진료", corp.busiCont)
            assertEquals("48034 부산광역시 해운대구 반여로 156", corp.corpAddr)

            val info = response.wantedInfo
            assertNotNull(info)
            assertEquals("일반 간호사(304002)", info.jobsNm)
            assertEquals("수간호사 구인합니다(일반병동)", info.wantedTitle)
            assertEquals("채용시까지", info.receiptCloseDt)
            assertEquals("1", info.collectPsncnt)
            assertEquals("연봉39,000,000원 이상,", info.salTpNm)
            assertEquals("경력 (최소5년) 우대", info.enterTpNm)
            assertEquals("학력무관", info.eduNm)
            assertEquals("간호사", info.certificate)
            assertEquals("서류,면접", info.selMthd)
            assertEquals("국민연금 고용보험 산재보험 의료보험", info.fourIns)
            assertEquals("304002", info.jobsCd)
            assertEquals("10", info.empTpCd)
            assertEquals("Y", info.salTpCd)

            val charge = response.empchargeInfo
            assertNotNull(charge)
            assertEquals("051-929-2000", charge.contactTelno)
            assertEquals("051-929-2002", charge.chargerFaxNo)
        }

        @Test
        fun `wantedInfo가 없으면 null`() {
            val xml = """
                <wantedDtl>
                </wantedDtl>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24DetailResponse::class.java)

            assertNull(response.wantedInfo)
            assertNull(response.corpInfo)
            assertNull(response.empchargeInfo)
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

        @Test
        fun `정보 없음 응답 파싱 — wantedInfo corpInfo 모두 null`() {
            val xml = """
                <wantedDtl>
                    <message>정보가 존재하지 않습니다</message>
                    <messageCd>018</messageCd>
                </wantedDtl>
            """.trimIndent()

            val response = xmlMapper.readValue(xml, Work24DetailResponse::class.java)

            assertNull(response.wantedInfo)
            assertNull(response.corpInfo)
            assertEquals("정보가 존재하지 않습니다", response.message)
            assertEquals("018", response.messageCd)
        }
    }
}
