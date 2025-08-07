package duit.server.domain.host.dto

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.dto.pagination.PaginationParam
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Sort

data class HostPaginationParam(
    @field:Schema(description = "페이지 번호", example = "0", required = false)
    override val page: Int?,

    @field:Schema(description = "페이지 크기", example = "10", required = false)
    override val size: Int?,

    @field:Schema(description = "정렬 순서", example = "ASC", required = false)
    override val sortDirection: Sort.Direction?,

    @field:Schema(description = "정렬 필드", example = PaginationField.CONST_NAME, required = false)
    override val field: PaginationField? = PaginationField.NAME
) : PaginationParam(page, size, sortDirection, field ?: PaginationField.NAME)
