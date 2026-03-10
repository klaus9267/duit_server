package duit.server.infrastructure.external.job.work24

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.SourceType
import duit.server.infrastructure.external.job.JobFetcher
import duit.server.infrastructure.external.job.dto.JobFetchResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDateTime

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
                    .body(String::class.java) ?: break

                val apiResponse = xmlMapper.readValue(responseBody, Work24ApiResponse::class.java)
                val total = apiResponse.total?.toLongOrNull() ?: 0L
                val items = apiResponse.wanted ?: emptyList()

                if (items.isEmpty()) break

                results.addAll(items.mapNotNull { it.toJobFetchResult() })

                val fetched = (startPage - 1) * display + items.size
                if (fetched >= total) break

                startPage++
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch jobs from Work24", e)
            return emptyList()
        }

        return results
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
