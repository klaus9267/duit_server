package duit.server.domain.bookmark.controller

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Bookmark API 통합 테스트")
class BookmarkControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var user: User
    private lateinit var host: Host
    private lateinit var approvedEvent: Event
    private lateinit var unapprovedEvent: Event

    @BeforeEach
    fun setUp() {
        user = TestFixtures.user(nickname = "북마크유저", providerId = "bookmark-provider")
        entityManager.persist(user)

        host = TestFixtures.host(name = "북마크테스트주최")
        entityManager.persist(host)

        approvedEvent = TestFixtures.event(
            title = "승인된 행사",
            host = host,
            status = EventStatus.ACTIVE,
            statusGroup = EventStatusGroup.ACTIVE
        )
        entityManager.persist(approvedEvent)

        unapprovedEvent = TestFixtures.event(
            title = "미승인 행사",
            host = host
        )
        entityManager.persist(unapprovedEvent)

        // 기존 북마크 데이터 (목록 조회 테스트용)
        val event2 = TestFixtures.event(
            title = "북마크된 행사",
            host = host,
            status = EventStatus.RECRUITING,
            statusGroup = EventStatusGroup.ACTIVE
        )
        entityManager.persist(event2)
        entityManager.persist(TestFixtures.bookmark(user = user, event = event2))

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("POST /api/v1/bookmarks/{eventId} - 북마크 토글")
    inner class BookmarkToggleTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("북마크를 추가한다")
            fun addBookmark() {
                mockMvc.perform(
                    post("/api/v1/bookmarks/{eventId}", approvedEvent.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.eventId").value(approvedEvent.id!!.toInt()))
                    .andExpect(jsonPath("$.isBookmarked").value(true))
            }

            @Test
            @DisplayName("북마크를 해제한다")
            fun removeBookmark() {
                // 먼저 북마크 추가
                mockMvc.perform(
                    post("/api/v1/bookmarks/{eventId}", approvedEvent.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.isBookmarked").value(true))

                // 다시 토글하여 해제
                mockMvc.perform(
                    post("/api/v1/bookmarks/{eventId}", approvedEvent.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.eventId").value(approvedEvent.id!!.toInt()))
                    .andExpect(jsonPath("$.isBookmarked").value(false))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("미승인 행사를 북마크하면 403을 반환한다")
            fun unapprovedEvent() {
                mockMvc.perform(
                    post("/api/v1/bookmarks/{eventId}", unapprovedEvent.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isForbidden)
            }

            @Test
            @DisplayName("존재하지 않는 eventId로 요청하면 404를 반환한다")
            fun notFoundEvent() {
                mockMvc.perform(
                    post("/api/v1/bookmarks/{eventId}", 999999)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isNotFound)
            }

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(post("/api/v1/bookmarks/{eventId}", approvedEvent.id!!))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/bookmarks - 북마크 목록 조회")
    inner class GetBookmarksTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("북마크 목록을 조회한다")
            fun getBookmarks() {
                mockMvc.perform(
                    get("/api/v1/bookmarks")
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(1))
            }

            @Test
            @DisplayName("페이지네이션으로 조회한다")
            fun getBookmarksWithPagination() {
                mockMvc.perform(
                    get("/api/v1/bookmarks")
                        .header("Authorization", authHeader(user.id!!))
                        .param("page", "0")
                        .param("size", "5")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.pageInfo.pageSize").value(5))
            }

            @Test
            @DisplayName("북마크가 없으면 빈 목록을 반환한다")
            fun emptyBookmarks() {
                val newUser = TestFixtures.user(nickname = "새유저", providerId = "new-provider")
                entityManager.persist(newUser)
                entityManager.flush()
                entityManager.clear()

                mockMvc.perform(
                    get("/api/v1/bookmarks")
                        .header("Authorization", authHeader(newUser.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(0))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(get("/api/v1/bookmarks"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }
}
