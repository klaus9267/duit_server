package duit.server.domain.bookmark.dto

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.dto.pagination.PaginationParam
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Sort

data class BookmarkPaginationParam(
    @field:Schema(description = "페이지 번호", example = "0", required = false)
    override val page: Int = 0,

    @field:Schema(description = "페이지 크기", example = "10", required = false)
    override val size: Int = 10,

    @field:Schema(hidden = true)
    override val sortDirection: Sort.Direction = Sort.Direction.DESC,

    @field:Schema(hidden = true)
    override val field: PaginationField? = PaginationField.ID
) : PaginationParam(page, size, sortDirection, field)