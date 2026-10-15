package duit.server.domain.job.entity

import java.time.LocalDate

/**
 * 최저임금 미달 구인정보 판정 (직업안정법 제25조 제2호, 시행령 제28조 제6호).
 *
 * 임금을 시간당 금액과 비교한다. 오탐으로 정상 공고를 숨기지 않도록 근거가 부족하면 미달로 보지 않는다.
 * - 시급은 그대로, 일급은 1일 8시간으로 나눈다. 단, 시간제 일급은 근로시간을 알 수 없어 판정하지 않는다.
 * - 월급·연봉은 주 소정근로시간이 있으면 (주 소정근로시간 + 주휴시간) × 4.345주를 월 유급시간으로 쓰고,
 *   없으면 명백한 전일제만 월 209시간을 적용한다. 시간제·고용형태 불명은 판정하지 않는다.
 */
object MinimumWage {

    // 고용노동부 고시 시간급 최저임금(원). 표 이후 연도는 마지막 값을 적용한다.
    private val HOURLY_BY_YEAR = sortedMapOf(
        2025 to 10_030L,
        2026 to 10_320L,
        2027 to 10_700L,
    )

    private const val DAILY_HOURS = 8.0
    private const val FULL_TIME_MONTHLY_HOURS = 209.0
    private const val STATUTORY_WEEKLY_HOURS = 40.0
    private const val WEEKS_PER_MONTH = 4.345

    private val PART_TIME_EMP_CODES = setOf("11", "21") // 고용24 고용형태: 시간(선택)제
    private val FULL_TIME_EMP_CODES = setOf("10", "20") // 고용24 고용형태: 기간의 정함이 없는/있는 근로계약
    private val PART_TIME_KEYWORDS = listOf("시간(선택)제", "시간제", "파트타임", "아르바이트")
    private val FULL_TIME_KEYWORDS = listOf("정규직", "기간의 정함이 없는 근로계약", "기간의 정함이 있는 근로계약")

    private val weeklyHoursPattern =
        Regex("주\\s*(?:소정\\s*)?(?:근로\\s*)?(?:시간\\s*)?[:：]?\\s*(\\d{1,2}(?:\\.\\d+)?)\\s*시간")

    enum class WorkPattern { FULL_TIME, PART_TIME, UNKNOWN }

    fun hourlyMinimumWage(year: Int): Long =
        HOURLY_BY_YEAR.headMap(year + 1).values.lastOrNull() ?: HOURLY_BY_YEAR.getValue(HOURLY_BY_YEAR.firstKey())

    /** 평가일 기준 최저임금. 접수 마감일이 더 높은 최저임금이 고시된 해에 걸치면 그 금액을 적용한다 */
    fun applicableHourlyMinimumWage(evaluationDate: LocalDate, closeDate: LocalDate?): Long =
        maxOf(hourlyMinimumWage(evaluationDate.year), closeDate?.let { hourlyMinimumWage(it.year) } ?: 0L)

    /**
     * @param amount 임금형태 기준 금액(원). 시급이면 시간당, 월급이면 월 금액
     */
    fun isBelow(
        salaryType: SalaryType?,
        amount: Long?,
        weeklyHours: Double?,
        workPattern: WorkPattern,
        evaluationDate: LocalDate,
        closeDate: LocalDate?,
    ): Boolean {
        if (amount == null || amount <= 0) return false
        val paidHours = paidHoursPerPayUnit(salaryType, weeklyHours, workPattern) ?: return false
        return amount < applicableHourlyMinimumWage(evaluationDate, closeDate) * paidHours
    }

    fun parseWeeklyHours(workingHours: String?): Double? =
        workingHours?.let { weeklyHoursPattern.find(it)?.groupValues?.get(1)?.toDoubleOrNull() }
            ?.takeIf { it > 0 }

    /** 고용형태 코드·명칭과 제목 등 보조 문구로 전일제/시간제를 가린다. 시간제 단서가 하나라도 있으면 시간제 */
    fun workPatternOf(empTpCd: String?, empTpNm: String?, vararg otherTexts: String?): WorkPattern = when {
        empTpCd in PART_TIME_EMP_CODES ||
            (listOf(empTpNm) + otherTexts).any { it.containsAny(PART_TIME_KEYWORDS) } -> WorkPattern.PART_TIME
        empTpCd in FULL_TIME_EMP_CODES || empTpNm.containsAny(FULL_TIME_KEYWORDS) -> WorkPattern.FULL_TIME
        else -> WorkPattern.UNKNOWN
    }

    private fun paidHoursPerPayUnit(salaryType: SalaryType?, weeklyHours: Double?, workPattern: WorkPattern): Double? =
        when (salaryType) {
            SalaryType.HOURLY -> 1.0
            SalaryType.DAILY -> DAILY_HOURS.takeIf { workPattern != WorkPattern.PART_TIME }
            SalaryType.MONTHLY -> monthlyPaidHours(weeklyHours, workPattern)
            SalaryType.ANNUAL -> monthlyPaidHours(weeklyHours, workPattern)?.times(12)
            null -> null
        }

    // 소정근로시간은 법정 주 40시간을 넘을 수 없어 40시간에서 자른다 (초과분은 가산임금이 붙는 연장근로)
    private fun monthlyPaidHours(weeklyHours: Double?, workPattern: WorkPattern): Double? = when {
        weeklyHours != null -> weeklyHours.coerceAtMost(STATUTORY_WEEKLY_HOURS)
            .let { (it + weeklyHolidayHours(it)) * WEEKS_PER_MONTH }
        workPattern == WorkPattern.FULL_TIME -> FULL_TIME_MONTHLY_HOURS
        else -> null
    }

    // 주 15시간 이상이면 주휴시간 = 주 소정근로시간 / 40 × 8
    private fun weeklyHolidayHours(weeklyHours: Double): Double =
        if (weeklyHours >= 15) weeklyHours / STATUTORY_WEEKLY_HOURS * 8 else 0.0

    private fun String?.containsAny(keywords: List<String>): Boolean =
        this != null && keywords.any { contains(it) }
}
