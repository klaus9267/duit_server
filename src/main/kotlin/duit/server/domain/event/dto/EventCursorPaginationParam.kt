package duit.server.domain.event.dto

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Event API v2 커서 기반 페이지네이션 파라미터
 *
 * @property cursor Base64로 인코딩된 커서 (첫 페이지는 null)
 * @property size 페이지 크기 (1~100, 기본값: 10)
 * @property field 정렬 필드 (기본값: CREATED_AT)
 * @property types 이벤트 타입 필터 (다중 선택 가능)
 * @property status 이벤트 상태 필터 (statusGroup과 둘 중 하나만 사용)
 * @property statusGroup 이벤트 상태 그룹 필터 (status와 둘 중 하나만 사용, 기본값: ACTIVE)
 * @property bookmarked 북마크한 이벤트만 조회 (기본값: false)
 */
@Schema(description = "Event API v2 커서 기반 페이지네이션 파라미터")
data class EventCursorPaginationParam(
    @get:Parameter(description = "다음 페이지 커서 (Base64 인코딩). 첫 페이지는 null")
    val cursor: String? = null,

    @get:Parameter(
        description = "페이지 크기 (1~100)",
        example = "10"
    )
    @get:Schema(minimum = "1", maximum = "100", defaultValue = "10")
    val size: Int = 10,

    @get:Parameter(
        description = """정렬 필드
- CREATED_AT: 등록일 (최신순)
- START_DATE: 시작일 (임박순/종료순 - 상태에 따라 동적 변경)
- RECRUITMENT_DEADLINE: 모집 마감일 (임박순/종료순 - 상태에 따라 동적 변경)
- VIEW_COUNT: 조회수 (많은순)
- ID: ID (최신순)""",
        example = "CREATED_AT"
    )
    @get:Schema(defaultValue = "CREATED_AT")
    val field: PaginationField = PaginationField.CREATED_AT,

    @get:Parameter(description = "이벤트 타입 필터 (다중 선택 가능)")
    val types: List<EventType>? = null,

    @get:Parameter(description = "이벤트 상태 (statusGroup과 둘 중 하나만 사용)")
    val status: EventStatus? = null,

    @get:Parameter(
        description = "이벤트 상태 그룹 (status와 둘 중 하나만 사용)",
        example = "ACTIVE"
    )
    @get:Schema(defaultValue = "ACTIVE")
    val statusGroup: EventStatusGroup? = null,

    @get:Parameter(
        description = "북마크한 이벤트만 조회 (로그인 필요)",
        example = "false"
    )
    @get:Schema(defaultValue = "false")
    val bookmarked: Boolean = false,

    @get:Parameter(description = "검색 키워드 (행사 제목 검색)")
    val searchKeyword: String? = null,

    @get:Parameter(description = "주최자 ID (특정 주최자의 행사만 조회)")
    val hostId: Long? = null
) {
    init {
        require(status == null || statusGroup == null) {
            "status와 statusGroup 중 하나만 사용 가능합니다"
        }
        require(size in 1..100) {
            "size는 1 이상 100 이하여야 합니다 (현재: $size)"
        }
    }
}
