package duit.server.domain.job.entity

/** annualMultiplier: 정렬용 연간 급여(salaryMin)로 환산할 때 곱하는 값 */
enum class SalaryType(val displayName: String, val annualMultiplier: Long) {
    ANNUAL("연봉", 1),
    MONTHLY("월급", 12),
    HOURLY("시급", 2_508), // 월 209시간 × 12개월
    DAILY("일급", 261), // 연간 근무일수
    ;

    companion object {
        private val keywordPattern = Regex("연봉|월급|시급|일급|급여")

        /** 임금조건 설명(예: "월급 250만원 이상")에 처음 나오는 임금형태. '급여'처럼 형태를 알 수 없으면 null */
        fun fromDescription(description: String?): SalaryType? =
            description?.let { keywordPattern.find(it)?.value }
                ?.let { keyword -> entries.firstOrNull { it.displayName == keyword } }
    }
}
