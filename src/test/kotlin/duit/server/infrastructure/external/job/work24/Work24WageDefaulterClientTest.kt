package duit.server.infrastructure.external.job.work24

import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.client.ResourceAccessException

@DisplayName("Work24WageDefaulterClient 단위 테스트")
class Work24WageDefaulterClientTest {

    private val client = Work24WageDefaulterClient(wageDefaulterKey = "wage-key", work24Key = "")

    private fun response(
        lnkSucsYn: String = "Y",
        judgReltYn: String = "N",
        errMsgCd: String = "",
        errMsg: String = "",
    ) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <jdgnList>
            <lnkReqNo>REQ-1</lnkReqNo>
            <lnkSucsYn>$lnkSucsYn</lnkSucsYn>
            <brno>1234567890</brno>
            <bzmn>테스트병원</bzmn>
            <judgReltYn>$judgReltYn</judgReltYn>
            <errMsgCd>$errMsgCd</errMsgCd>
            <errMsg>$errMsg</errMsg>
        </jdgnList>
    """.trimIndent()

    @Nested
    @DisplayName("parse()")
    inner class ParseTests {

        @Test
        fun `judgReltYn 이 Y 면 명단공개 대상이다`() {
            assertEquals(WageDefaulterCheckResult.Checked(listed = true), client.parse(response(judgReltYn = "Y")))
        }

        @Test
        fun `judgReltYn 이 N 이면 명단공개 미대상이다`() {
            assertEquals(WageDefaulterCheckResult.Checked(listed = false), client.parse(response(judgReltYn = "N")))
        }

        @Test
        fun `오류 코드가 있으면 판정하지 않는다`() {
            val result = client.parse(response(lnkSucsYn = "N", judgReltYn = "", errMsgCd = "ERR0001", errMsg = "API KEY 오류"))

            assertTrue(result is WageDefaulterCheckResult.Unknown)
            assertEquals("ERR0001", (result as WageDefaulterCheckResult.Unknown).errorCode)
            assertTrue(result.apiFailure)
        }

        @Test
        fun `연계에 실패하면 판정 값이 있어도 판정하지 않는다`() {
            val result = client.parse(response(lnkSucsYn = "N", judgReltYn = "Y"))

            assertTrue(result is WageDefaulterCheckResult.Unknown)
            assertFalse((result as WageDefaulterCheckResult.Unknown).apiFailure)
        }

        @Test
        fun `판정 값이 비어 있으면 판정하지 않는다`() {
            val result = client.parse(response(judgReltYn = ""))

            assertTrue(result is WageDefaulterCheckResult.Unknown)
            assertFalse((result as WageDefaulterCheckResult.Unknown).apiFailure)
        }

        @Test
        fun `깨진 XML 이나 다른 형식의 응답은 API 장애로 보고 판정하지 않는다`() {
            listOf("<jdgnList><lnkSucsYn>Y</lnkSucsYn>", "not xml", "<html><body>점검 중</body></html>")
                .forEach { body ->
                    val result = client.parse(body)
                    assertTrue(result is WageDefaulterCheckResult.Unknown, body)
                    assertTrue((result as WageDefaulterCheckResult.Unknown).apiFailure, body)
                }
        }

        @Test
        fun `상위 요소로 감싼 응답도 읽는다`() {
            val wrapped = "<root><jdgnList><lnkSucsYn>Y</lnkSucsYn><judgReltYn>Y</judgReltYn></jdgnList></root>"

            assertEquals(WageDefaulterCheckResult.Checked(listed = true), client.parse(wrapped))
        }
    }

    @Nested
    @DisplayName("check()")
    inner class CheckTests {

        @Test
        fun `응답 XML 을 판정 결과로 변환한다`() {
            val spyClient = spyk(client, recordPrivateCalls = true)
            every { spyClient["fetchXml"]("1234567890") } returns response(judgReltYn = "Y")

            assertEquals(WageDefaulterCheckResult.Checked(listed = true), spyClient.check("1234567890"))
        }

        @Test
        fun `요청이 실패하면 판정하지 않는다`() {
            val spyClient = spyk(client, recordPrivateCalls = true)
            every { spyClient["fetchXml"]("1234567890") } throws ResourceAccessException("timeout")

            val result = spyClient.check("1234567890")
            assertTrue(result is WageDefaulterCheckResult.Unknown)
            assertTrue((result as WageDefaulterCheckResult.Unknown).apiFailure)
        }
    }

    @Nested
    @DisplayName("API 키")
    inner class AuthKeyTests {

        @Test
        fun `전용 키가 없으면 기존 고용24 키를 사용한다`() {
            assertTrue(Work24WageDefaulterClient(wageDefaulterKey = "", work24Key = "work24-key").isEnabled)
        }

        @Test
        fun `키가 모두 없으면 비활성화된다`() {
            assertFalse(Work24WageDefaulterClient(wageDefaulterKey = " ", work24Key = "").isEnabled)
        }
    }
}
