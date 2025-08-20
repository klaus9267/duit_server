package duit.server.domain.auth.controller.docs

import duit.server.domain.common.dto.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
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
            useReturnTypeSchema = true
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

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "소셜 로그인/회원가입",
    description = """
Firebase ID 토큰을 사용하여 소셜 로그인 또는 회원가입을 수행합니다.

**지원 로그인 방식:**
- Kakao

**처리 과정:**
1. Firebase ID 토큰 검증
2. 기존 사용자 조회 (providerId 기준)
3. 신규 사용자인 경우 자동 회원가입
4. JWT 액세스 토큰 발급 및 사용자 정보 반환

**닉네임 중복 처리:**
닉네임이 이미 존재하는 경우 자동으로 숫자를 붙여서 유니크한 닉네임을 생성합니다.
(예: "홍길동" -> "홍길동1" -> "홍길동2")
"""
)
@RequestBody(
    description = "소셜 로그인 요청 정보",
    required = true,
    useParameterTypeSchema = true
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "소셜 로그인 성공",
            useReturnTypeSchema = true
        ),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (유효하지 않은 ID 토큰)",
            content = [
                Content(
                    examples = [
                        ExampleObject(
                            value = """
                            {
                                "code": "FIREBASE_001",
                                "message": "유효하지 않은 Firebase 토큰입니다.",
                                "traceId": "12345678-1234-5678-9012-123456789012"
                            }
                            """
                        )
                    ]
                )
            ]
        ),
        ApiResponse(
            responseCode = "500",
            description = "서버 오류 (토큰 검증 실패)",
            content = [
                Content(
                    examples = [
                        ExampleObject(
                            value = """
                            {
                                "code": "FIREBASE_002",
                                "message": "Firebase 토큰 검증에 실패했습니다.",
                                "traceId": "12345678-1234-5678-9012-123456789012"
                            }
                            """
                        )
                    ]
                )
            ]
        )
    ]
)
annotation class SocialLoginApi