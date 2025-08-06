package duit.server.domain.bookmark.service

import duit.server.application.controller.dto.bookmarks.BookmarkResponse
import duit.server.application.controller.dto.pagination.PageInfo
import duit.server.application.controller.dto.pagination.PageResponse
import duit.server.application.controller.dto.pagination.PaginationParam
import duit.server.application.security.SecurityUtil
import duit.server.domain.bookmark.entity.Bookmark
import duit.server.domain.bookmark.repository.BookmarkRepository
import duit.server.domain.event.service.EventService
import duit.server.domain.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val eventService: EventService,
    private val userService: UserService,
    private val securityUtil: SecurityUtil
) {
    @Transactional
    fun bookmarkEvent(eventId: Long) {
        val currentUserId = securityUtil.getCurrentUserId()
        val bookmark = bookmarkRepository.findByEventIdAndUserId(eventId, currentUserId)
        if (bookmark != null) {
            bookmarkRepository.delete(bookmark)
            return
        }

        val event = eventService.getEvent(eventId)
        val currentUser = userService.findUserById(currentUserId)

        val newBookmark = Bookmark(
            event = event,
            user = currentUser
        )
        bookmarkRepository.save(newBookmark)
    }

    fun getBookmarks(param: PaginationParam): PageResponse<BookmarkResponse> {
        val bookmarks = bookmarkRepository.findAll(param.toPageable())
            .map { BookmarkResponse.from(it) }
        return PageResponse<BookmarkResponse>(
            bookmarks.content,
            pageInfo = PageInfo.from(bookmarks)
        )
    }
}