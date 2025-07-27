package duit.server.application.controller.dto.pagination

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

open class PaginationParam(
    open val page: Int,
    open val size: Int,
    open val sortDirection: Sort.Direction = Sort.Direction.ASC,
) {
    open fun toPageable(): Pageable = PageRequest.of(
        page, size, sortDirection, PaginationField.NAME.displayName
    )
}