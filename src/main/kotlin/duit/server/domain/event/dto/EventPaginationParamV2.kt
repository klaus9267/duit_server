package duit.server.domain.event.dto

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.dto.pagination.PaginationParam
import duit.server.domain.event.entity.EventStatus
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

    @Schema(description = "행사 종류 null 입력 시 전체 행사 조회")
    val types: List<EventType>? = null,

    @Schema(description = "행사 상태 (복수 선택 가능, 미입력 시 기본값 ACTIVE)", defaultValue = "[\"ACTIVE\"]")
    val statuses: List<EventStatus> = listOf(EventStatus.ACTIVE),

    @Schema(description = "북마크한 행사만 조회 (미입력 시 기본값 false)", defaultValue = "false", required = false)
    val bookmarked: Boolean = false,
) : PaginationParam(page, size, sortDirection, field)

