package duit.server.infrastructure.external.job.work24

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import duit.server.domain.job.entity.JobPostingWork24Detail
import duit.server.domain.job.entity.SourceType
import duit.server.infrastructure.external.job.JobFetcher
import duit.server.infrastructure.external.job.dto.CompanyFetchResult
import duit.server.infrastructure.external.job.dto.JobFetchResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDateTime

@Component
class Work24JobFetcher(
    @Value("\${api.work24.auth-key:}") private val authKey: String,
    @Value("\${api.work24.list-page-limit:0}") private val listPageLimit: Int,
    @Value("\${api.work24.detail-limit:0}") private val detailLimit: Int,
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

    companion object {
        private const val OCCUPATION_FILTER = "304000|304001|304002|307500"
        private const val LIST_DISPLAY = 100
    }

    override fun fetchAll(): List<JobFetchResult> {
        if (authKey.isBlank()) {
            logger.warn("Work24 auth key is not configured. Skipping fetch.")
            return emptyList()
        }

        val listItems = fetchListAll()
        if (listItems.isEmpty()) return emptyList()

        val targets = if (detailLimit > 0) listItems.take(detailLimit) else listItems
        logger.info("Work24 detail 조회 시작: {}/{}건", targets.size, listItems.size)

        val results = mutableListOf<JobFetchResult>()
        var success = 0
        var skipped = 0

        targets.forEachIndexed { index, item ->
            val authNo = item.wantedAuthNo
            if (authNo.isNullOrBlank()) {
                skipped++
                return@forEachIndexed
            }

            val detail = runCatching { fetchDetail(authNo) }
                .onFailure { logger.warn("Work24 detail 에러 (건너뜀): wantedAuthNo={}", authNo, it) }
                .getOrNull()

            if (detail == null) {
                skipped++
            } else {
                results.add(mergeListAndDetail(item, detail))
                success++
            }

            val processed = index + 1
            if (processed % 50 == 0 || processed == targets.size) {
                logger.info("Work24 detail 진행 {}/{} (성공={}, 스킵={})", processed, targets.size, success, skipped)
            }
        }

        logger.info("Work24 수집 완료: list={}, 요청={}, 성공={}, 스킵={}", listItems.size, targets.size, success, skipped)
        return results
    }

    private fun fetchListAll(): List<Work24ApiResponse.WantedItem> {
        val results = mutableListOf<Work24ApiResponse.WantedItem>()
        val pageCap = if (listPageLimit > 0) listPageLimit else Int.MAX_VALUE
        var startPage = 1
        var total = 0L

        while (startPage <= pageCap) {
            val response = fetchPageWithRetry(startPage) ?: break
            val items = response.wanted ?: emptyList()
            if (items.isEmpty()) break

            results.addAll(items)
            total = response.total?.toLongOrNull() ?: total
            val fetched = results.size.toLong()
            logger.info("Work24 list page {}: 누적={}, total={}", startPage, fetched, total)

            if (total > 0 && fetched >= total) break
            startPage++
        }
        return results
    }

    private fun fetchPageWithRetry(startPage: Int): Work24ApiResponse? =
        fetchWithRetry("page $startPage fetch") { fetchListPage(startPage) }

    private fun fetchListPage(startPage: Int): Work24ApiResponse? {
        val body = restClient.get()
            .uri { builder ->
                builder.path("/cm/openApi/call/wk/callOpenApiSvcInfo210L01.do")
                    .queryParam("authKey", authKey)
                    .queryParam("callTp", "L")
                    .queryParam("returnType", "XML")
                    .queryParam("display", LIST_DISPLAY)
                    .queryParam("occupation", OCCUPATION_FILTER)
                    .queryParam("startPage", startPage)
                    .build()
            }
            .retrieve()
            .body(String::class.java) ?: return null

        return xmlMapper.readValue(stripNonXmlTags(body), Work24ApiResponse::class.java)
    }

    private fun fetchDetail(wantedAuthNo: String): Work24DetailResponse? =
        fetchWithRetry("상세 API (wantedAuthNo=$wantedAuthNo)") {
            val body = restClient.get()
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

            val response = xmlMapper.readValue(stripNonXmlTags(body), Work24DetailResponse::class.java)
            if (response.wantedInfo == null && response.corpInfo == null) {
                logger.warn("Work24 detail 정보 없음: wantedAuthNo={}, message={}", wantedAuthNo, response.message)
                null
            } else {
                response
            }
        }

    private val xmlElementWhitelist = setOf(
        // 목록 API (callTp=L)
        "wantedRoot", "total", "startPage", "display", "wanted",
        "wantedAuthNo", "company", "busino", "indTpNm", "title", "salTpNm", "sal", "minSal", "maxSal",
        "region", "holidayTpNm", "minEdubg", "maxEdubg", "career",
        "regDt", "closeDt", "infoSvc", "wantedInfoUrl", "wantedMobileInfoUrl",
        "zipCd", "strtnmCd", "basicAddr", "detailAddr",
        "empTpCd", "jobsCd", "smodifyDtm",
        // 상세 API (callTp=D) — 루트/섹션
        "wantedDtl", "corpInfo", "wantedInfo", "empchargeInfo", "message", "messageCd",
        // 상세 corpInfo
        "corpNm", "reperNm", "totPsncnt", "capitalAmt", "yrSalesAmt", "indTpCdNm",
        "busiCont", "corpAddr", "homePg", "busiSize",
        // 상세 wantedInfo
        "jobsNm", "wantedTitle", "relJobsNm", "jobCont", "receiptCloseDt", "empTpNm",
        "collectPsncnt", "enterTpNm", "eduNm", "forLang", "major", "certificate",
        "mltsvcExcHope", "compAbl", "pfCond", "etcPfCond", "selMthd", "rcptMthd",
        "submitDoc", "etcHopeCont", "workRegion", "nearLine", "workdayWorkhrCont",
        "fourIns", "retirepay", "etcWelfare", "disableCvntl", "dtlRecrContUrl",
        "minEdubgIcd", "maxEdubgIcd", "regionCd", "enterTpCd", "salTpCd",
        "staAreaRegionCd", "lineCd", "staNmCd", "exitNoCd", "walkDistCd",
        // 상세 empchargeInfo
        "empChargerDpt", "contactTelno", "chargerFaxNo",
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

    private fun mergeListAndDetail(
        listItem: Work24ApiResponse.WantedItem,
        detail: Work24DetailResponse,
    ): JobFetchResult {
        val info = detail.wantedInfo
        val corp = detail.corpInfo
        val charge = detail.empchargeInfo

        val receiptCloseDt = info?.receiptCloseDt ?: listItem.closeDt
        val isActive = determineIsActive(receiptCloseDt)

        val work24Detail = JobPostingWork24Detail(
            jobsNm = info?.jobsNm,
            wantedTitle = info?.wantedTitle ?: listItem.title,
            relJobsNm = info?.relJobsNm,
            jobCont = info?.jobCont,
            receiptCloseDt = receiptCloseDt,
            empTpNm = info?.empTpNm,
            collectPsncnt = info?.collectPsncnt,
            salTpNm = info?.salTpNm ?: listItem.salTpNm,
            enterTpNm = info?.enterTpNm,
            eduNm = info?.eduNm,
            forLang = info?.forLang,
            major = info?.major,
            certificate = info?.certificate,
            mltsvcExcHope = info?.mltsvcExcHope,
            compAbl = info?.compAbl,
            pfCond = info?.pfCond,
            etcPfCond = info?.etcPfCond,
            selMthd = info?.selMthd,
            rcptMthd = info?.rcptMthd,
            submitDoc = info?.submitDoc,
            etcHopeCont = info?.etcHopeCont,
            workRegion = info?.workRegion ?: listItem.region,
            nearLine = info?.nearLine,
            workdayWorkhrCont = info?.workdayWorkhrCont,
            fourIns = info?.fourIns,
            retirepay = info?.retirepay,
            etcWelfare = info?.etcWelfare,
            disableCvntl = info?.disableCvntl,
            dtlRecrContUrl = info?.dtlRecrContUrl ?: listItem.wantedInfoUrl,
            jobsCd = info?.jobsCd ?: listItem.jobsCd,
            minEdubgIcd = info?.minEdubgIcd ?: listItem.minEdubg,
            maxEdubgIcd = info?.maxEdubgIcd ?: listItem.maxEdubg,
            regionCd = info?.regionCd,
            empTpCd = info?.empTpCd ?: listItem.empTpCd,
            enterTpCd = info?.enterTpCd,
            salTpCd = info?.salTpCd,
            staAreaRegionCd = info?.staAreaRegionCd,
            lineCd = info?.lineCd,
            staNmCd = info?.staNmCd,
            exitNoCd = info?.exitNoCd,
            walkDistCd = info?.walkDistCd,
            empChargerDpt = charge?.empChargerDpt,
            contactTelno = charge?.contactTelno,
            chargerFaxNo = charge?.chargerFaxNo,
        )

        val company = CompanyFetchResult(
            businessNumber = listItem.busino?.takeIf { it.isNotBlank() },
            corpNm = (corp?.corpNm ?: listItem.company)?.takeIf { it.isNotBlank() },
            reperNm = corp?.reperNm?.takeIf { it.isNotBlank() },
            totPsncnt = Work24CodeMapper.parseLongPrefix(corp?.totPsncnt),
            capitalAmt = Work24CodeMapper.parseLongPrefix(corp?.capitalAmt),
            yrSalesAmt = Work24CodeMapper.parseLongPrefix(corp?.yrSalesAmt),
            indTpCdNm = corp?.indTpCdNm?.takeIf { it.isNotBlank() },
            busiCont = corp?.busiCont?.takeIf { it.isNotBlank() },
            corpAddr = corp?.corpAddr?.takeIf { it.isNotBlank() },
            homePg = corp?.homePg?.takeIf { it.isNotBlank() },
            busiSize = corp?.busiSize?.takeIf { it.isNotBlank() },
        )

        return JobFetchResult(
            externalId = listItem.wantedAuthNo!!,
            isActive = isActive,
            detail = work24Detail,
            company = company,
        )
    }

    private fun determineIsActive(receiptCloseDt: String?): Boolean {
        val cleaned = receiptCloseDt?.trim().orEmpty()
        if (cleaned.isBlank()) return true
        if (cleaned.contains("채용시까지") || cleaned.contains("상시")) return true
        val parsed = Work24CodeMapper.parseDate(cleaned) ?: return true
        return parsed.isAfter(LocalDateTime.now())
    }
}
