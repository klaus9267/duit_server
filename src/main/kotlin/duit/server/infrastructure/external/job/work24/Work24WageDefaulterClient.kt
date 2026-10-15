package duit.server.infrastructure.external.job.work24

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

/** 임금체불 명단공개 사업주 조회 결과 */
sealed interface WageDefaulterCheckResult {
    /** listed = true 면 명단공개 대상 */
    data class Checked(val listed: Boolean) : WageDefaulterCheckResult

    /**
     * 연계 실패·오류 코드·파싱 실패 등으로 판정할 수 없는 경우 (기존 값 유지).
     * apiFailure 는 회사와 무관한 API 장애(요청 실패·오류 코드·비정상 응답)로, 이어서 조회해도 실패할 가능성이 높다.
     */
    data class Unknown(
        val reason: String,
        val errorCode: String? = null,
        val apiFailure: Boolean = errorCode != null,
    ) : WageDefaulterCheckResult
}

/**
 * 고용24 임금체불 명단공개 사업주 조회 API.
 * 사업자등록번호(brno)만 보내고, 개인정보인 대표자명(rpstPnm) 등 선택 파라미터는 보내지 않는다.
 */
@Component
class Work24WageDefaulterClient(
    @Value("\${api.work24.wage-defaulter.auth-key:}") wageDefaulterKey: String,
    @Value("\${api.work24.auth-key:}") work24Key: String,
) {
    private val authKey = wageDefaulterKey.ifBlank { work24Key }.trim()

    val isEnabled: Boolean get() = authKey.isNotEmpty()

    private val xmlMapper = XmlMapper()

    private val restClient = RestClient.builder()
        .baseUrl("https://www.work24.go.kr")
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofSeconds(5))
                setReadTimeout(Duration.ofSeconds(10))
            }
        )
        .build()

    companion object {
        private const val PATH = "/cm/openApi/call/wk/callOpenApiSvcInfo210L41.do"
    }

    fun check(businessNumber: String): WageDefaulterCheckResult =
        runCatching { fetchXml(businessNumber) }
            .fold(
                onSuccess = { body ->
                    body?.let(::parse) ?: WageDefaulterCheckResult.Unknown("빈 응답", apiFailure = true)
                },
                // 예외 메시지에 authKey 가 포함된 요청 URL 이 들어갈 수 있어 예외 종류만 남긴다
                onFailure = {
                    WageDefaulterCheckResult.Unknown("요청 실패: ${it.javaClass.simpleName}", apiFailure = true)
                },
            )

    fun parse(xml: String): WageDefaulterCheckResult {
        val root = runCatching { xmlMapper.readTree(xml) }.getOrNull()
            ?: return WageDefaulterCheckResult.Unknown("응답 XML 파싱 실패", apiFailure = true)
        val item = root.get("jdgnList") ?: root

        val errorCode = item.textOf("errMsgCd")
        val linked = item.textOf("lnkSucsYn")
        val judgement = item.textOf("judgReltYn")

        return when {
            errorCode != null -> WageDefaulterCheckResult.Unknown("오류 응답: ${item.textOf("errMsg")}", errorCode)
            linked == null -> WageDefaulterCheckResult.Unknown("응답 형식 오류: lnkSucsYn 없음", apiFailure = true)
            linked != "Y" -> WageDefaulterCheckResult.Unknown("연계 실패: lnkSucsYn=$linked")
            judgement == "Y" -> WageDefaulterCheckResult.Checked(listed = true)
            judgement == "N" -> WageDefaulterCheckResult.Checked(listed = false)
            else -> WageDefaulterCheckResult.Unknown("판정 결과 없음: judgReltYn=$judgement")
        }
    }

    private fun fetchXml(businessNumber: String): String? =
        restClient.get()
            .uri { builder ->
                builder.path(PATH)
                    .queryParam("authKey", authKey)
                    .queryParam("returnType", "XML")
                    .queryParam("brno", businessNumber)
                    .build()
            }
            .retrieve()
            .body(String::class.java)

    private fun JsonNode.textOf(field: String): String? =
        get(field)?.asText()?.trim()?.takeIf { it.isNotEmpty() }
}
