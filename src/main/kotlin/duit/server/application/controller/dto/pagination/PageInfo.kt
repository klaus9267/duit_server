package duit.server.application.controller.dto.pagination

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

data class PageInfo(
    @Schema(description = "현재 페이지 번호", example = "1")
    val pageNumber: Int,

    @Schema(description = "페이지 당 아이템 수", example = "1")
    val pageSize: Int,

    @Schema(description = "전체 페이지 수", example = "1")
    val totalPages: Int,

    @Schema(description = "전체 아이템 수", example = "1")
    val totalElements: Long
) {
    companion object {
        fun from(page: Page<*>): PageInfo {
            val pageable = page.pageable

            return PageInfo(
                pageNumber = pageable.pageNumber,
                pageSize = page.numberOfElements,
                totalPages = page.totalPages,
                totalElements = page.totalElements
            )
        }
    }
}