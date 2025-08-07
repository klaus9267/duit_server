package duit.server.domain.common.dto.pagination

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

open class PaginationParam(
    @field:Schema(description = "페이지 번호", example = "0", required = false)
    open val page: Int?,

    @field:Schema(description = "페이지 크기", example = "10", required = false)
    open val size: Int?,

    @field:Schema(description = "정렬 순서", required = false)
    open val sortDirection: Sort.Direction?,

    @field:Schema(description = "정렬 필드", required = false)
    open val field: PaginationField?
) {
    open fun toPageable(): Pageable = PageRequest.of(
        page ?: 0,
        size ?: 10,
        sortDirection ?: Sort.Direction.DESC,
        (field ?: PaginationField.ID).displayName
    )
}