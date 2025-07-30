package duit.server.application.controller.dto.pagination

data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val pageInfo: PageInfo
)
