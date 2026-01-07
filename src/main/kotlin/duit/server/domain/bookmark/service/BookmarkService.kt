package duit.server.domain.bookmark.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.bookmark.dto.BookmarkPaginationParam
import duit.server.domain.bookmark.dto.BookmarkResponse
import duit.server.domain.bookmark.dto.BookmarkToggleResponse
import duit.server.domain.bookmark.entity.Bookmark
import duit.server.domain.bookmark.repository.BookmarkRepository
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.event.service.EventService
import duit.server.domain.user.service.UserService
import org.springframework.security.access.AccessDeniedException
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
    fun bookmarkEvent(eventId: Long): BookmarkToggleResponse {
        val currentUserId = securityUtil.getCurrentUserId()
        val bookmark = bookmarkRepository.findByEventIdAndUserId(eventId, currentUserId)

        val isBookmarked = if (bookmark != null) {
            bookmarkRepository.delete(bookmark)
            false
        } else {
            val event = eventService.getEvent(eventId)
            if (!event.isApproved) {
                throw AccessDeniedException("승인되지 않은 행사입니다.")
            }

            val currentUser = userService.findUserById(currentUserId)

            val newBookmark = Bookmark(
                event = event,
                user = currentUser
            )

            if (currentUser.autoAddBookmarkToCalendar) {
                newBookmark.isAddedToCalendar = true
            }

            bookmarkRepository.save(newBookmark)
            true
        }

        return BookmarkToggleResponse(eventId, isBookmarked)
    }

    fun getBookmarks(param: BookmarkPaginationParam): PageResponse<BookmarkResponse> {
        val currentUserId = securityUtil.getCurrentUserId()
        val bookmarks = bookmarkRepository.findByUserId(currentUserId, param.toPageable())
            .map { BookmarkResponse.from(it) }

        return PageResponse(
            bookmarks.content,
            pageInfo = PageInfo.from(bookmarks)
        )
    }
}