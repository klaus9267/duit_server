package duit.server.domain.common.dto.pagination

data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val pageInfo: PageInfo
)
