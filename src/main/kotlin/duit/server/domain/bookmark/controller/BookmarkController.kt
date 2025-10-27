package duit.server.domain.bookmark.controller

import duit.server.application.common.RequireAuth
import duit.server.domain.bookmark.dto.BookmarkPaginationParam
import duit.server.domain.bookmark.dto.BookmarkToggleResponse
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
    @Operation(summary = "북마크 목록 조회", description = "사용자가 북마크한 행사 목록을 조회합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun getBookmarks(
        @Valid @ParameterObject
        param: BookmarkPaginationParam
    ) = bookmarkService.getBookmarks(param)

    @PostMapping("{eventId}")
    @Operation(summary = "북마크 토글", description = "행사 북마크를 생성하거나 취소합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun bookmarkEvent(
        @PathVariable eventId: Long
    ): BookmarkToggleResponse = bookmarkService.bookmarkEvent(eventId)
}