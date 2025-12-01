package duit.server.domain.common.dto.pagination

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

abstract class PaginationParam(
    open val page: Int?,
    open val size: Int?,
    open val sortDirection: Sort.Direction?,
    open val field: PaginationField?
) {
    open fun toPageable(): Pageable = PageRequest.of(
        page ?: 0,
        size ?: 10,
        sortDirection ?: Sort.Direction.DESC,
        (field ?: PaginationField.ID).displayName
    )

    open fun toPageableUnsorted(): Pageable = PageRequest.of(
        page ?: 0,
        size ?: 10
    )
}