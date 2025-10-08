package duit.server.domain.bookmark.controller

import duit.server.domain.bookmark.controller.docs.BookmarkEventApi
import duit.server.domain.bookmark.controller.docs.GetBookmarksApi
import duit.server.domain.bookmark.dto.BookmarkToggleResponse
import duit.server.domain.bookmark.service.BookmarkService
import duit.server.domain.common.dto.pagination.PaginationParam
import io.swagger.v3.oas.annotations.Hidden
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
    @GetBookmarksApi
    @ResponseStatus(HttpStatus.OK)
    fun getBookmarks(
        @Valid @ParameterObject
        param: PaginationParam
    ) = bookmarkService.getBookmarks(param)

    @PostMapping("{eventId}")
    @BookmarkEventApi
    @ResponseStatus(HttpStatus.OK)
    fun bookmarkEvent(
        @PathVariable eventId: Long
    ): BookmarkToggleResponse = bookmarkService.bookmarkEvent(eventId)
}