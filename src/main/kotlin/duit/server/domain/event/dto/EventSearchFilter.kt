package duit.server.domain.event.dto

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.event.entity.EventType
import org.springframework.data.domain.Sort

data class EventSearchFilter(
    val eventTypes: List<EventType>? = null,
    val hostId: Long? = null,
    val excludePending: Boolean = true,
    val isBookmarked: Boolean = false,
    val includeFinished: Boolean = false,
    val searchKeyword: String? = null,
    val userId: Long? = null,
    val sortField: PaginationField? = null,
    val sortDirection: Sort.Direction = Sort.Direction.ASC
) {
    fun eventTypesToString(): String? {
        return eventTypes?.joinToString(",") { it.name }
    }

    fun sortFieldName(): String? {
        return sortField?.displayName
    }

    fun sortDirectionName(): String {
        return sortDirection.name.lowercase()
    }
}