package duit.server.domain.bookmark.` controller`.docs

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
    summary = "북마크 목록 조회",
    description = """
현재 로그인한 사용자가 북마크한 행사 목록을 페이지네이션으로 조회합니다.

**조회 정보:**
- 북마크한 행사 목록
- 행사 기본 정보 (제목, 일정, 주최기관 등)
- 페이지네이션 정보

**정렬 순서:**
- 북마크 등록 순서 (최신순)

**사용 목적:**
- 마이페이지에서 북마크 목록 표시
- 관심 행사 관리
""",
    security = [SecurityRequirement(name = "bearerAuth")]
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "북마크 목록 조회 성공",
            useReturnTypeSchema = true
        )
    ]
)
annotation class GetBookmarksApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "북마크 생성/취소",
    description = """
특정 행사에 대한 북마크를 생성하거나 취소합니다.

**토글 방식:**
- 북마크가 없으면 → 생성
- 북마크가 있으면 → 삭제

**비즈니스 로직:**
- 동일한 사용자는 같은 행사를 한 번만 북마크 가능
- 삭제된 행사는 북마크 불가능
- 승인되지 않은 행사도 북마크 가능

**응답:**
- 성공 시 204 No Content
- 별도의 응답 데이터 없음
""",
    security = [SecurityRequirement(name = "bearerAuth")]
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "204",
            description = "북마크 생성/취소 성공",
        ),
        ApiResponse(
            responseCode = "404",
            description = "행사를 찾을 수 없음",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "행사 없음",
                    value = """
                    {
                        "code": "EVENT_001",
                        "message": "이벤트를 찾을 수 없습니다.",
                        "timestamp": "2024-01-01T10:00:00"
                    }
                    """
                )]
            )]
        )
    ]
)
annotation class BookmarkEventApi