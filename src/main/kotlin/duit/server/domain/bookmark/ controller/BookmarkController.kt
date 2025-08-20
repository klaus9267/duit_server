package duit.server.domain.bookmark.controller

import duit.server.domain.bookmark.` controller`.docs.BookmarkEventApi
import duit.server.domain.bookmark.` controller`.docs.GetBookmarksApi
import duit.server.application.docs.common.AuthApiResponses
import duit.server.application.docs.common.CommonApiResponses
import duit.server.domain.common.dto.pagination.PaginationParam
import duit.server.domain.bookmark.service.BookmarkService
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/bookmarks")
@Tag(name = "Bookmark", description = "북마크 관련 API")
class BookmarkController(
    private val bookmarkService: BookmarkService
) {
    @GetMapping
    @GetBookmarksApi
    @AuthApiResponses
    @CommonApiResponses
    @ResponseStatus(HttpStatus.OK)
    fun getBookmarks(
        @Valid @ParameterObject
        param: PaginationParam
    ) = bookmarkService.getBookmarks(param)

    @PostMapping("{eventId}")
    @BookmarkEventApi
    @AuthApiResponses
    @CommonApiResponses
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun bookmarkEvent(
        @PathVariable eventId: Long
    ) = bookmarkService.bookmarkEvent(eventId)
}