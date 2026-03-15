package duit.server.infrastructure.external.job.saramin

import duit.server.domain.job.entity.SourceType
import duit.server.infrastructure.external.job.JobFetcher
import duit.server.infrastructure.external.job.dto.IncrementalFetchResult
import duit.server.infrastructure.external.job.dto.JobFetchResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class SaraminJobFetcher(
    @Value("\${api.saramin.access-key:}") private val accessKey: String
) : JobFetcher {

    override val sourceType = SourceType.SARAMIN

    private val logger = LoggerFactory.getLogger(SaraminJobFetcher::class.java)

    private val restClient = RestClient.builder()
        .baseUrl("https://oapi.saramin.co.kr")
        .build()

    private val jobCodes = "462,465,469,470,471,475,489,496,498,500"
    private val pageSize = 110
    private val maxPages = 50
    private val seoulZone = ZoneId.of("Asia/Seoul")

    override fun fetchAll(): List<JobFetchResult> {
        if (accessKey.isBlank()) {
            logger.warn("Saramin access key is not configured. Skipping fetch.")
            return emptyList()
        }

        val results = mutableListOf<JobFetchResult>()
        var start = 0
        var total = Int.MAX_VALUE
        var pageCount = 0

        try {
            while (start < total && pageCount < maxPages) {
                val response = fetchPageWithRetry(start) ?: break
                val jobs = response.jobs

                if (pageCount == 0) {
                    total = jobs.total.toIntOrNull() ?: break
                }

                if (jobs.job.isEmpty()) break

                jobs.job.forEach { job ->
                    results.add(toJobFetchResult(job))
                }

                start += pageSize
                pageCount++
            }
        } catch (e: Exception) {
            logger.error("Saramin fetchAll: start=${start}에서 에러 발생, ${results.size}건 부분 반환", e)
        }

        return results
    }

    override fun fetchIncremental(since: LocalDateTime): IncrementalFetchResult {
        if (accessKey.isBlank()) {
            logger.warn("Saramin access key is not configured. Skipping incremental fetch.")
            return IncrementalFetchResult(emptyList(), null)
        }

        val results = mutableListOf<JobFetchResult>()
        var latestTimestamp: LocalDateTime? = null
        var start = 0
        var total = Int.MAX_VALUE
        var pageCount = 0
        var earlyTerminated = false

        try {
            while (start < total && pageCount < maxPages && !earlyTerminated) {
                val response = fetchPageWithRetry(start) ?: break
                val jobs = response.jobs

                if (pageCount == 0) {
                    total = jobs.total.toIntOrNull() ?: break
                }

                if (jobs.job.isEmpty()) break

                for (job in jobs.job) {
                    val postingDtm = toLocalDateTime(job.postingTimestamp)

                    if (!postingDtm.isAfter(since)) {
                        earlyTerminated = true
                        break
                    }

                    val fetchResult = toJobFetchResult(job)
                    results.add(fetchResult)

                    if (latestTimestamp == null || postingDtm.isAfter(latestTimestamp)) {
                        latestTimestamp = postingDtm
                    }
                }

                start += pageSize
                pageCount++
            }
        } catch (e: Exception) {
            logger.error("Saramin fetchIncremental: start=${start}에서 에러 발생, ${results.size}건 부분 반환", e)
        }

        logger.info("Saramin incremental: ${results.size}건 수집, ${pageCount}페이지 스캔, earlyTerminated=$earlyTerminated")
        return IncrementalFetchResult(results, latestTimestamp)
    }

    private fun fetchPage(start: Int): SaraminApiResponse? {
        return restClient.get()
            .uri { builder ->
                builder.path("/job-search")
                    .queryParam("access-key", accessKey)
                    .queryParam("job_cd", jobCodes)
                    .queryParam("fields", "posting-date,expiration-date,count")
                    .queryParam("count", pageSize)
                    .queryParam("sort", "pd")
                    .queryParam("start", start)
                    .build()
            }
            .retrieve()
            .body(SaraminApiResponse::class.java)
    }

    private fun fetchPageWithRetry(start: Int, maxRetries: Int = 2): SaraminApiResponse? {
        repeat(maxRetries + 1) { attempt ->
            try {
                return fetchPage(start)
            } catch (e: Exception) {
                if (attempt == maxRetries) throw e
                logger.warn("Saramin start=$start fetch 실패 (attempt ${attempt + 1}/$maxRetries), 재시도...", e)
                Thread.sleep(1000L * (attempt + 1))
            }
        }
        return null
    }

    private fun toJobFetchResult(job: SaraminApiResponse.Job): JobFetchResult {
        val (salaryMin, salaryMax) = SaraminCodeMapper.mapSalaryRange(job.salary.code)
        val workRegion = SaraminCodeMapper.mapWorkRegion(job.position.location.code)
        val workDistrict = SaraminCodeMapper.extractDistrict(job.position.location.name)
        val closeType = SaraminCodeMapper.mapCloseType(job.closeType.code)
        val employmentType = SaraminCodeMapper.mapEmploymentType(job.position.jobType.code)
        val educationLevel = SaraminCodeMapper.mapEducationLevel(job.position.requiredEducationLevel.code)

        val postedAt = toLocalDateTime(job.postingTimestamp)
        val expiresAt = if (closeType == duit.server.domain.job.entity.CloseType.FIXED) {
            toLocalDateTime(job.expirationTimestamp)
        } else {
            null
        }

        return JobFetchResult(
            externalId = job.id,
            title = job.position.title,
            companyName = job.company.detail.name,
            jobCategory = job.position.jobCode.name.takeIf { it.isNotBlank() },
            location = job.position.location.name.takeIf { it.isNotBlank() },
            workRegion = workRegion,
            workDistrict = workDistrict,
            employmentType = employmentType,
            careerMin = job.position.experienceLevel.min.takeIf { it > 0 },
            careerMax = job.position.experienceLevel.max.takeIf { it > 0 },
            educationLevel = educationLevel,
            salaryMin = salaryMin,
            salaryMax = salaryMax,
            salaryType = null,
            postingUrl = job.url,
            postedAt = postedAt,
            expiresAt = expiresAt,
            closeType = closeType,
            isActive = job.active == 1,
            workHoursPerWeek = null
        )
    }

    private fun toLocalDateTime(unixTimestamp: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimestamp), seoulZone)
}
