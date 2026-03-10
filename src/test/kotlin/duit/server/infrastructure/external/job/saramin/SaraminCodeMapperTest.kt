package duit.server.infrastructure.external.job.saramin

import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.EducationLevel
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.WorkRegion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("SaraminCodeMapper 단위 테스트")
class SaraminCodeMapperTest {

    @Nested
    @DisplayName("mapCloseType")
    inner class MapCloseTypeTests {

        @Test
        fun `code 1은 FIXED로 매핑`() {
            val result = SaraminCodeMapper.mapCloseType("1")
            assertEquals(CloseType.FIXED, result)
        }

        @Test
        fun `code 2는 ON_HIRE로 매핑`() {
            val result = SaraminCodeMapper.mapCloseType("2")
            assertEquals(CloseType.ON_HIRE, result)
        }

        @Test
        fun `code 3은 ONGOING으로 매핑`() {
            val result = SaraminCodeMapper.mapCloseType("3")
            assertEquals(CloseType.ONGOING, result)
        }

        @Test
        fun `code 4는 ONGOING으로 매핑`() {
            val result = SaraminCodeMapper.mapCloseType("4")
            assertEquals(CloseType.ONGOING, result)
        }

        @Test
        fun `unknown code는 ONGOING으로 매핑`() {
            val result = SaraminCodeMapper.mapCloseType("unknown")
            assertEquals(CloseType.ONGOING, result)
        }

        @Test
        fun `empty string은 ONGOING으로 매핑`() {
            val result = SaraminCodeMapper.mapCloseType("")
            assertEquals(CloseType.ONGOING, result)
        }
    }

    @Nested
    @DisplayName("mapEmploymentType")
    inner class MapEmploymentTypeTests {

        @Test
        fun `code 1은 FULL_TIME으로 매핑`() {
            val result = SaraminCodeMapper.mapEmploymentType("1")
            assertEquals(EmploymentType.FULL_TIME, result)
        }

        @Test
        fun `code 2는 CONTRACT로 매핑`() {
            val result = SaraminCodeMapper.mapEmploymentType("2")
            assertEquals(EmploymentType.CONTRACT, result)
        }

        @Test
        fun `code 4는 INTERN으로 매핑`() {
            val result = SaraminCodeMapper.mapEmploymentType("4")
            assertEquals(EmploymentType.INTERN, result)
        }

        @Test
        fun `code 5는 PART_TIME으로 매핑`() {
            val result = SaraminCodeMapper.mapEmploymentType("5")
            assertEquals(EmploymentType.PART_TIME, result)
        }

        @Test
        fun `code 6은 DISPATCH로 매핑`() {
            val result = SaraminCodeMapper.mapEmploymentType("6")
            assertEquals(EmploymentType.DISPATCH, result)
        }

        @Test
        fun `unknown code는 ETC로 매핑`() {
            val result = SaraminCodeMapper.mapEmploymentType("999")
            assertEquals(EmploymentType.ETC, result)
        }

        @Test
        fun `empty string은 ETC로 매핑`() {
            val result = SaraminCodeMapper.mapEmploymentType("")
            assertEquals(EmploymentType.ETC, result)
        }
    }

    @Nested
    @DisplayName("mapEducationLevel")
    inner class MapEducationLevelTests {

        @Test
        fun `code 0은 NONE으로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("0")
            assertEquals(EducationLevel.NONE, result)
        }

        @Test
        fun `code 1은 HIGH_SCHOOL로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("1")
            assertEquals(EducationLevel.HIGH_SCHOOL, result)
        }

        @Test
        fun `code 2는 ASSOCIATE로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("2")
            assertEquals(EducationLevel.ASSOCIATE, result)
        }

        @Test
        fun `code 3은 BACHELOR로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("3")
            assertEquals(EducationLevel.BACHELOR, result)
        }

        @Test
        fun `code 4는 MASTER로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("4")
            assertEquals(EducationLevel.MASTER, result)
        }

        @Test
        fun `code 5는 DOCTOR로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("5")
            assertEquals(EducationLevel.DOCTOR, result)
        }

        @Test
        fun `code 7은 ASSOCIATE로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("7")
            assertEquals(EducationLevel.ASSOCIATE, result)
        }

        @Test
        fun `code 8은 BACHELOR로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("8")
            assertEquals(EducationLevel.BACHELOR, result)
        }

        @Test
        fun `code 9는 MASTER로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("9")
            assertEquals(EducationLevel.MASTER, result)
        }

        @Test
        fun `unknown code는 null로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("unknown")
            assertNull(result)
        }

        @Test
        fun `empty string은 null로 매핑`() {
            val result = SaraminCodeMapper.mapEducationLevel("")
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("mapSalaryRange")
    inner class MapSalaryRangeTests {

        @Test
        fun `code 0은 null null으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("0")
            assertEquals(null to null, result)
        }

        @Test
        fun `code 1은 800 1000으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("1")
            assertEquals(800L to 1000L, result)
        }

        @Test
        fun `code 2는 1000 1200으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("2")
            assertEquals(1000L to 1200L, result)
        }

        @Test
        fun `code 3은 1200 1400으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("3")
            assertEquals(1200L to 1400L, result)
        }

        @Test
        fun `code 4는 1400 1600으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("4")
            assertEquals(1400L to 1600L, result)
        }

        @Test
        fun `code 5는 1600 1800으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("5")
            assertEquals(1600L to 1800L, result)
        }

        @Test
        fun `code 6은 1800 2000으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("6")
            assertEquals(1800L to 2000L, result)
        }

        @Test
        fun `code 7은 2000 2200으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("7")
            assertEquals(2000L to 2200L, result)
        }

        @Test
        fun `code 8은 2200 2400으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("8")
            assertEquals(2200L to 2400L, result)
        }

        @Test
        fun `code 9는 2400 2600으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("9")
            assertEquals(2400L to 2600L, result)
        }

        @Test
        fun `code 10은 2600 2800으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("10")
            assertEquals(2600L to 2800L, result)
        }

        @Test
        fun `code 11은 2800 3000으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("11")
            assertEquals(2800L to 3000L, result)
        }

        @Test
        fun `code 12는 3000 3500으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("12")
            assertEquals(3000L to 3500L, result)
        }

        @Test
        fun `code 13은 3500 4000으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("13")
            assertEquals(3500L to 4000L, result)
        }

        @Test
        fun `code 14는 4000 4500으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("14")
            assertEquals(4000L to 4500L, result)
        }

        @Test
        fun `code 15는 4500 5000으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("15")
            assertEquals(4500L to 5000L, result)
        }

        @Test
        fun `code 16은 5000 5500으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("16")
            assertEquals(5000L to 5500L, result)
        }

        @Test
        fun `code 17은 5500 6000으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("17")
            assertEquals(5500L to 6000L, result)
        }

        @Test
        fun `code 18은 6000 7000으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("18")
            assertEquals(6000L to 7000L, result)
        }

        @Test
        fun `code 19는 7000 8000으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("19")
            assertEquals(7000L to 8000L, result)
        }

        @Test
        fun `code 20은 8000 9000으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("20")
            assertEquals(8000L to 9000L, result)
        }

        @Test
        fun `code 21은 9000 10000으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("21")
            assertEquals(9000L to 10000L, result)
        }

        @Test
        fun `code 22는 10000 null로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("22")
            assertEquals(10000L to null, result)
        }

        @Test
        fun `code 99는 null null으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("99")
            assertEquals(null to null, result)
        }

        @Test
        fun `unknown code는 null null으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("unknown")
            assertEquals(null to null, result)
        }

        @Test
        fun `empty string은 null null으로 매핑`() {
            val result = SaraminCodeMapper.mapSalaryRange("")
            assertEquals(null to null, result)
        }
    }

    @Nested
    @DisplayName("mapWorkRegion")
    inner class MapWorkRegionTests {

        @Test
        fun `locationCode 101000은 SEOUL로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("101000")
            assertEquals(WorkRegion.SEOUL, result)
        }

        @Test
        fun `locationCode 102000은 GYEONGGI로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("102000")
            assertEquals(WorkRegion.GYEONGGI, result)
        }

        @Test
        fun `locationCode 103000은 GWANGJU로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("103000")
            assertEquals(WorkRegion.GWANGJU, result)
        }

        @Test
        fun `locationCode 104000은 DAEGU로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("104000")
            assertEquals(WorkRegion.DAEGU, result)
        }

        @Test
        fun `locationCode 105000은 DAEJEON으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("105000")
            assertEquals(WorkRegion.DAEJEON, result)
        }

        @Test
        fun `locationCode 106000은 BUSAN으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("106000")
            assertEquals(WorkRegion.BUSAN, result)
        }

        @Test
        fun `locationCode 107000은 ULSAN으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("107000")
            assertEquals(WorkRegion.ULSAN, result)
        }

        @Test
        fun `locationCode 108000은 INCHEON으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("108000")
            assertEquals(WorkRegion.INCHEON, result)
        }

        @Test
        fun `locationCode 109000은 GANGWON으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("109000")
            assertEquals(WorkRegion.GANGWON, result)
        }

        @Test
        fun `locationCode 110000은 GYEONGNAM으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("110000")
            assertEquals(WorkRegion.GYEONGNAM, result)
        }

        @Test
        fun `locationCode 111000은 GYEONGBUK으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("111000")
            assertEquals(WorkRegion.GYEONGBUK, result)
        }

        @Test
        fun `locationCode 112000은 JEONNAM으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("112000")
            assertEquals(WorkRegion.JEONNAM, result)
        }

        @Test
        fun `locationCode 113000은 JEONBUK으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("113000")
            assertEquals(WorkRegion.JEONBUK, result)
        }

        @Test
        fun `locationCode 114000은 CHUNGNAM으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("114000")
            assertEquals(WorkRegion.CHUNGNAM, result)
        }

        @Test
        fun `locationCode 115000은 CHUNGBUK으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("115000")
            assertEquals(WorkRegion.CHUNGBUK, result)
        }

        @Test
        fun `locationCode 116000은 JEJU로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("116000")
            assertEquals(WorkRegion.JEJU, result)
        }

        @Test
        fun `locationCode 118000은 SEJONG으로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("118000")
            assertEquals(WorkRegion.SEJONG, result)
        }

        @Test
        fun `unknown locationCode는 null로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("999000")
            assertNull(result)
        }

        @Test
        fun `empty string은 null로 매핑`() {
            val result = SaraminCodeMapper.mapWorkRegion("")
            assertNull(result)
        }

        @Test
        fun `prefix만 추출하여 매핑 suffix 무시`() {
            val result = SaraminCodeMapper.mapWorkRegion("101999")
            assertEquals(WorkRegion.SEOUL, result)
        }
    }

    @Nested
    @DisplayName("extractDistrict")
    inner class ExtractDistrictTests {

        @Test
        fun `서울과 강남구 형식에서 강남구 추출`() {
            val result = SaraminCodeMapper.extractDistrict("서울 > 강남구")
            assertEquals("강남구", result)
        }

        @Test
        fun `경기와 성남시 분당구 형식에서 성남시 분당구 추출`() {
            val result = SaraminCodeMapper.extractDistrict("경기 > 성남시 분당구")
            assertEquals("성남시 분당구", result)
        }

        @Test
        fun `구분자가 없는 서울은 null로 반환`() {
            val result = SaraminCodeMapper.extractDistrict("서울")
            assertNull(result)
        }

        @Test
        fun `empty string은 null로 반환`() {
            val result = SaraminCodeMapper.extractDistrict("")
            assertNull(result)
        }

        @Test
        fun `구분자만 있는 경우 null로 반환`() {
            val result = SaraminCodeMapper.extractDistrict("서울 > ")
            assertNull(result)
        }

        @Test
        fun `여러 개의 구분자가 있을 때 두 번째 부분만 추출`() {
            val result = SaraminCodeMapper.extractDistrict("서울 > 강남구 > 역삼동")
            assertEquals("강남구", result)
        }

        @Test
        fun `공백이 있는 지역명 정상 추출`() {
            val result = SaraminCodeMapper.extractDistrict("경기 > 수원시 팔달구")
            assertEquals("수원시 팔달구", result)
        }
    }
}
