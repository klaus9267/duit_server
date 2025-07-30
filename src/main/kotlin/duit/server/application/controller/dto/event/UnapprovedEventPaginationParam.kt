package duit.server.application.controller.dto.event

import duit.server.application.controller.dto.pagination.PaginationParam
import org.springframework.data.domain.Sort

data class UnapprovedEventPaginationParam(
    override val sortDirection: Sort.Direction = Sort.Direction.DESC,
) : PaginationParam(
    sortDirection = sortDirection
)
