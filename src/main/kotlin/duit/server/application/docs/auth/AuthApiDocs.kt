package duit.server.application.docs.auth

import duit.server.domain.auth.dto.AuthResponse
import duit.server.domain.auth.controller.TokenResponse
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
//    content =
//        [
//        Content(
//            mediaType = "application/json",
//            schema = Schema(implementation = String::class),
//            examples = [
//                ExampleObject(
//                    name = "소셜 로그인 요청",
//                    summary = "Firebase ID 토큰으로 로그인",
//                    description = "클라이언트에서 Firebase Authentication을 통해 받은 ID 토큰",
//                    value = """
//                    {
//                        "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjFlOWdkazcifQ.ewogImlzcyI6ICJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vZXhhbXBsZS1wcm9qZWN0IiwKICJhdWQiOiAiZXhhbXBsZS1wcm9qZWN0IiwKICJhdXRoX3RpbWUiOiAxNjg5MTIzNDU2LAogICJ1c2VyX2lkIjogInVzZXJfMTIzIiwKICJzdWIiOiAidXNlcl8xMjMiLAogICJpYXQiOiAxNjg5MTIzNDU2LAogICJleHAiOiAxNjg5MTI3MDU2LAogICJlbWFpbCI6ICJ1c2VyQGV4YW1wbGUuY29tIiwKICJlbWFpbF92ZXJpZmllZCI6IHRydWUsCiAgImZpcmViYXNlIjogewogICAgInNpZ25faW5fcHJvdmlkZXIiOiAiZ29vZ2xlLmNvbSIsCiAgICAiaWRlbnRpdGllcyI6IHsKICAgICAgImdvb2dsZS5jb20iOiBbInVzZXJfMTIzIl0sCiAgICAgICJlbWFpbCI6IFsidXNlckBleGFtcGxlLmNvbSJdCiAgICB9CiAgfQp9.signature"
//                    }
//                    """
//                )
//            ]
//        )
//    ]
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "소셜 로그인 성공",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = AuthResponse::class),
                    examples = [
                        ExampleObject(
                            name = "기존 사용자 로그인",
                            summary = "기존 사용자가 로그인한 경우",
                            value = """
                            {
                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsImlhdCI6MTY4OTEyMzQ1NiwiZXhwIjoxNjg5MTI3MDU2fQ.signature",
                                "tokenType": "Bearer",
                                "user": {
                                    "id": 1,
                                    "email": "user@example.com",
                                    "nickname": "홍길동",
                                    "allowPushAlarm": true,
                                    "allowMarketingAlarm": true,
                                    "createdAt": "2024-07-12T10:30:00",
                                    "updatedAt": "2024-07-12T10:30:00"
                                },
                                "isNewUser": false
                            }
                            """
                        ),
                        ExampleObject(
                            name = "신규 사용자 회원가입",
                            summary = "신규 사용자가 회원가입한 경우",
                            value = """
                            {
                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjIsImlhdCI6MTY4OTEyMzQ1NiwiZXhwIjoxNjg5MTI3MDU2fQ.signature",
                                "tokenType": "Bearer",
                                "user": {
                                    "id": 2,
                                    "email": "newuser@example.com",
                                    "nickname": "김철수",
                                    "allowPushAlarm": true,
                                    "allowMarketingAlarm": true,
                                    "createdAt": "2024-07-12T11:00:00",
                                    "updatedAt": "2024-07-12T11:00:00"
                                },
                                "isNewUser": true
                            }
                            """
                        )
                    ]
                )
            ]
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