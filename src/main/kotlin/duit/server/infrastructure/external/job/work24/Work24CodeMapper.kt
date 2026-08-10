package duit.server.infrastructure.external.job.work24

import duit.server.domain.job.entity.EducationLevel
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.SalaryType
import duit.server.domain.job.entity.WorkRegion
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object Work24CodeMapper {

    fun mapEmploymentType(empTpCd: String?): EmploymentType? = when (empTpCd) {
        "10", "11" -> EmploymentType.FULL_TIME
        "20", "21" -> EmploymentType.CONTRACT
        "4" -> EmploymentType.DISPATCH
        null -> null
        else -> EmploymentType.ETC
    }

    fun mapEducationLevel(minEdubg: String?): EducationLevel? = when (minEdubg) {
        "00", null -> EducationLevel.NONE
        "01", "02" -> null
        "03" -> EducationLevel.HIGH_SCHOOL
        "04" -> EducationLevel.ASSOCIATE
        "05" -> EducationLevel.BACHELOR
        "06" -> EducationLevel.MASTER
        "07" -> EducationLevel.DOCTOR
        else -> null
    }

    fun mapSalaryType(salTpNm: String?): SalaryType? {
        if (salTpNm.isNullOrBlank()) return null
        return when (salTpNm) {
            "연봉" -> SalaryType.ANNUAL
            "월급" -> SalaryType.MONTHLY
            "시급" -> SalaryType.HOURLY
            else -> null
        }
    }

    fun mapWorkRegion(region: String?): WorkRegion? {
        if (region.isNullOrBlank()) return null
        val normalized = normalizeRegion(region)
        return when {
            normalized.startsWith("서울") -> WorkRegion.SEOUL
            normalized.startsWith("부산") -> WorkRegion.BUSAN
            normalized.startsWith("대구") -> WorkRegion.DAEGU
            normalized.startsWith("인천") -> WorkRegion.INCHEON
            normalized.startsWith("광주") -> WorkRegion.GWANGJU
            normalized.startsWith("대전") -> WorkRegion.DAEJEON
            normalized.startsWith("울산") -> WorkRegion.ULSAN
            normalized.startsWith("세종") -> WorkRegion.SEJONG
            normalized.startsWith("경기") -> WorkRegion.GYEONGGI
            normalized.startsWith("강원") -> WorkRegion.GANGWON
            normalized.startsWith("충북") || normalized.startsWith("충청북도") -> WorkRegion.CHUNGBUK
            normalized.startsWith("충남") || normalized.startsWith("충청남도") -> WorkRegion.CHUNGNAM
            normalized.startsWith("전북") || normalized.startsWith("전라북도") -> WorkRegion.JEONBUK
            normalized.startsWith("전남") || normalized.startsWith("전라남도") -> WorkRegion.JEONNAM
            normalized.startsWith("경북") || normalized.startsWith("경상북도") -> WorkRegion.GYEONGBUK
            normalized.startsWith("경남") || normalized.startsWith("경상남도") -> WorkRegion.GYEONGNAM
            normalized.startsWith("제주") -> WorkRegion.JEJU
            else -> WorkRegion.ETC
        }
    }

    fun extractDistrict(region: String?): String? {
        if (region.isNullOrBlank()) return null
        val parts = normalizeRegion(region).split(" ").filter { it.isNotBlank() }
        if (parts.size <= 1) return null
        return parts.drop(1).joinToString(" ")
    }

    private val dateFormatters = listOf(
        DateTimeFormatter.ofPattern("yy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
    )

    fun parseDate(dateStr: String?): LocalDateTime? {
        if (dateStr.isNullOrBlank()) return null
        val cleaned = dateStr.trim()
        for (formatter in dateFormatters) {
            try {
                val date = java.time.LocalDate.parse(cleaned, formatter)
                return LocalDateTime.of(date, java.time.LocalTime.MIDNIGHT)
            } catch (_: DateTimeParseException) {
                continue
            }
        }
        return null
    }

    fun parseSalary(sal: String?): Long? {
        if (sal.isNullOrBlank()) return null
        val value = sal.trim().toLongOrNull() ?: return null
        return if (value <= 0) null else value
    }

    private val leadingNumberPattern = Regex("^-?\\d+")
    private val leadingPostalCodePattern = Regex("^\\s*(?:\\([^)]*\\)|\\d{5})\\s*")

    private fun normalizeRegion(region: String): String =
        region.replaceFirst(leadingPostalCodePattern, "").trim()

    /** "100 명", "50 백만원" 같은 선행 숫자 추출 */
    fun parseLongPrefix(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val matched = leadingNumberPattern.find(raw.trim())?.value ?: return null
        return matched.toLongOrNull()?.takeIf { it != 0L }
    }
}
