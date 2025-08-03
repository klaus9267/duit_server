package duit.server.application.controller

import duit.server.application.controller.dto.pagination.PaginationParam
import duit.server.domain.bookmark.service.BookmarkService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/bookmarks")
@Tag(name = "Bookmark", description = "북마크 관련 API")
class BookmarkController(
    private val bookmarkService: BookmarkService
) {
    @GetMapping
    @Operation(summary = "북마크 목록 조회")
    @ResponseStatus(HttpStatus.OK)
    fun getBookmarks(
        @Valid @ParameterObject
        param: PaginationParam
    ) = bookmarkService.getBookmarks(param)

    @PostMapping("{eventId}")
    @Operation(summary = "북마크 생성/취소")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun bookmarkEvent(
        @PathVariable eventId: Long
    ) = bookmarkService.bookmarkEvent(eventId)
}