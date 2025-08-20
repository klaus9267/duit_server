package duit.server.domain.user.controller.docs

import duit.server.domain.common.dto.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "닉네임 중복 확인",
    description = """
회원가입이나 닉네임 변경 시 닉네임 중복 여부를 확인합니다.

**검증 규칙:**
- 2자 이상 10자 이하
- 한글, 영문, 숫자만 허용
- 특수문자 사용 불가

**인증 불필요:**
- 회원가입 전에도 사용 가능한 공개 API
"""
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "사용 가능한 닉네임"
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 사용 중인 닉네임",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "닉네임 중복",
                    value = """
                    {
                        "code": "USER_005",
                        "message": "이미 사용 중인 닉네임입니다.",
                        "timestamp": "2024-01-01T10:00:00"
                    }
                    """
                )]
            )]
        )
    ]
)
annotation class CheckNicknameDuplicateApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "현재 사용자 정보 조회",
    description = """
JWT 토큰을 기반으로 현재 로그인한 사용자의 정보를 조회합니다.

**반환 정보:**
- 사용자 기본 정보 (ID, 이메일, 닉네임)
- 소셜 로그인 정보 (provider, providerId)
- 알림 설정 (푸시 알림, 마케팅 알림)
- 가입일시, 수정일시
""",
    security = [SecurityRequirement(name = "bearerAuth")]
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "사용자 정보 조회 성공",
            useReturnTypeSchema = true
        )
    ]
)
annotation class GetCurrentUserApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "현재 사용자 닉네임 수정",
    description = """
현재 로그인한 사용자의 닉네임을 수정합니다.

**수정 규칙:**
- 중복 닉네임 검증
- 2자 이상 10자 이하
- 한글, 영문, 숫자만 허용

**주의사항:**
- 수정 후 즉시 적용됨
- 이전 닉네임으로 되돌릴 수 없음
""",
    security = [SecurityRequirement(name = "bearerAuth")]
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "닉네임 수정 성공",
            useReturnTypeSchema = true
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 사용 중인 닉네임",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "닉네임 중복",
                    value = """
                    {
                        "code": "USER_005",
                        "message": "이미 사용 중인 닉네임입니다.",
                        "timestamp": "2024-01-01T10:00:00"
                    }
                    """
                )]
            )]
        )
    ]
)
annotation class UpdateCurrentUserNicknameApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "회원탈퇴",
    description = """
현재 로그인한 사용자의 계정을 삭제합니다.

**삭제 범위:**
- 사용자 기본 정보
- 북마크 정보
- 관련된 모든 데이터

**주의사항:**
- 삭제 후 복구 불가능
- 관련 데이터 모두 삭제됨
- JWT 토큰 무효화
""",
    security = [SecurityRequirement(name = "bearerAuth")]
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "204",
            description = "회원탈퇴 성공"
        )
    ]
)
annotation class WithdrawApi