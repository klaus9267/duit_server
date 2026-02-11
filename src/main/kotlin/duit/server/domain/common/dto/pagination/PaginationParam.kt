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
    open fun toPageable(): Pageable {
        val safePage = (page ?: 0).coerceAtLeast(0)
        val safeSize = (size ?: 10).coerceIn(1, 100)
        return PageRequest.of(
            safePage,
            safeSize,
            sortDirection ?: Sort.Direction.DESC,
            (field ?: PaginationField.ID).displayName
        )
    }

    open fun toPageableUnsorted(): Pageable {
        val safePage = (page ?: 0).coerceAtLeast(0)
        val safeSize = (size ?: 10).coerceIn(1, 100)
        return PageRequest.of(safePage, safeSize)
    }
}