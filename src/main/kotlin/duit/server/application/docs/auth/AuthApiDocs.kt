package duit.server.application.docs.auth

import duit.server.domain.common.controller.TokenResponse
import duit.server.domain.common.dto.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "JWT 토큰 발급",
    description = """
개발 및 테스트 용도로 사용자 ID를 기반으로 JWT 토큰을 발급합니다.

**개발/테스트 전용:**
- 실제 운영에서는 소셜 로그인으로 대체 예정
- 카카오, 구글, 네이버 로그인 연동 준비 중

**토큰 정보:**
- Access Token: 24시간 유효
- Bearer 타입
- 모든 인증 API에서 사용 가능

**사용 방법:**
- Authorization 헤더에 "Bearer {token}" 형태로 포함
"""
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "토큰 발급 성공",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = TokenResponse::class),
                examples = [ExampleObject(
                    name = "성공 응답",
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzA0MDk2MDAwLCJleHAiOjE3MDQxODI0MDB9.abc123...",
                        "tokenType": "Bearer"
                    }
                    """
                )]
            )]
        ),
        ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "사용자 없음",
                    value = """
                    {
                        "code": "USER_001",
                        "message": "사용자를 찾을 수 없습니다.",
                        "timestamp": "2024-01-01T10:00:00"
                    }
                    """
                )]
            )]
        )
    ]
)
annotation class IssueTokenApi