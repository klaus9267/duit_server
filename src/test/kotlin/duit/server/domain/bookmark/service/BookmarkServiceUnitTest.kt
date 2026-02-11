package duit.server.domain.bookmark.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.bookmark.entity.Bookmark
import duit.server.domain.bookmark.repository.BookmarkRepository
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.service.EventService
import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.User
import duit.server.domain.user.service.UserService
import io.mockk.*
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDateTime

@DisplayName("BookmarkService 단위 테스트")
class BookmarkServiceUnitTest {

    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var eventService: EventService
    private lateinit var userService: UserService
    private lateinit var securityUtil: SecurityUtil
    private lateinit var bookmarkService: BookmarkService

    private lateinit var host: Host
    private lateinit var user: User
    private lateinit var approvedEvent: Event

    @BeforeEach
    fun setUp() {
        bookmarkRepository = mockk()
        eventService = mockk()
        userService = mockk()
        securityUtil = mockk()
        bookmarkService = BookmarkService(bookmarkRepository, eventService, userService, securityUtil)

        host = Host(id = 1L, name = "테스트 주최")
        user = User(id = 1L, nickname = "테스트유저", providerId = "p1")
        approvedEvent = Event(
            id = 10L, title = "승인 행사", startAt = LocalDateTime.now().plusDays(7),
            endAt = null, recruitmentStartAt = null, recruitmentEndAt = null,
            uri = "https://example.com", thumbnail = null,
            eventType = EventType.CONFERENCE, host = host,
            status = EventStatus.ACTIVE, statusGroup = EventStatusGroup.ACTIVE
        )
    }

    @Nested
    @DisplayName("bookmarkEvent - 북마크 토글")
    inner class BookmarkEventTests {

        @Test
        @DisplayName("북마크가 없으면 새로 생성하고 isBookmarked=true를 반환한다")
        fun createsNewBookmark() {
            every { securityUtil.getCurrentUserId() } returns 1L
            every { bookmarkRepository.findByEventIdAndUserId(10L, 1L) } returns null
            every { eventService.getEvent(10L) } returns approvedEvent
            every { userService.findUserById(1L) } returns user
            every { bookmarkRepository.save(any<Bookmark>()) } answers { firstArg() }

            val result = bookmarkService.bookmarkEvent(10L)

            assertTrue(result.isBookmarked)
            assertEquals(10L, result.eventId)
        }

        @Test
        @DisplayName("기존 북마크가 있으면 삭제하고 isBookmarked=false를 반환한다")
        fun removesExistingBookmark() {
            val existingBookmark = Bookmark(id = 1L, user = user, event = approvedEvent)
            every { securityUtil.getCurrentUserId() } returns 1L
            every { bookmarkRepository.findByEventIdAndUserId(10L, 1L) } returns existingBookmark
            every { bookmarkRepository.delete(existingBookmark) } just runs

            val result = bookmarkService.bookmarkEvent(10L)

            assertFalse(result.isBookmarked)
            assertEquals(10L, result.eventId)
            verify(exactly = 1) { bookmarkRepository.delete(existingBookmark) }
        }

        @Test
        @DisplayName("미승인 행사를 북마크하면 AccessDeniedException이 발생한다")
        fun throwsOnUnapprovedEvent() {
            val unapprovedEvent = approvedEvent.copy(status = EventStatus.PENDING, statusGroup = EventStatusGroup.PENDING)
            every { securityUtil.getCurrentUserId() } returns 1L
            every { bookmarkRepository.findByEventIdAndUserId(10L, 1L) } returns null
            every { eventService.getEvent(10L) } returns unapprovedEvent

            assertThrows<AccessDeniedException> {
                bookmarkService.bookmarkEvent(10L)
            }
        }

        @Test
        @DisplayName("autoAddBookmarkToCalendar=true이면 isAddedToCalendar=true로 생성한다")
        fun setsCalendarFlagWhenAutoEnabled() {
            val calendarUser = User(id = 2L, nickname = "캘린더유저", providerId = "p2", autoAddBookmarkToCalendar = true)
            val bookmarkSlot = slot<Bookmark>()
            every { securityUtil.getCurrentUserId() } returns 2L
            every { bookmarkRepository.findByEventIdAndUserId(10L, 2L) } returns null
            every { eventService.getEvent(10L) } returns approvedEvent
            every { userService.findUserById(2L) } returns calendarUser
            every { bookmarkRepository.save(capture(bookmarkSlot)) } answers { bookmarkSlot.captured }

            bookmarkService.bookmarkEvent(10L)

            assertTrue(bookmarkSlot.captured.isAddedToCalendar)
        }

        @Test
        @DisplayName("autoAddBookmarkToCalendar=false이면 isAddedToCalendar=false로 생성한다")
        fun defaultCalendarFlagWhenAutoDisabled() {
            val bookmarkSlot = slot<Bookmark>()
            every { securityUtil.getCurrentUserId() } returns 1L
            every { bookmarkRepository.findByEventIdAndUserId(10L, 1L) } returns null
            every { eventService.getEvent(10L) } returns approvedEvent
            every { userService.findUserById(1L) } returns user
            every { bookmarkRepository.save(capture(bookmarkSlot)) } answers { bookmarkSlot.captured }

            bookmarkService.bookmarkEvent(10L)

            assertFalse(bookmarkSlot.captured.isAddedToCalendar)
        }

        @Test
        @DisplayName("존재하지 않는 행사를 북마크하면 EntityNotFoundException이 발생한다")
        fun throwsOnNonExistentEvent() {
            every { securityUtil.getCurrentUserId() } returns 1L
            every { bookmarkRepository.findByEventIdAndUserId(999L, 1L) } returns null
            every { eventService.getEvent(999L) } throws EntityNotFoundException("행사를 찾을 수 없습니다")

            assertThrows<EntityNotFoundException> {
                bookmarkService.bookmarkEvent(999L)
            }
        }
    }

    private fun Event.copy(status: EventStatus, statusGroup: EventStatusGroup): Event = Event(
        id = this.id, title = this.title, startAt = this.startAt, endAt = this.endAt,
        recruitmentStartAt = this.recruitmentStartAt, recruitmentEndAt = this.recruitmentEndAt,
        uri = this.uri, thumbnail = this.thumbnail,
        eventType = this.eventType, host = this.host,
        status = status, statusGroup = statusGroup
    )
}
