package duit.server.domain.job.dto

enum class JobPostingSortField(val displayName: String) {
    CREATED_AT("createdAt"),
    EXPIRES_AT("expiresAt"),
    SALARY("salaryMin"),
}
