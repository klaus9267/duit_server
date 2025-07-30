package duit.server.application.controller.dto.pagination

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

open class PaginationParam(
    open val page: Int = 0,
    open val size: Int = 10,
    open val sortDirection: Sort.Direction = Sort.Direction.ASC,
    open val field: PaginationField = PaginationField.ID
) {
    open fun toPageable(): Pageable = PageRequest.of(
        page, size, sortDirection, field.displayName
    )
}