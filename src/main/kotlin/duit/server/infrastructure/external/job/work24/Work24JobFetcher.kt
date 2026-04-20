package duit.server.infrastructure.external.job.work24

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.SourceType
import duit.server.infrastructure.external.job.JobFetcher
import duit.server.infrastructure.external.job.dto.IncrementalFetchResult
import duit.server.infrastructure.external.job.dto.JobFetchResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class Work24JobFetcher(
    @Value("\${api.work24.auth-key:}") private val authKey: String
) : JobFetcher {

    override val sourceType = SourceType.WORK24

    private val logger = LoggerFactory.getLogger(Work24JobFetcher::class.java)

    private val xmlMapper = XmlMapper().apply {
        registerKotlinModule()
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private val restClient = RestClient.builder()
        .baseUrl("https://www.work24.go.kr")
        .build()

    private val modifyDtmFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

    companion object {
        private val ALLOWED_NURSE_JOB_CODES = setOf(
            "304000",
            "304001",
            "304002",
            "307500",
        )
    }

    override fun fetchAll(): List<JobFetchResult> {
        if (authKey.isBlank()) {
            logger.warn("Work24 auth key is not configured. Skipping fetch.")
            return emptyList()
        }

        val results = mutableListOf<JobFetchResult>()
        var startPage = 1
        val display = 100
        val maxPages = 100

        try {
            while (startPage <= maxPages) {
                val apiResponse = fetchPageWithRetry(startPage, display) ?: break
                val total = apiResponse.total?.toLongOrNull() ?: 0L
                val items = apiResponse.wanted ?: emptyList()

                if (items.isEmpty()) break

                results.addAll(items.mapNotNull { it.toJobFetchResult() })

                val fetched = (startPage - 1) * display + items.size
                logger.info("Work24 fetchAll: {}/{} 건 수집 (page {})", fetched, total, startPage)
                if (fetched >= total) break

                startPage++
            }
        } catch (e: Exception) {
            logger.error("Work24 fetchAll: page ${startPage}에서 에러 발생, ${results.size}건 부분 반환", e)
        }

        return enrichTruncatedTitles(results)
    }

    override fun fetchIncremental(since: LocalDateTime): IncrementalFetchResult {
        if (authKey.isBlank()) {
            logger.warn("Work24 auth key is not configured. Skipping incremental fetch.")
            return IncrementalFetchResult(emptyList(), null)
        }

        val results = mutableListOf<JobFetchResult>()
        var latestTimestamp: LocalDateTime? = null
        var startPage = 1
        val display = 100
        val maxPages = 100
        var earlyTerminated = false

        try {
            while (startPage <= maxPages && !earlyTerminated) {
                val apiResponse = fetchPageWithRetry(startPage, display) ?: break
                val items = apiResponse.wanted ?: emptyList()

                if (items.isEmpty()) break

                for (item in items) {
                    val modifyDtm = parseModifyDtm(item.smodifyDtm)

                    if (modifyDtm != null && !modifyDtm.isAfter(since)) {
                        earlyTerminated = true
                        break
                    }

                    val fetchResult = item.toJobFetchResult() ?: continue
                    results.add(fetchResult)

                    if (modifyDtm != null && (latestTimestamp == null || modifyDtm.isAfter(latestTimestamp))) {
                        latestTimestamp = modifyDtm
                    }
                }

                startPage++
            }
        } catch (e: Exception) {
            logger.error("Work24 fetchIncremental: page ${startPage}에서 에러 발생, ${results.size}건 부분 반환", e)
        }

        logger.info("Work24 incremental: ${results.size}건 수집, ${startPage - 1}페이지 스캔, earlyTerminated=$earlyTerminated")
        return IncrementalFetchResult(enrichTruncatedTitles(results), latestTimestamp)
    }

    private fun fetchPage(startPage: Int, display: Int): Work24ApiResponse? {
        val responseBody = restClient.get()
            .uri { builder ->
                builder.path("/cm/openApi/call/wk/callOpenApiSvcInfo210L01.do")
                    .queryParam("authKey", authKey)
                    .queryParam("callTp", "L")
                    .queryParam("returnType", "XML")
                    .queryParam("display", display)
                    .queryParam("keyword", "간호")
                    .queryParam("startPage", startPage)
                    .build()
            }
            .retrieve()
            .body(String::class.java) ?: return null

        val sanitized = stripNonXmlTags(responseBody)
        return xmlMapper.readValue(sanitized, Work24ApiResponse::class.java)
    }

    private val xmlElementWhitelist = setOf(
        // 목록 API (callTp=L)
        "wantedRoot", "total", "startPage", "display", "wanted",
        "wantedAuthNo", "company", "busino", "indTpNm", "title", "salTpNm", "sal", "minSal", "maxSal",
        "region", "holidayTpNm", "minEdubg", "maxEdubg", "career",
        "regDt", "closeDt", "infoSvc", "wantedInfoUrl", "wantedMobileInfoUrl",
        "zipCd", "strtnmCd", "basicAddr", "detailAddr",
        "empTpCd", "jobsCd", "smodifyDtm",
        // 상세 API (callTp=D)
        "wantedDtl", "wantedInfo", "wantedTitle",
    )

    private val tagPattern = Regex("</?([a-zA-Z][a-zA-Z0-9]*)[^>]*>")

    private fun stripNonXmlTags(xml: String): String =
        tagPattern.replace(xml) { match ->
            if (match.groupValues[1] in xmlElementWhitelist) match.value else ""
        }

    private fun <T> fetchWithRetry(description: String, maxRetries: Int = 2, action: () -> T?): T? {
        repeat(maxRetries + 1) { attempt ->
            try {
                return action()
            } catch (e: Exception) {
                if (attempt == maxRetries) {
                    logger.warn("Work24 {} 실패", description, e)
                    return null
                }
                Thread.sleep(500L * (attempt + 1))
            }
        }
        return null
    }

    private fun fetchPageWithRetry(startPage: Int, display: Int): Work24ApiResponse? =
        fetchWithRetry("page $startPage fetch") { fetchPage(startPage, display) }

    private fun fetchDetailTitle(wantedAuthNo: String): String? =
        fetchWithRetry("상세 API (wantedAuthNo=$wantedAuthNo)") {
            val responseBody = restClient.get()
                .uri { builder ->
                    builder.path("/cm/openApi/call/wk/callOpenApiSvcInfo210L01.do")
                        .queryParam("authKey", authKey)
                        .queryParam("callTp", "D")
                        .queryParam("returnType", "XML")
                        .queryParam("wantedAuthNo", wantedAuthNo)
                        .queryParam("infoSvc", "VALIDATION")
                        .build()
                }
                .retrieve()
                .body(String::class.java) ?: return@fetchWithRetry null

            val sanitized = stripNonXmlTags(responseBody)
            val response = xmlMapper.readValue(sanitized, Work24DetailResponse::class.java)
            response.wantedInfo?.wantedTitle
        }

    private fun enrichTruncatedTitles(results: List<JobFetchResult>): List<JobFetchResult> {
        val truncated = results.filter { it.title.contains("...") }
        if (truncated.isEmpty()) return results

        logger.info("Work24: 잘린 제목 {}건 보정 시작", truncated.size)

        return try {
            val total = truncated.size
            val enrichedTitles = truncated.mapIndexed { index, item ->
                if ((index + 1) % 100 == 0) {
                    logger.info("Work24: 제목 보정 진행 중 {}/{}", index + 1, total)
                }
                val fullTitle = fetchDetailTitle(item.externalId)
                item.externalId to fullTitle
            }.toMap()

            val enrichedResults = results.map { item ->
                val fullTitle = enrichedTitles[item.externalId]
                if (fullTitle != null) item.copy(title = fullTitle) else item
            }

            val enrichedCount = enrichedTitles.values.count { it != null }
            logger.info("Work24: 잘린 제목 보정 완료 (성공: {}건, 실패: {}건)", enrichedCount, truncated.size - enrichedCount)

            enrichedResults
        } catch (e: Exception) {
            logger.error("Work24: 제목 보정 중 에러 발생, 원본 {}건 반환", results.size, e)
            results
        }
    }

    private fun parseModifyDtm(smodifyDtm: String?): LocalDateTime? {
        if (smodifyDtm.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(smodifyDtm, modifyDtmFormatter)
        } catch (e: Exception) {
            logger.debug("Work24 smodifyDtm 파싱 실패: $smodifyDtm", e)
            null
        }
    }

    private fun Work24ApiResponse.WantedItem.toJobFetchResult(): JobFetchResult? {
        if (jobsCd !in ALLOWED_NURSE_JOB_CODES) return null

        val externalId = wantedAuthNo ?: return null
        val jobTitle = title ?: return null
        val companyName = company ?: return null
        val postingUrl = wantedInfoUrl ?: return null

        val closeDtValue = closeDt?.trim()
        val hasOnHireMarker = closeDtValue?.contains("채용시까지") == true
        val closeType = when {
            closeDtValue.isNullOrBlank() || closeDtValue == "채용시까지" -> CloseType.ON_HIRE
            hasOnHireMarker -> CloseType.ON_HIRE
            else -> CloseType.FIXED
        }

        val expiresAt = if (closeType == CloseType.FIXED) Work24CodeMapper.parseDate(closeDtValue) else null
        val isActive = closeType == CloseType.ON_HIRE || (expiresAt != null && expiresAt.isAfter(LocalDateTime.now()))

        return JobFetchResult(
            externalId = externalId,
            title = jobTitle,
            companyName = companyName,
            businessNumber = busino,
            jobCategory = jobsCd,
            location = region,
            zipCode = zipCd,
            roadNameAddress = strtnmCd,
            basicAddress = basicAddr,
            detailAddress = detailAddr,
            infoService = infoSvc,
            workRegion = Work24CodeMapper.mapWorkRegion(region),
            workDistrict = Work24CodeMapper.extractDistrict(region),
            employmentType = Work24CodeMapper.mapEmploymentType(empTpCd),
            careerMin = null,
            careerMax = null,
            educationLevel = Work24CodeMapper.mapEducationLevel(minEdubg),
            salaryMin = Work24CodeMapper.parseSalary(minSal),
            salaryMax = Work24CodeMapper.parseSalary(maxSal),
            salaryType = Work24CodeMapper.mapSalaryType(salTpNm),
            postingUrl = postingUrl,
            postedAt = Work24CodeMapper.parseDate(regDt),
            expiresAt = expiresAt,
            closeType = closeType,
            isActive = isActive,
            workHoursPerWeek = null
        )
    }
}
