package duit.server.domain.event.dto

data class EventSearchFilter(
    val eventTypes: String? = null,
    val hostId: Long? = null,
    val isApproved: Boolean = true,
    val isBookmarked: Boolean = false,
    val includeFinished: Boolean = false,
    val searchKeyword: String? = null,
    val userId: Long? = null,
    val sortField: String? = null,
    val sortDirection: String? = "asc"
)