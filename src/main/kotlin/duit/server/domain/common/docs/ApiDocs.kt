package duit.server.domain.common.docs

import duit.server.domain.common.dto.ErrorResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
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
annotation class AuthApiResponses

