package duit.server.application.controller.dto.pagination

data class PaginationResponse<T>(
    val content: List<T> = mutableListOf(),
    val pageInfo: PageInfo
)
