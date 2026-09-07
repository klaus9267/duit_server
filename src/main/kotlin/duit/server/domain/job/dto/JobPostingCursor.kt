package duit.server.domain.job.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import duit.server.domain.job.entity.JobPosting
import java.time.LocalDateTime
import java.util.Base64

sealed interface JobPostingCursor {
    data class CreatedAtCursor(val postedAt: LocalDateTime? = null, val id: Long) : JobPostingCursor
    data class ExpiresAtCursor(val expiresAt: LocalDateTime?, val id: Long) : JobPostingCursor
    data class SalaryCursor(val salaryMin: Long?, val id: Long) : JobPostingCursor

    companion object {
        internal val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())

        fun decode(encoded: String, field: JobPostingSortField): JobPostingCursor =
            try {
                val decoded = String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
                val cursorNode = objectMapper.readTree(decoded)
                val cursorType = when (field) {
                    JobPostingSortField.CREATED_AT -> CreatedAtCursor::class.java
                    JobPostingSortField.EXPIRES_AT -> ExpiresAtCursor::class.java.also {
                        require(cursorNode.has("expiresAt")) { "EXPIRES_AT 커서에 expiresAt이 필요합니다" }
                    }
                    JobPostingSortField.SALARY -> SalaryCursor::class.java.also {
                        require(cursorNode.has("salaryMin")) { "SALARY 커서에 salaryMin이 필요합니다" }
                    }
                }
                objectMapper.treeToValue(cursorNode, cursorType)
            } catch (e: Exception) {
                throw IllegalArgumentException("유효하지 않은 커서입니다: ${e.message}", e)
            }

        fun fromJobPosting(jobPosting: JobPosting, field: JobPostingSortField): JobPostingCursor {
            val id = jobPosting.id ?: throw IllegalArgumentException("JobPosting ID must not be null")

            return when (field) {
                JobPostingSortField.CREATED_AT -> CreatedAtCursor(jobPosting.postedAt, id)
                JobPostingSortField.EXPIRES_AT -> ExpiresAtCursor(jobPosting.expiresAt, id)
                JobPostingSortField.SALARY -> SalaryCursor(jobPosting.salaryMin, id)
            }
        }
    }
}

fun JobPostingCursor.encode(): String {
    val json = JobPostingCursor.objectMapper.writeValueAsString(this)
    return Base64.getUrlEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
}
