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
                if (fetched >= total) break

                startPage++
            }
        } catch (e: Exception) {
            logger.error("Work24 fetchAll: page ${startPage}에서 에러 발생, ${results.size}건 부분 반환", e)
        }

        return results
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
        return IncrementalFetchResult(results, latestTimestamp)
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

        return xmlMapper.readValue(responseBody, Work24ApiResponse::class.java)
    }

    private fun fetchPageWithRetry(startPage: Int, display: Int, maxRetries: Int = 2): Work24ApiResponse? {
        repeat(maxRetries + 1) { attempt ->
            try {
                return fetchPage(startPage, display)
            } catch (e: Exception) {
                if (attempt == maxRetries) throw e
                logger.warn("Work24 page $startPage fetch 실패 (attempt ${attempt + 1}/$maxRetries), 재시도...", e)
                Thread.sleep(1000L * (attempt + 1))
            }
        }
        return null
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
            jobCategory = jobsCd,
            location = region,
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
