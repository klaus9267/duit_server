package duit.server.domain.event.dto

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

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
 * @property eventDateFrom·eventDateTo 행사일 범위 (선택, 행사 기간과 겹치는 행사)
 * @property recruitmentDateFrom·recruitmentDateTo 모집일 범위 (선택, 모집 기간과 겹치는 행사)
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
    var statusGroup: EventStatusGroup? = null,

    @get:Parameter(
        description = "북마크한 이벤트만 조회 (로그인 필요)",
        example = "false"
    )
    @get:Schema(defaultValue = "false")
    val bookmarked: Boolean = false,

    @get:Parameter(description = "검색 키워드 (행사 제목 또는 주최자명 부분 검색, 여러 주최 중 하나라도 일치)")
    val searchKeyword: String? = null,

    @get:Parameter(description = "주최자 ID (특정 주최자의 행사만 조회)")
    val hostId: Long? = null,

    @get:Parameter(
        description = "행사일 범위 시작 (yyyy-MM-dd, 이 날 포함). 행사 기간(시작일~종료일, 종료일이 없으면 시작일)이 범위와 겹치는 행사만 조회. eventDateTo 만 쓰거나 둘 다 생략 가능",
        example = "2026-11-01"
    )
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val eventDateFrom: LocalDate? = null,

    @get:Parameter(description = "행사일 범위 끝 (yyyy-MM-dd, 이 날 포함)", example = "2026-11-30")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val eventDateTo: LocalDate? = null,

    @get:Parameter(
        description = """모집일 범위 시작 (yyyy-MM-dd, 이 날 포함). 모집 기간이 범위와 겹치는 행사만 조회. recruitmentDateTo 만 쓰거나 둘 다 생략 가능
- 모집 시작일·마감일 모두 있음: 시작일~마감일
- 마감일만 있음: 마감일까지
- 마감일 없음: 행사 시작 시각까지 (모집 시작일도 없으면 시작은 제한 없음)""",
        example = "2026-11-01"
    )
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val recruitmentDateFrom: LocalDate? = null,

    @get:Parameter(description = "모집일 범위 끝 (yyyy-MM-dd, 이 날 포함)", example = "2026-11-30")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val recruitmentDateTo: LocalDate? = null,
) {
    init {
        require(status == null || statusGroup == null) {
            "status, statusGroup 하나만 입력 가능합니다"
        }

        if (status == null && statusGroup == null) {
            statusGroup = EventStatusGroup.ACTIVE
        }

        require(size in 1..100) {
            "size는 1 이상 100 이하여야 합니다 (현재: $size)"
        }

        require(eventDateFrom == null || eventDateTo == null || !eventDateFrom.isAfter(eventDateTo)) {
            "eventDateFrom 은 eventDateTo 보다 늦을 수 없습니다"
        }

        require(recruitmentDateFrom == null || recruitmentDateTo == null || !recruitmentDateFrom.isAfter(recruitmentDateTo)) {
            "recruitmentDateFrom 은 recruitmentDateTo 보다 늦을 수 없습니다"
        }
    }

    fun eventDateRange(): DateRangeFilter = DateRangeFilter(eventDateFrom, eventDateTo)

    fun recruitmentDateRange(): DateRangeFilter = DateRangeFilter(recruitmentDateFrom, recruitmentDateTo)

    /**
     * Spring @Cacheable 캐시 키 생성
     * 패턴: {field}:{statusFilter}:{types}:{size}:{cursor}, 주최 필터가 있으면 {field}:h{hostId}:… (D-026)
     * 날짜 범위 필터는 있을 때만 붙인다 (e{행사일}:r{모집일}:). 필터 없는 요청의 키는 이전 버전과 같다
     */
    fun cacheKey(): String {
        val fieldStr = field.name.lowercase()
        val statusFilter = when {
            status != null -> "s:${status.name.lowercase()}"
            statusGroup != null -> "sg:${statusGroup!!.name.lowercase()}"
            else -> "sg:active"
        }
        val typesStr = types?.sortedBy { it.name }?.joinToString(",") { it.name.lowercase() } ?: "all"
        val cursorStr = cursor ?: "first"
        val hostStr = hostId?.let { "h$it:" } ?: ""
        val dateStr = listOf("e" to eventDateRange(), "r" to recruitmentDateRange())
            .filterNot { (_, range) -> range.isUnbounded }
            .joinToString("") { (prefix, range) -> "$prefix${range.cacheKey()}:" }
        return "$fieldStr:$hostStr$dateStr$statusFilter:$typesStr:$size:$cursorStr"
    }
}
