package duit.server.infrastructure.external.job.work24

import duit.server.domain.job.entity.EducationLevel
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.SalaryType
import duit.server.domain.job.entity.WorkRegion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("Work24CodeMapper 단위 테스트")
class Work24CodeMapperTest {

    @Nested
    @DisplayName("mapEmploymentType")
    inner class MapEmploymentTypeTests {

        @Test
        fun `"10"은 FULL_TIME으로 매핑`() {
            val result = Work24CodeMapper.mapEmploymentType("10")
            assertEquals(EmploymentType.FULL_TIME, result)
        }

        @Test
        fun `"11"은 FULL_TIME으로 매핑`() {
            val result = Work24CodeMapper.mapEmploymentType("11")
            assertEquals(EmploymentType.FULL_TIME, result)
        }

        @Test
        fun `"20"은 CONTRACT로 매핑`() {
            val result = Work24CodeMapper.mapEmploymentType("20")
            assertEquals(EmploymentType.CONTRACT, result)
        }

        @Test
        fun `"21"은 CONTRACT로 매핑`() {
            val result = Work24CodeMapper.mapEmploymentType("21")
            assertEquals(EmploymentType.CONTRACT, result)
        }

        @Test
        fun `"4"는 DISPATCH로 매핑`() {
            val result = Work24CodeMapper.mapEmploymentType("4")
            assertEquals(EmploymentType.DISPATCH, result)
        }

        @Test
        fun `null은 null로 반환`() {
            val result = Work24CodeMapper.mapEmploymentType(null)
            assertNull(result)
        }

        @Test
        fun `"999"는 ETC로 매핑`() {
            val result = Work24CodeMapper.mapEmploymentType("999")
            assertEquals(EmploymentType.ETC, result)
        }

        @Test
        fun `"99"는 ETC로 매핑`() {
            val result = Work24CodeMapper.mapEmploymentType("99")
            assertEquals(EmploymentType.ETC, result)
        }
    }

    @Nested
    @DisplayName("mapEducationLevel")
    inner class MapEducationLevelTests {

        @Test
        fun `"00"은 NONE으로 매핑`() {
            val result = Work24CodeMapper.mapEducationLevel("00")
            assertEquals(EducationLevel.NONE, result)
        }

        @Test
        fun `null은 NONE으로 매핑`() {
            val result = Work24CodeMapper.mapEducationLevel(null)
            assertEquals(EducationLevel.NONE, result)
        }

        @Test
        fun `"01"은 null로 반환`() {
            val result = Work24CodeMapper.mapEducationLevel("01")
            assertNull(result)
        }

        @Test
        fun `"02"는 null로 반환`() {
            val result = Work24CodeMapper.mapEducationLevel("02")
            assertNull(result)
        }

        @Test
        fun `"03"은 HIGH_SCHOOL로 매핑`() {
            val result = Work24CodeMapper.mapEducationLevel("03")
            assertEquals(EducationLevel.HIGH_SCHOOL, result)
        }

        @Test
        fun `"04"는 ASSOCIATE로 매핑`() {
            val result = Work24CodeMapper.mapEducationLevel("04")
            assertEquals(EducationLevel.ASSOCIATE, result)
        }

        @Test
        fun `"05"는 BACHELOR로 매핑`() {
            val result = Work24CodeMapper.mapEducationLevel("05")
            assertEquals(EducationLevel.BACHELOR, result)
        }

        @Test
        fun `"06"은 MASTER로 매핑`() {
            val result = Work24CodeMapper.mapEducationLevel("06")
            assertEquals(EducationLevel.MASTER, result)
        }

        @Test
        fun `"07"은 DOCTOR로 매핑`() {
            val result = Work24CodeMapper.mapEducationLevel("07")
            assertEquals(EducationLevel.DOCTOR, result)
        }

        @Test
        fun `"99"는 null로 반환`() {
            val result = Work24CodeMapper.mapEducationLevel("99")
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("mapSalaryType")
    inner class MapSalaryTypeTests {

        @Test
        fun `"연봉"은 ANNUAL로 매핑`() {
            val result = Work24CodeMapper.mapSalaryType("연봉")
            assertEquals(SalaryType.ANNUAL, result)
        }

        @Test
        fun `"월급"은 MONTHLY로 매핑`() {
            val result = Work24CodeMapper.mapSalaryType("월급")
            assertEquals(SalaryType.MONTHLY, result)
        }

        @Test
        fun `"시급"은 HOURLY로 매핑`() {
            val result = Work24CodeMapper.mapSalaryType("시급")
            assertEquals(SalaryType.HOURLY, result)
        }

        @Test
        fun `"일급"은 null로 반환`() {
            val result = Work24CodeMapper.mapSalaryType("일급")
            assertNull(result)
        }

        @Test
        fun `null은 null로 반환`() {
            val result = Work24CodeMapper.mapSalaryType(null)
            assertNull(result)
        }

        @Test
        fun `빈 문자열은 null로 반환`() {
            val result = Work24CodeMapper.mapSalaryType("")
            assertNull(result)
        }

        @Test
        fun `공백만 있는 문자열은 null로 반환`() {
            val result = Work24CodeMapper.mapSalaryType("   ")
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("mapWorkRegion")
    inner class MapWorkRegionTests {

        @Test
        fun `"서울특별시 강남구"는 SEOUL로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("서울특별시 강남구")
            assertEquals(WorkRegion.SEOUL, result)
        }

        @Test
        fun `"부산광역시"는 BUSAN으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("부산광역시")
            assertEquals(WorkRegion.BUSAN, result)
        }

        @Test
        fun `"경기도 성남시"는 GYEONGGI로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("경기도 성남시")
            assertEquals(WorkRegion.GYEONGGI, result)
        }

        @Test
        fun `"전북특별자치도"는 JEONBUK으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("전북특별자치도")
            assertEquals(WorkRegion.JEONBUK, result)
        }

        @Test
        fun `"전라북도 전주시"는 JEONBUK으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("전라북도 전주시")
            assertEquals(WorkRegion.JEONBUK, result)
        }

        @Test
        fun `null은 null로 반환`() {
            val result = Work24CodeMapper.mapWorkRegion(null)
            assertNull(result)
        }

        @Test
        fun `빈 문자열은 null로 반환`() {
            val result = Work24CodeMapper.mapWorkRegion("")
            assertNull(result)
        }

        @Test
        fun `공백만 있는 문자열은 null로 반환`() {
            val result = Work24CodeMapper.mapWorkRegion("   ")
            assertNull(result)
        }

        @Test
        fun `"대구광역시"는 DAEGU로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("대구광역시")
            assertEquals(WorkRegion.DAEGU, result)
        }

        @Test
        fun `"인천광역시"는 INCHEON으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("인천광역시")
            assertEquals(WorkRegion.INCHEON, result)
        }

        @Test
        fun `"광주광역시"는 GWANGJU로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("광주광역시")
            assertEquals(WorkRegion.GWANGJU, result)
        }

        @Test
        fun `"대전광역시"는 DAEJEON으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("대전광역시")
            assertEquals(WorkRegion.DAEJEON, result)
        }

        @Test
        fun `"울산광역시"는 ULSAN으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("울산광역시")
            assertEquals(WorkRegion.ULSAN, result)
        }

        @Test
        fun `"세종특별자치시"는 SEJONG으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("세종특별자치시")
            assertEquals(WorkRegion.SEJONG, result)
        }

        @Test
        fun `"강원도"는 GANGWON으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("강원도")
            assertEquals(WorkRegion.GANGWON, result)
        }

        @Test
        fun `"충북도"는 CHUNGBUK으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("충북도")
            assertEquals(WorkRegion.CHUNGBUK, result)
        }

        @Test
        fun `"충남도"는 CHUNGNAM으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("충남도")
            assertEquals(WorkRegion.CHUNGNAM, result)
        }

        @Test
        fun `"전라남도"는 JEONNAM으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("전라남도")
            assertEquals(WorkRegion.JEONNAM, result)
        }

        @Test
        fun `"경상북도"는 GYEONGBUK으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("경상북도")
            assertEquals(WorkRegion.GYEONGBUK, result)
        }

        @Test
        fun `"경상남도"는 GYEONGNAM으로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("경상남도")
            assertEquals(WorkRegion.GYEONGNAM, result)
        }

        @Test
        fun `"제주도"는 JEJU로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("제주도")
            assertEquals(WorkRegion.JEJU, result)
        }

        @Test
        fun `알 수 없는 지역은 ETC로 매핑`() {
            val result = Work24CodeMapper.mapWorkRegion("화성시")
            assertEquals(WorkRegion.ETC, result)
        }
    }

    @Nested
    @DisplayName("extractDistrict")
    inner class ExtractDistrictTests {

        @Test
        fun `"서울특별시 강남구 역삼동"에서 "강남구 역삼동"을 추출`() {
            val result = Work24CodeMapper.extractDistrict("서울특별시 강남구 역삼동")
            assertEquals("강남구 역삼동", result)
        }

        @Test
        fun `"경기도 성남시 분당구 정자동"에서 "성남시 분당구 정자동"을 추출`() {
            val result = Work24CodeMapper.extractDistrict("경기도 성남시 분당구 정자동")
            assertEquals("성남시 분당구 정자동", result)
        }

        @Test
        fun `"서울특별시"는 null로 반환`() {
            val result = Work24CodeMapper.extractDistrict("서울특별시")
            assertNull(result)
        }

        @Test
        fun `null은 null로 반환`() {
            val result = Work24CodeMapper.extractDistrict(null)
            assertNull(result)
        }

        @Test
        fun `빈 문자열은 null로 반환`() {
            val result = Work24CodeMapper.extractDistrict("")
            assertNull(result)
        }

        @Test
        fun `공백만 있는 문자열은 null로 반환`() {
            val result = Work24CodeMapper.extractDistrict("   ")
            assertNull(result)
        }

        @Test
        fun `"부산광역시 해운대구 중동"에서 "해운대구 중동"을 추출`() {
            val result = Work24CodeMapper.extractDistrict("부산광역시 해운대구 중동")
            assertEquals("해운대구 중동", result)
        }

        @Test
        fun `"인천 남동구"에서 "남동구"를 추출`() {
            val result = Work24CodeMapper.extractDistrict("인천 남동구")
            assertEquals("남동구", result)
        }

        @Test
        fun `여러 공백이 있는 경우 정상 처리`() {
            val result = Work24CodeMapper.extractDistrict("서울특별시   강남구   역삼동")
            assertEquals("강남구 역삼동", result)
        }
    }

    @Nested
    @DisplayName("parseDate")
    inner class ParseDateTests {

        @Test
        fun `"20250424"는 yyyyMMdd 형식으로 파싱`() {
            val result = Work24CodeMapper.parseDate("20250424")
            assertEquals(LocalDateTime.of(2025, 4, 24, 0, 0), result)
        }

        @Test
        fun `"26-03-24"는 yy-MM-dd 형식으로 파싱`() {
            val result = Work24CodeMapper.parseDate("26-03-24")
            assertEquals(LocalDateTime.of(2026, 3, 24, 0, 0), result)
        }

        @Test
        fun `"2025-04-24"는 yyyy-MM-dd 형식으로 파싱`() {
            val result = Work24CodeMapper.parseDate("2025-04-24")
            assertEquals(LocalDateTime.of(2025, 4, 24, 0, 0), result)
        }

        @Test
        fun `null은 null로 반환`() {
            val result = Work24CodeMapper.parseDate(null)
            assertNull(result)
        }

        @Test
        fun `빈 문자열은 null로 반환`() {
            val result = Work24CodeMapper.parseDate("")
            assertNull(result)
        }

        @Test
        fun `"invalid"는 null로 반환`() {
            val result = Work24CodeMapper.parseDate("invalid")
            assertNull(result)
        }

        @Test
        fun `공백만 있는 문자열은 null로 반환`() {
            val result = Work24CodeMapper.parseDate("   ")
            assertNull(result)
        }

        @Test
        fun `"20240229"는 윤년으로 정상 파싱`() {
            val result = Work24CodeMapper.parseDate("20240229")
            assertEquals(LocalDateTime.of(2024, 2, 29, 0, 0), result)
        }
    }

    @Nested
    @DisplayName("parseSalary")
    inner class ParseSalaryTests {

        @Test
        fun `"3000"은 3000L로 파싱`() {
            val result = Work24CodeMapper.parseSalary("3000")
            assertEquals(3000L, result)
        }

        @Test
        fun `"0"은 0L로 파싱`() {
            val result = Work24CodeMapper.parseSalary("0")
            assertEquals(0L, result)
        }

        @Test
        fun `null은 null로 반환`() {
            val result = Work24CodeMapper.parseSalary(null)
            assertNull(result)
        }

        @Test
        fun `빈 문자열은 null로 반환`() {
            val result = Work24CodeMapper.parseSalary("")
            assertNull(result)
        }

        @Test
        fun `"abc"는 null로 반환`() {
            val result = Work24CodeMapper.parseSalary("abc")
            assertNull(result)
        }

        @Test
        fun `공백만 있는 문자열은 null로 반환`() {
            val result = Work24CodeMapper.parseSalary("   ")
            assertNull(result)
        }

        @Test
        fun `"50000000"은 50000000L로 파싱`() {
            val result = Work24CodeMapper.parseSalary("50000000")
            assertEquals(50000000L, result)
        }

        @Test
        fun `앞뒤 공백이 있는 경우 정상 파싱`() {
            val result = Work24CodeMapper.parseSalary("  5000  ")
            assertEquals(5000L, result)
        }

        @Test
        fun `음수는 null로 반환`() {
            val result = Work24CodeMapper.parseSalary("-1000")
            assertEquals(-1000L, result)
        }

        @Test
        fun `소수점이 있는 경우 null로 반환`() {
            val result = Work24CodeMapper.parseSalary("3000.5")
            assertNull(result)
        }
    }
}
