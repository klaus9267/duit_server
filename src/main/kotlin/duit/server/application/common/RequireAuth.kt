package duit.server.application.common

import duit.server.domain.common.dto.ErrorResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement

/**
 * 인증이 필요한 엔드포인트에 붙이는 어노테이션
 * - Swagger에서 자동으로 🔒 자물쇠 표시
 * - 401/403 응답 자동 추가
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "401",
            description = "인증 필요",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "인증 필요",
                    value = """
                    {
                        "code": "COMMON_003",
                        "message": "인증이 필요합니다.",
                        "timestamp": "2024-01-01T10:00:00"
                    }
                    """
                )]
            )]
        ),
        ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "접근 권한 없음",
                    value = """
                    {
                        "code": "COMMON_004",
                        "message": "접근 권한이 없습니다.",
                        "timestamp": "2024-01-01T10:00:00"
                    }
                    """
                )]
            )]
        )
    ]
)
annotation class RequireAuth
