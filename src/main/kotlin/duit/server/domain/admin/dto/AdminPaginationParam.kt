package duit.server.domain.admin.dto

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.dto.pagination.PaginationParam
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Sort

data class AdminPaginationParam(
    @field:Schema(description = "페이지 번호", example = "0", required = false)
    override val page: Int? = 0,

    @field:Schema(description = "페이지 크기 (1~100)", example = "10", required = false)
    override val size: Int? = 10,

    @field:Schema(description = "정렬 순서 (등록 순)", example = "DESC", required = false)
    override val sortDirection: Sort.Direction? = Sort.Direction.DESC
) : PaginationParam(page, size, sortDirection, PaginationField.ID) {

    @get:Parameter(hidden = true)
    override val field: PaginationField = PaginationField.ID
}
