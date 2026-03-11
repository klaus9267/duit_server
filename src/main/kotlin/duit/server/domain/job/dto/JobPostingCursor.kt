package duit.server.domain.job.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import duit.server.domain.job.entity.JobPosting
import java.time.LocalDateTime
import java.util.Base64

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = JobPostingCursor.CreatedAtCursor::class, name = "CREATED_AT"),
    JsonSubTypes.Type(value = JobPostingCursor.ExpiresAtCursor::class, name = "EXPIRES_AT"),
    JsonSubTypes.Type(value = JobPostingCursor.SalaryCursor::class, name = "SALARY"),
)
sealed interface JobPostingCursor {

    data class CreatedAtCursor(
        val createdAt: LocalDateTime,
        val id: Long
    ) : JobPostingCursor

    data class ExpiresAtCursor(
        val expiresAt: LocalDateTime,
        val id: Long
    ) : JobPostingCursor

    data class SalaryCursor(
        val salaryMin: Long,
        val id: Long
    ) : JobPostingCursor

    companion object {
        internal val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())

        fun decode(encoded: String, field: JobPostingSortField): JobPostingCursor {
            return try {
                val decoded = String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
                val cursorType = when (field) {
                    JobPostingSortField.CREATED_AT -> CreatedAtCursor::class.java
                    JobPostingSortField.EXPIRES_AT -> ExpiresAtCursor::class.java
                    JobPostingSortField.SALARY -> SalaryCursor::class.java
                }
                objectMapper.readValue(decoded, cursorType)
            } catch (e: Exception) {
                throw IllegalArgumentException("유효하지 않은 커서입니다: ${e.message}", e)
            }
        }

        fun fromJobPosting(jobPosting: JobPosting, field: JobPostingSortField): JobPostingCursor {
            val id = jobPosting.id ?: throw IllegalArgumentException("JobPosting ID must not be null")

            return when (field) {
                JobPostingSortField.CREATED_AT -> CreatedAtCursor(
                    createdAt = jobPosting.createdAt,
                    id = id
                )
                JobPostingSortField.EXPIRES_AT -> ExpiresAtCursor(
                    expiresAt = jobPosting.expiresAt
                        ?: throw IllegalArgumentException("expiresAt must not be null for EXPIRES_AT sort"),
                    id = id
                )
                JobPostingSortField.SALARY -> SalaryCursor(
                    salaryMin = jobPosting.salaryMin
                        ?: throw IllegalArgumentException("salaryMin must not be null for SALARY sort"),
                    id = id
                )
            }
        }
    }
}

fun JobPostingCursor.encode(): String {
    val json = JobPostingCursor.objectMapper.writeValueAsString(this)
    return Base64.getUrlEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
}
