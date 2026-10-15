package duit.server.domain.job.service

import duit.server.domain.job.repository.JobCompanyRepository
import duit.server.infrastructure.external.job.work24.WageDefaulterCheckResult
import duit.server.infrastructure.external.job.work24.Work24WageDefaulterClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 임금체불 명단공개 사업주 여부 갱신 (직업안정법 제25조 제1호 표시 의무).
 *
 * 활성 공고가 있는 회사 중 미확인이거나 24시간이 지난 회사를 순차 조회한다.
 * 고용24 API 에 부담을 주지 않도록 요청 사이에 간격을 두고 1회 실행당 조회 수를 제한하며,
 * 판정할 수 없는 응답은 기존 값을 유지한다. 트랜잭션은 회사별 갱신 쿼리 단위로만 잡는다.
 * API 키 오류나 연속된 API 장애(시간 초과·오류 코드 등)가 나면 남은 회사는 다음 실행으로 미룬다.
 */
@Service
class WageDefaulterService(
    private val jobCompanyRepository: JobCompanyRepository,
    private val wageDefaulterClient: Work24WageDefaulterClient,
    @Value("\${api.work24.wage-defaulter.request-interval-ms:200}")
    private val requestIntervalMs: Long,
) {
    companion object {
        const val MAX_CHECKS_PER_RUN = 300
        const val MAX_CONSECUTIVE_API_FAILURES = 3
        private const val RECHECK_HOURS = 24L
        private const val API_KEY_ERROR_CODE = "ERR0001"
    }

    private val log = LoggerFactory.getLogger(javaClass)

    fun refreshStaleCompanies(now: LocalDateTime = LocalDateTime.now()) {
        if (!wageDefaulterClient.isEnabled) {
            log.info("임금체불 명단 조회 API 키가 없어 건너뜀")
            return
        }

        val targets = jobCompanyRepository.findWageDefaulterCheckTargets(now.minusHours(RECHECK_HOURS))
            .mapNotNull { company -> company.normalizedBusinessNumber()?.let { company.id!! to it } }
            .take(MAX_CHECKS_PER_RUN)

        var updated = 0
        var unknown = 0
        var consecutiveApiFailures = 0
        for ((index, target) in targets.withIndex()) {
            if (index > 0 && requestIntervalMs > 0) Thread.sleep(requestIntervalMs)

            val (companyId, businessNumber) = target
            when (val result = wageDefaulterClient.check(businessNumber)) {
                is WageDefaulterCheckResult.Checked -> {
                    jobCompanyRepository.updateWageDefaulter(companyId, result.listed, LocalDateTime.now())
                    updated++
                    consecutiveApiFailures = 0
                }
                is WageDefaulterCheckResult.Unknown -> {
                    unknown++
                    log.warn("임금체불 명단 확인 불가 (기존 값 유지): companyId={}, reason={}", companyId, result.reason)
                    consecutiveApiFailures = if (result.apiFailure) consecutiveApiFailures + 1 else 0
                    if (result.errorCode == API_KEY_ERROR_CODE) {
                        log.error("임금체불 명단 조회 API 키 오류로 이번 실행을 중단합니다")
                        break
                    }
                    if (consecutiveApiFailures >= MAX_CONSECUTIVE_API_FAILURES) {
                        log.error("임금체불 명단 조회 API 장애가 {}회 연속 발생해 이번 실행을 중단합니다", consecutiveApiFailures)
                        break
                    }
                }
            }
        }

        log.info("임금체불 명단 조회 완료: 대상={}, 갱신={}, 확인불가={}", targets.size, updated, unknown)
    }
}
