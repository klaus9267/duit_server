package duit.server.application.common

import duit.server.domain.common.dto.ErrorResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement

/**
 * 관리자 권한이 필요한 엔드포인트에 붙이는 어노테이션
 * - Swagger에서 자동으로 🔒 자물쇠 표시
 * - 401/403 응답 자동 추가
 * - AdminAuthorizationInterceptor 가 런타임에 관리자 여부를 검증함
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
                        "code": "UNAUTHORIZED",
                        "message": "인증이 필요합니다.",
                        "timestamp": "2024-01-01T10:00:00"
                    }
                    """
                )]
            )]
        ),
        ApiResponse(
            responseCode = "403",
            description = "관리자 권한 필요",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "관리자 권한 필요",
                    value = """
                    {
                        "code": "FORBIDDEN",
                        "message": "관리자 권한이 필요합니다.",
                        "timestamp": "2024-01-01T10:00:00"
                    }
                    """
                )]
            )]
        )
    ]
)
annotation class RequireAdmin
