package duit.server.infrastructure.external.job.saramin

import com.fasterxml.jackson.annotation.JsonProperty

data class SaraminApiResponse(
    val jobs: Jobs
) {
    data class Jobs(
        val count: Int,
        val start: Int,
        val total: String,
        val job: List<Job>
    )

    data class Job(
        val id: String,
        val url: String,
        val active: Int,
        @JsonProperty("posting-timestamp") val postingTimestamp: Long,
        @JsonProperty("expiration-timestamp") val expirationTimestamp: Long,
        @JsonProperty("close-type") val closeType: CodeName,
        val company: Company,
        val position: Position,
        val salary: CodeName
    )

    data class Company(
        val detail: CompanyDetail
    )

    data class CompanyDetail(
        val name: String
    )

    data class Position(
        val title: String,
        val location: Location,
        @JsonProperty("job-type") val jobType: CodeName,
        @JsonProperty("job-code") val jobCode: CodeName,
        @JsonProperty("experience-level") val experienceLevel: ExperienceLevel,
        @JsonProperty("required-education-level") val requiredEducationLevel: CodeName
    )

    data class Location(
        val code: String,
        val name: String
    )

    data class ExperienceLevel(
        val code: Int,
        val min: Int,
        val max: Int,
        val name: String
    )

    data class CodeName(
        val code: String,
        val name: String
    )
}
