package duit.server.domain.job.entity

import duit.server.domain.job.entity.MinimumWage.WorkPattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("MinimumWage 단위 테스트")
class MinimumWageTest {

    private val today = LocalDate.of(2026, 10, 15)

    private fun isBelow(
        salaryType: SalaryType?,
        amount: Long?,
        weeklyHours: Double? = null,
        workPattern: WorkPattern = WorkPattern.FULL_TIME,
        closeDate: LocalDate? = null,
    ) = MinimumWage.isBelow(salaryType, amount, weeklyHours, workPattern, today, closeDate)

    @Nested
    @DisplayName("연도별 최저임금")
    inner class HourlyMinimumWageTests {

        @Test
        fun `고시된 연도는 표의 금액을 쓴다`() {
            assertEquals(10_030L, MinimumWage.hourlyMinimumWage(2025))
            assertEquals(10_320L, MinimumWage.hourlyMinimumWage(2026))
            assertEquals(10_700L, MinimumWage.hourlyMinimumWage(2027))
        }

        @Test
        fun `표 이후 연도는 마지막 금액, 이전 연도는 첫 금액을 쓴다`() {
            assertEquals(10_700L, MinimumWage.hourlyMinimumWage(2030))
            assertEquals(10_030L, MinimumWage.hourlyMinimumWage(2024))
        }

        @Test
        fun `접수 마감일이 최저임금이 오르는 다음 해면 그 금액을 적용한다`() {
            assertEquals(10_320L, MinimumWage.applicableHourlyMinimumWage(today, null))
            assertEquals(10_320L, MinimumWage.applicableHourlyMinimumWage(today, LocalDate.of(2026, 12, 31)))
            assertEquals(10_700L, MinimumWage.applicableHourlyMinimumWage(today, LocalDate.of(2027, 1, 10)))
        }
    }

    @Nested
    @DisplayName("임금형태별 판정")
    inner class IsBelowTests {

        @Test
        fun `시급은 금액을 그대로 비교한다`() {
            assertTrue(isBelow(SalaryType.HOURLY, 10_319))
            assertFalse(isBelow(SalaryType.HOURLY, 10_320))
            assertTrue(isBelow(SalaryType.HOURLY, 10_500, closeDate = LocalDate.of(2027, 1, 31)))
        }

        @Test
        fun `일급은 8시간 기준으로 비교하고 시간제는 판정하지 않는다`() {
            assertTrue(isBelow(SalaryType.DAILY, 82_559))
            assertFalse(isBelow(SalaryType.DAILY, 82_560))
            assertFalse(isBelow(SalaryType.DAILY, 50_000, workPattern = WorkPattern.PART_TIME))
        }

        @Test
        fun `전일제 월급과 연봉은 월 209시간으로 비교한다`() {
            assertTrue(isBelow(SalaryType.MONTHLY, 2_156_879))
            assertFalse(isBelow(SalaryType.MONTHLY, 2_156_880))
            assertTrue(isBelow(SalaryType.ANNUAL, 25_882_559))
            assertFalse(isBelow(SalaryType.ANNUAL, 25_882_560))
        }

        @Test
        fun `주 소정근로시간이 있으면 주휴시간을 더한 월 유급시간으로 비교한다`() {
            // 주 20시간: (20 + 4) × 4.345 = 104.28시간 → 1,076,169.6원
            assertTrue(isBelow(SalaryType.MONTHLY, 1_000_000, weeklyHours = 20.0, workPattern = WorkPattern.PART_TIME))
            assertFalse(isBelow(SalaryType.MONTHLY, 1_100_000, weeklyHours = 20.0, workPattern = WorkPattern.PART_TIME))
            // 주 15시간 미만은 주휴시간 없음: 10 × 4.345 = 43.45시간 → 448,404원
            assertFalse(isBelow(SalaryType.MONTHLY, 450_000, weeklyHours = 10.0, workPattern = WorkPattern.PART_TIME))
        }

        @Test
        fun `주 40시간을 넘는 근로시간은 연장근로로 보고 40시간까지만 쓴다`() {
            assertFalse(isBelow(SalaryType.MONTHLY, 2_200_000, weeklyHours = 52.0))
        }

        @Test
        fun `근로시간을 모르는 시간제·고용형태 불명 월급은 판정하지 않는다`() {
            assertFalse(isBelow(SalaryType.MONTHLY, 1_000_000, workPattern = WorkPattern.PART_TIME))
            assertFalse(isBelow(SalaryType.ANNUAL, 10_000_000, workPattern = WorkPattern.UNKNOWN))
        }

        @Test
        fun `금액이나 임금형태가 없으면 판정하지 않는다`() {
            assertFalse(isBelow(SalaryType.HOURLY, null))
            assertFalse(isBelow(SalaryType.HOURLY, 0))
            assertFalse(isBelow(null, 1_000))
        }
    }

    @Nested
    @DisplayName("근로조건 해석")
    inner class ParsingTests {

        @Test
        fun `근무시간 문구에서 주 소정근로시간을 읽는다`() {
            assertEquals(40.0, MinimumWage.parseWeeklyHours("주 40시간"))
            assertEquals(40.0, MinimumWage.parseWeeklyHours("주 5일 근무 , (근무시간) (오전) 9시 00분 ~ (오후) 6시 00분 , 주 소정근로시간: 40시간"))
            assertEquals(20.0, MinimumWage.parseWeeklyHours("주 소정근로시간 : 20 시간"))
            assertEquals(null, MinimumWage.parseWeeklyHours("주 5일 근무"))
            assertEquals(null, MinimumWage.parseWeeklyHours("주휴수당 별도 지급"))
            assertEquals(null, MinimumWage.parseWeeklyHours(null))
        }

        @Test
        fun `시간제 단서가 있으면 시간제로 본다`() {
            assertEquals(WorkPattern.PART_TIME, MinimumWage.workPatternOf("11", "기간의 정함이 없는 근로계약(시간(선택)제)"))
            assertEquals(WorkPattern.PART_TIME, MinimumWage.workPatternOf("10", "기간의 정함이 없는 근로계약", "파트타임 간호사 모집"))
            assertEquals(WorkPattern.PART_TIME, MinimumWage.workPatternOf(null, "파트타임"))
        }

        @Test
        fun `전일제 고용형태만 전일제로 본다`() {
            assertEquals(WorkPattern.FULL_TIME, MinimumWage.workPatternOf("10", null))
            assertEquals(WorkPattern.FULL_TIME, MinimumWage.workPatternOf(null, "기간의 정함이 있는 근로계약"))
            assertEquals(WorkPattern.FULL_TIME, MinimumWage.workPatternOf(null, "정규직"))
            assertEquals(WorkPattern.UNKNOWN, MinimumWage.workPatternOf("4", "파견근로"))
            assertEquals(WorkPattern.UNKNOWN, MinimumWage.workPatternOf(null, null))
        }
    }
}
