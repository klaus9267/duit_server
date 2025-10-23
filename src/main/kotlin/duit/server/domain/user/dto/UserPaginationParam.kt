package duit.server.domain.user.dto

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.common.dto.pagination.PaginationParam
import org.springframework.data.domain.Sort

data class UserPaginationParam(
    override val page: Int? = 0,
    override val size: Int? = 10,
    override val sortDirection: Sort.Direction? = Sort.Direction.DESC,
    override val field: PaginationField? = PaginationField.ID
) : PaginationParam(page, size, sortDirection, field)
