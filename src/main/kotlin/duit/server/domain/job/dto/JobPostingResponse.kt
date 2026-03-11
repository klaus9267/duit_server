package duit.server.domain.job.dto

import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.EducationLevel
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.SalaryType
import duit.server.domain.job.entity.SourceType
import duit.server.domain.job.entity.WorkRegion
import java.time.LocalDateTime

data class JobPostingResponse(
    val id: Long,
    val sourceType: SourceType,
    val title: String,
    val companyName: String,
    val jobCategory: String?,
    val location: String?,
    val workRegion: WorkRegion?,
    val workDistrict: String?,
    val employmentType: EmploymentType?,
    val careerDescription: String,
    val educationLevel: EducationLevel?,
    val salaryDescription: String,
    val salaryType: SalaryType?,
    val postingUrl: String,
    val postedAt: LocalDateTime?,
    val expiresAt: LocalDateTime?,
    val closeType: CloseType,
    val isBookmarked: Boolean = false,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(jobPosting: JobPosting, isBookmarked: Boolean = false) = JobPostingResponse(
            id = jobPosting.id!!,
            sourceType = jobPosting.sourceType,
            title = jobPosting.title,
            companyName = jobPosting.companyName,
            jobCategory = jobPosting.jobCategory,
            location = jobPosting.location,
            workRegion = jobPosting.workRegion,
            workDistrict = jobPosting.workDistrict,
            employmentType = jobPosting.employmentType,
            careerDescription = jobPosting.careerDescription,
            educationLevel = jobPosting.educationLevel,
            salaryDescription = jobPosting.salaryDescription,
            salaryType = jobPosting.salaryType,
            postingUrl = jobPosting.postingUrl,
            postedAt = jobPosting.postedAt,
            expiresAt = jobPosting.expiresAt,
            closeType = jobPosting.closeType,
            isBookmarked = isBookmarked,
            createdAt = jobPosting.createdAt,
        )
    }
}
