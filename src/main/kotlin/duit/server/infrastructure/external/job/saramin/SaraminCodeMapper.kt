package duit.server.infrastructure.external.job.saramin

import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.EducationLevel
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.WorkRegion

object SaraminCodeMapper {

    fun mapCloseType(code: String): CloseType = when (code) {
        "1" -> CloseType.FIXED
        "2" -> CloseType.ON_HIRE
        "3", "4" -> CloseType.ONGOING
        else -> CloseType.ONGOING
    }

    fun mapEmploymentType(code: String): EmploymentType = when (code) {
        "1" -> EmploymentType.FULL_TIME
        "2" -> EmploymentType.CONTRACT
        "4" -> EmploymentType.INTERN
        "5" -> EmploymentType.PART_TIME
        "6" -> EmploymentType.DISPATCH
        else -> EmploymentType.ETC
    }

    fun mapEducationLevel(code: String): EducationLevel? = when (code) {
        "0" -> EducationLevel.NONE
        "1" -> EducationLevel.HIGH_SCHOOL
        "2", "7" -> EducationLevel.ASSOCIATE
        "3", "8" -> EducationLevel.BACHELOR
        "4", "9" -> EducationLevel.MASTER
        "5" -> EducationLevel.DOCTOR
        else -> null
    }

    fun mapSalaryRange(code: String): Pair<Long?, Long?> = when (code) {
        "0" -> null to null
        "1" -> 800L to 1000L
        "2" -> 1000L to 1200L
        "3" -> 1200L to 1400L
        "4" -> 1400L to 1600L
        "5" -> 1600L to 1800L
        "6" -> 1800L to 2000L
        "7" -> 2000L to 2200L
        "8" -> 2200L to 2400L
        "9" -> 2400L to 2600L
        "10" -> 2600L to 2800L
        "11" -> 2800L to 3000L
        "12" -> 3000L to 3500L
        "13" -> 3500L to 4000L
        "14" -> 4000L to 4500L
        "15" -> 4500L to 5000L
        "16" -> 5000L to 5500L
        "17" -> 5500L to 6000L
        "18" -> 6000L to 7000L
        "19" -> 7000L to 8000L
        "20" -> 8000L to 9000L
        "21" -> 9000L to 10000L
        "22" -> 10000L to null
        "99" -> null to null
        else -> null to null
    }

    fun mapWorkRegion(locationCode: String): WorkRegion? {
        val prefix = locationCode.take(3)
        return when (prefix) {
            "101" -> WorkRegion.SEOUL
            "102" -> WorkRegion.GYEONGGI
            "103" -> WorkRegion.GWANGJU
            "104" -> WorkRegion.DAEGU
            "105" -> WorkRegion.DAEJEON
            "106" -> WorkRegion.BUSAN
            "107" -> WorkRegion.ULSAN
            "108" -> WorkRegion.INCHEON
            "109" -> WorkRegion.GANGWON
            "110" -> WorkRegion.GYEONGNAM
            "111" -> WorkRegion.GYEONGBUK
            "112" -> WorkRegion.JEONNAM
            "113" -> WorkRegion.JEONBUK
            "114" -> WorkRegion.CHUNGNAM
            "115" -> WorkRegion.CHUNGBUK
            "116" -> WorkRegion.JEJU
            "118" -> WorkRegion.SEJONG
            else -> null
        }
    }

    fun extractDistrict(locationName: String): String? {
        val parts = locationName.split(">")
        return if (parts.size >= 2) parts[1].trim().takeIf { it.isNotEmpty() } else null
    }
}
