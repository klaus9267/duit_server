package duit.server.domain.event.dto

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.dto.pagination.PaginationParam
import duit.server.domain.event.entity.EventType
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Sort

data class EventPaginationParam(
    @field:Schema(description = "페이지 번호", defaultValue = "0")
    override val page: Int?,

    @field:Schema(description = "페이지 크기", defaultValue = "10")
    override val size: Int?,

    @field:Schema(description = "정렬 순서", defaultValue = "DESC")
    override val sortDirection: Sort.Direction?,

    @field:Schema(description = "정렬 필드", defaultValue = "ID")
    override val field: PaginationField?,

    @field:Schema(description = "행사 종류 null 입력 시 전체 행사 조회")
    val type: List<EventType>?,

    @field:Schema(description = "주최")
    val hostId: Long?,

    @field:Schema(description = "검색 키워드 (제목, 주최기관명 검색)")
    val searchKeyword: String?

) : PaginationParam(page, size, sortDirection, field) {
    fun toFilter(
        currentUserId: Long?,
        isApproved: Boolean = true,
        isBookmarked: Boolean = false,
        includeFinished: Boolean = false
    ): EventSearchFilter {
        val sortField = this.field?.displayName?.lowercase()
        val sortDirection = this.sortDirection?.name?.lowercase() ?: "asc"
        val eventTypesString = this.type?.joinToString(",") { it.name }

        return EventSearchFilter(
            eventTypes = eventTypesString,
            hostId = this.hostId,
            isApproved = isApproved,
            isBookmarked = isBookmarked,
            includeFinished = includeFinished,
            searchKeyword = this.searchKeyword,
            userId = currentUserId,
            sortField = sortField,
            sortDirection = sortDirection
        )
    }
}

