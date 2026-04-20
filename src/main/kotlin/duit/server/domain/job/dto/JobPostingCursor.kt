package duit.server.domain.job.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import duit.server.domain.job.entity.JobPosting
import java.util.Base64

data class JobPostingCursor(
    val id: Long,
) {
    companion object {
        internal val objectMapper = ObjectMapper().registerKotlinModule()

        fun decode(encoded: String): JobPostingCursor =
            try {
                val decoded = String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
                objectMapper.readValue(decoded, JobPostingCursor::class.java)
            } catch (e: Exception) {
                throw IllegalArgumentException("유효하지 않은 커서입니다: ${e.message}", e)
            }

        fun fromJobPosting(jobPosting: JobPosting): JobPostingCursor =
            JobPostingCursor(jobPosting.id ?: throw IllegalArgumentException("JobPosting ID must not be null"))
    }
}

fun JobPostingCursor.encode(): String {
    val json = JobPostingCursor.objectMapper.writeValueAsString(this)
    return Base64.getUrlEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
}
