package duit.server.domain.common.dto.pagination

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 커서 기반 페이지네이션 응답
 *
 * @param T 컨텐츠 타입
 * @property content 현재 페이지의 데이터 리스트
 * @property pageInfo 페이지 메타데이터 (hasNext, nextCursor, pageSize)
 */
@Schema(description = "커서 기반 페이지네이션 응답")
data class CursorPageResponse<T>(
    @Schema(description = "현재 페이지 데이터")
    val content: List<T> = emptyList(),

    @Schema(description = "페이지 메타정보")
    val pageInfo: CursorPageInfo
)

/**
 * 커서 페이지네이션 메타정보
 *
 * @property hasNext 다음 페이지 존재 여부
 * @property nextCursor 다음 페이지 조회를 위한 커서 (Base64 인코딩, hasNext가 true일 때만 존재)
 * @property pageSize 현재 페이지에 포함된 실제 아이템 수
 */
@Schema(description = "커서 페이지네이션 메타정보")
data class CursorPageInfo(
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,

    @Schema(description = "다음 페이지 커서 (Base64 인코딩)", example = "eyJjcmVhdGVkQXQiOiIyMDI1LTAxLTE1VDEwOjMwOjAwIiwiaWQiOjEyM30=", nullable = true)
    val nextCursor: String?,

    @Schema(description = "현재 페이지 아이템 수", example = "10")
    val pageSize: Int
)
