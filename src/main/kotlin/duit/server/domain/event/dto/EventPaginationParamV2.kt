package duit.server.domain.event.dto

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.dto.pagination.PaginationParam
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Sort

data class EventPaginationParamV2(
    @get:Schema(description = "페이지 번호", defaultValue = "0", required = false)
    override val page: Int = 0,

    @get:Schema(description = "페이지 크기", defaultValue = "10", required = false)
    override val size: Int = 10,

    @get:Schema(description = "정렬 필드", defaultValue = "CREATED_AT")
    override val field: PaginationField = PaginationField.CREATED_AT,

    @get:Schema(hidden = true)
    override val sortDirection: Sort.Direction? = null,

    @get:Schema(description = "행사 종류 null 입력 시 전체 행사 조회")
    val types: List<EventType>? = null,

    @get:Schema(description = "행사 상태 (statusGroup과 둘 중 하나만 사용)")
    val status: EventStatus? = null,

    @get:Schema(description = "행사 상태 그룹 (status와 둘 중 하나만 사용)", defaultValue = "ACTIVE")
    val statusGroup: EventStatusGroup? = null,

    @get:Schema(description = "북마크한 행사만 조회 (미입력 시 기본값 false)", defaultValue = "false", required = false)
    val bookmarked: Boolean = false,
) : PaginationParam(page, size, sortDirection, field) {
    init {
        require(status == null || statusGroup == null) {
            "status와 statusGroup 중 하나만 사용 가능합니다"
        }
    }
}

