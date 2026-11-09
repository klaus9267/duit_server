package duit.server.domain.event.controller

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@DisplayName("Event API v1 통합 테스트")
class EventControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var user: User
    private lateinit var host: Host
    private lateinit var approvedEvent: Event
    private lateinit var pendingEvent: Event
    private lateinit var finishedEvent: Event

    @BeforeEach
    fun setUp() {
        user = TestFixtures.user(nickname = "이벤트유저", providerId = "event-provider")
        entityManager.persist(user)
        // 관리자 엔드포인트 테스트용: user 를 관리자로 등록
        entityManager.persist(TestFixtures.admin(user = user))

        host = TestFixtures.host(name = "이벤트테스트주최")
        entityManager.persist(host)

        approvedEvent = TestFixtures.event(
            title = "승인된 행사",
            host = host,
            status = EventStatus.ACTIVE,
            statusGroup = EventStatusGroup.ACTIVE,
            eventType = EventType.CONFERENCE
        )
        approvedEvent.view.count = 10
        entityManager.persist(approvedEvent)

        pendingEvent = TestFixtures.event(
            title = "승인대기 행사",
            host = host,
            status = EventStatus.PENDING,
            statusGroup = EventStatusGroup.PENDING,
            eventType = EventType.SEMINAR
        )
        entityManager.persist(pendingEvent)

        finishedEvent = TestFixtures.event(
            title = "종료된 행사",
            host = host,
            status = EventStatus.FINISHED,
            statusGroup = EventStatusGroup.FINISHED,
            eventType = EventType.WORKSHOP,
            startAt = LocalDateTime.now().minusDays(10),
            endAt = LocalDateTime.now().minusDays(9)
        )
        finishedEvent.view.count = 5
        entityManager.persist(finishedEvent)

        // 북마크 데이터
        entityManager.persist(TestFixtures.bookmark(user = user, event = approvedEvent))

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("GET /api/v1/events - 행사 목록 조회")
    @Disabled("V1 API uses MySQL native query (FIND_IN_SET, UNIX_TIMESTAMP) - incompatible with H2 test database")
    inner class GetEventsTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("기본 목록을 조회한다")
            fun getEventsDefault() {
                mockMvc.perform(get("/api/v1/events"))
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
            }

            @Test
            @DisplayName("종료된 행사를 포함하여 조회한다")
            fun getEventsIncludeFinished() {
                mockMvc.perform(
                    get("/api/v1/events")
                        .param("includeFinished", "true")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/events - 행사 생성 (사용자)")
    inner class CreateEventByUserTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("행사를 생성한다 (사용자 - PENDING)")
            fun createEvent() {
                val eventData = objectMapper.writeValueAsBytes(
                    mapOf(
                        "title" to "새 행사",
                        "startAt" to "2026-06-01T09:00:00",
                        "endAt" to "2026-06-01T18:00:00",
                        "uri" to "https://example.com/new-event",
                        "eventType" to "CONFERENCE",
                        "hostId" to host.id!!
                    )
                )
                val data = MockMultipartFile("data", "", "application/json", eventData)

                mockMvc.perform(
                    multipart("/api/v1/events")
                        .file(data)
                )
                    .andDo(print())
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.title").value("새 행사"))
                    .andExpect(jsonPath("$.eventStatus").value("PENDING"))
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/events/admin - 관리자 행사 생성")
    inner class CreateEventByAdminTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("관리자가 행사를 생성한다 (자동 승인)")
            fun createAdminEvent() {
                val eventData = objectMapper.writeValueAsBytes(
                    mapOf(
                        "title" to "관리자 행사",
                        "startAt" to "2026-07-01T09:00:00",
                        "endAt" to "2026-07-01T18:00:00",
                        "uri" to "https://example.com/admin-event",
                        "eventType" to "SEMINAR",
                        "hostId" to host.id!!
                    )
                )
                val data = MockMultipartFile("data", "", "application/json", eventData)

                mockMvc.perform(
                    multipart("/api/v1/events/admin")
                        .file(data)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.title").value("관리자 행사"))
                    .andExpect(jsonPath("$.eventStatus").value("RECRUITMENT_WAITING"))
            }

            @Test
            @DisplayName("여러 주최로 생성한다 - 순서 유지, host 는 첫 번째 주최, 신규 주최 로고는 thumbnailIndex 로 매핑")
            fun createAdminEventWithMultipleHosts() {
                val eventData = objectMapper.writeValueAsBytes(
                    mapOf(
                        "title" to "공동 주최 행사",
                        "startAt" to "2026-07-01T09:00:00",
                        "endAt" to "2026-07-01T18:00:00",
                        "uri" to "https://example.com/co-hosted",
                        "eventType" to "SEMINAR",
                        "hosts" to listOf(
                            mapOf("hostName" to "신규공동주최", "thumbnailIndex" to 0),
                            mapOf("hostId" to host.id!!),
                        )
                    )
                )
                val data = MockMultipartFile("data", "", "application/json", eventData)
                val logo = MockMultipartFile("hostThumbnails", "logo.png", "image/png", byteArrayOf(1, 2, 3))

                mockMvc.perform(
                    multipart("/api/v1/events/admin")
                        .file(data)
                        .file(logo)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.hosts.length()").value(2))
                    .andExpect(jsonPath("$.hosts[0].name").value("신규공동주최"))
                    .andExpect(jsonPath("$.hosts[0].thumbnail").isNotEmpty)
                    .andExpect(jsonPath("$.hosts[1].id").value(host.id!!.toInt()))
                    .andExpect(jsonPath("$.host.name").value("신규공동주최"))

                entityManager.flush()
                val savedHostNames = entityManager.createQuery(
                    "SELECT eh.host.name FROM EventHost eh WHERE eh.event.title = '공동 주최 행사' ORDER BY eh.sortOrder",
                    String::class.java
                ).resultList
                assertEquals(listOf("신규공동주최", host.name), savedHostNames)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                val eventData = objectMapper.writeValueAsBytes(
                    mapOf(
                        "title" to "행사",
                        "startAt" to "2026-07-01T09:00:00",
                        "uri" to "https://example.com",
                        "eventType" to "SEMINAR",
                        "hostId" to host.id!!
                    )
                )
                val data = MockMultipartFile("data", "", "application/json", eventData)

                mockMvc.perform(
                    multipart("/api/v1/events/admin")
                        .file(data)
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }

            @Test
            @DisplayName("hosts 와 hostId 를 함께 보내면 400을 반환한다")
            fun mixedHostInputs() {
                val eventData = objectMapper.writeValueAsBytes(
                    mapOf(
                        "title" to "행사",
                        "startAt" to "2026-07-01T09:00:00",
                        "uri" to "https://example.com",
                        "eventType" to "SEMINAR",
                        "hostId" to host.id!!,
                        "hosts" to listOf(mapOf("hostId" to host.id!!))
                    )
                )
                val data = MockMultipartFile("data", "", "application/json", eventData)

                mockMvc.perform(
                    multipart("/api/v1/events/admin")
                        .file(data)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("빈 hosts 를 보내면 400을 반환한다")
            fun emptyHosts() {
                val eventData = objectMapper.writeValueAsBytes(
                    mapOf(
                        "title" to "행사",
                        "startAt" to "2026-07-01T09:00:00",
                        "uri" to "https://example.com",
                        "eventType" to "SEMINAR",
                        "hosts" to emptyList<Any>()
                    )
                )
                val data = MockMultipartFile("data", "", "application/json", eventData)

                mockMvc.perform(
                    multipart("/api/v1/events/admin")
                        .file(data)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/events/calendar - 캘린더용 행사 조회")
    inner class GetEventsForCalendarTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("월별 캘린더 행사를 조회한다")
            fun getCalendarEvents() {
                val now = LocalDateTime.now()

                mockMvc.perform(
                    get("/api/v1/events/calendar")
                        .header("Authorization", authHeader(user.id!!))
                        .param("year", now.year.toString())
                        .param("month", now.monthValue.toString())
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$").isArray)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(
                    get("/api/v1/events/calendar")
                        .param("year", "2026")
                        .param("month", "6")
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/events/{eventId}/approve - 행사 승인")
    inner class ApproveEventTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("행사를 승인한다")
            fun approveEvent() {
                mockMvc.perform(
                    patch("/api/v1/events/{eventId}/approve", pendingEvent.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isNoContent)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(
                    patch("/api/v1/events/{eventId}/approve", pendingEvent.id!!)
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/events/{eventId} - 행사 수정")
    inner class UpdateEventTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("행사를 수정한다")
            fun updateEvent() {
                val updateData = objectMapper.writeValueAsBytes(
                    mapOf(
                        "title" to "수정된 행사",
                        "startAt" to "2026-08-01T09:00:00",
                        "endAt" to "2026-08-01T18:00:00",
                        "uri" to "https://example.com/updated",
                        "eventType" to "CONFERENCE",
                        "hostId" to host.id!!
                    )
                )
                val data = MockMultipartFile("data", "", "application/json", updateData)

                mockMvc.perform(
                    multipart("/api/v1/events/{eventId}", approvedEvent.id!!)
                        .file(data)
                        .with { it.method = "PUT"; it }
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.title").value("수정된 행사"))
            }

            @Test
            @DisplayName("hosts 로 주최를 교체하고, 다시 순서를 바꿔도 event_hosts 가 요청 순서대로 저장된다")
            fun updateEventHostsReplaceAndReorder() {
                val otherHost = TestFixtures.host(name = "다른주최")
                entityManager.persist(otherHost)
                entityManager.flush()
                entityManager.clear()

                fun putHosts(vararg hostIds: Long) = mockMvc.perform(
                    multipart("/api/v1/events/{eventId}", approvedEvent.id!!)
                        .file(
                            MockMultipartFile(
                                "data", "", "application/json",
                                objectMapper.writeValueAsBytes(
                                    mapOf(
                                        "title" to "공동 주최로 수정",
                                        "startAt" to "2026-08-01T09:00:00",
                                        "uri" to "https://example.com/updated",
                                        "eventType" to "CONFERENCE",
                                        "hosts" to hostIds.map { mapOf("hostId" to it) }
                                    )
                                )
                            )
                        )
                        .with { it.method = "PUT"; it }
                        .header("Authorization", authHeader(user.id!!))
                ).andDo(print()).andExpect(status().isOk).also {
                    entityManager.flush()
                    entityManager.clear()
                }

                putHosts(host.id!!, otherHost.id!!)
                    .andExpect(jsonPath("$.hosts[*].id").value(contains(host.id!!.toInt(), otherHost.id!!.toInt())))
                putHosts(otherHost.id!!, host.id!!)
                    .andExpect(jsonPath("$.hosts[*].id").value(contains(otherHost.id!!.toInt(), host.id!!.toInt())))
                    .andExpect(jsonPath("$.host.id").value(otherHost.id!!.toInt()))

                val storedOrder = entityManager.createQuery(
                    "SELECT eh.host.id FROM EventHost eh WHERE eh.event.id = :eventId ORDER BY eh.sortOrder",
                    Long::class.javaObjectType
                ).setParameter("eventId", approvedEvent.id!!).resultList
                assertEquals(listOf(otherHost.id!!, host.id!!), storedOrder)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("존재하지 않는 eventId로 요청하면 404를 반환한다")
            fun notFoundEvent() {
                val updateData = objectMapper.writeValueAsBytes(
                    mapOf(
                        "title" to "수정",
                        "startAt" to "2026-08-01T09:00:00",
                        "uri" to "https://example.com",
                        "eventType" to "CONFERENCE",
                        "hostId" to host.id!!
                    )
                )
                val data = MockMultipartFile("data", "", "application/json", updateData)

                mockMvc.perform(
                    multipart("/api/v1/events/{eventId}", 999999)
                        .file(data)
                        .with { it.method = "PUT"; it }
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isNotFound)
            }

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                val updateData = objectMapper.writeValueAsBytes(
                    mapOf(
                        "title" to "수정",
                        "startAt" to "2026-08-01T09:00:00",
                        "uri" to "https://example.com",
                        "eventType" to "CONFERENCE",
                        "hostId" to host.id!!
                    )
                )
                val data = MockMultipartFile("data", "", "application/json", updateData)

                mockMvc.perform(
                    multipart("/api/v1/events/{eventId}", approvedEvent.id!!)
                        .file(data)
                        .with { it.method = "PUT"; it }
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/events/batch - 행사 일괄 삭제")
    inner class BatchDeleteEventsTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("행사를 일괄 삭제한다")
            fun batchDeleteEvents() {
                mockMvc.perform(
                    delete("/api/v1/events/batch")
                        .header("Authorization", authHeader(user.id!!))
                        .param("eventIds", approvedEvent.id!!.toString(), pendingEvent.id!!.toString())
                )
                    .andDo(print())
                    .andExpect(status().isOk)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(
                    delete("/api/v1/events/batch")
                        .param("eventIds", approvedEvent.id!!.toString())
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }
}
