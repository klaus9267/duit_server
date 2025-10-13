package duit.server.domain.common.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 에러 응답 DTO
 */
@Schema(description = "에러 응답")
data class ErrorResponse(
    @Schema(description = "에러 코드", example = "USER_NOT_FOUND")
    val code: String,
    
    @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다.")
    val message: String,
    
    @Schema(description = "상세 메시지")
    val details: String? = null,
    
    @Schema(description = "필드별 검증 에러")
    val fieldErrors: List<FieldError>? = null,
    
    @Schema(description = "발생 시간", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now(),
    
    @Schema(description = "요청 경로", example = "/api/users/1")
    val path: String? = null,
    
    @Schema(description = "추적 ID", example = "abc123-def456")
    val traceId: String? = null
)

/**
 * 필드 검증 에러 DTO
 */
@Schema(description = "필드 검증 에러")
data class FieldError(
    @Schema(description = "필드명", example = "email")
    val field: String,
    
    @Schema(description = "거부된 값", example = "invalid-email")
    val rejectedValue: Any?,
    
    @Schema(description = "에러 메시지", example = "올바른 이메일 형식이 아닙니다.")
    val message: String
)
