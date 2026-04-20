package duit.server.infrastructure.external.job.dto

import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.EducationLevel
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.SalaryType
import duit.server.domain.job.entity.WorkRegion
import java.time.LocalDateTime

data class JobFetchResult(
    val externalId: String,
    val title: String,
    val companyName: String,
    val businessNumber: String?,
    val jobCategory: String?,
    val location: String?,
    val zipCode: String?,
    val roadNameAddress: String?,
    val basicAddress: String?,
    val detailAddress: String?,
    val infoService: String?,
    val workRegion: WorkRegion?,
    val workDistrict: String?,
    val employmentType: EmploymentType?,
    val careerMin: Int?,
    val careerMax: Int?,
    val educationLevel: EducationLevel?,
    val salaryMin: Long?,
    val salaryMax: Long?,
    val salaryType: SalaryType?,
    val postingUrl: String,
    val postedAt: LocalDateTime?,
    val expiresAt: LocalDateTime?,
    val closeType: CloseType,
    val isActive: Boolean,
    val workHoursPerWeek: Int?,
)
