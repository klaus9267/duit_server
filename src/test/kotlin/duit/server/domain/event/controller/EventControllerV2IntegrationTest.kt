package duit.server.domain.event.controller

import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import duit.server.support.util.assertDateAscendingOrder
import duit.server.support.util.assertDateDescendingOrder
import duit.server.support.util.assertDescendingOrder
import duit.server.support.util.extractCursorFromResponse
import duit.server.support.util.extractHasNextFromResponse
import duit.server.support.util.extractValuesFromResponse
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@DisplayName("Event API v2 통합 테스트")
class EventControllerV2IntegrationTest : IntegrationTestSupport() {

    // 테스트 데이터
    private lateinit var host1: Host
    private lateinit var host2: Host
    private lateinit var user1: User

    @BeforeEach
    fun setUp() {
        setupCompleteTestData()
    }

    private fun setupCompleteTestData() {
        val now = LocalDateTime.now()

        // Host 생성
        host1 = TestFixtures.host(name = "테크 컨퍼런스")
        host2 = TestFixtures.host(name = "교육 센터")
        entityManager.persist(host1)
        entityManager.persist(host2)

        // User 생성 (북마크 테스트용)
        user1 = TestFixtures.user(
            providerId = "test-user-1",
            providerType = ProviderType.GOOGLE,
            nickname = "테스트유저1",
        )
        entityManager.persist(user1)

        // 모든 EventType에 대한 이벤트 생성 (10개)
        val eventTypes = listOf(
            EventType.CONFERENCE to "개발자 컨퍼런스",
            EventType.SEMINAR to "기술 세미나",
            EventType.WEBINAR to "온라인 웨비나",
            EventType.WORKSHOP to "실무 워크샵",
            EventType.CONTEST to "프로그래밍 공모전",
            EventType.CONTINUING_EDUCATION to "보수교육 과정",
            EventType.EDUCATION to "기초 교육",
            EventType.VOLUNTEER to "IT 봉사활동",
            EventType.TRAINING to "집중 연수",
            EventType.ETC to "기타 행사"
        )

        val eventStatuses = listOf(
            EventStatus.PENDING,
            EventStatus.RECRUITMENT_WAITING,
            EventStatus.RECRUITING,
            EventStatus.EVENT_WAITING,
            EventStatus.ACTIVE,
            EventStatus.FINISHED
        )

        val events = mutableListOf<duit.server.domain.event.entity.Event>()

        // EventType별로 이벤트 생성 (각 타입마다 다양한 상태)
        eventTypes.forEachIndexed { typeIndex, (eventType, title) ->
            val status = eventStatuses[typeIndex % eventStatuses.size]
            val statusGroup = when (status) {
                EventStatus.PENDING -> EventStatusGroup.PENDING
                EventStatus.FINISHED -> EventStatusGroup.FINISHED
                else -> EventStatusGroup.ACTIVE
            }

            val event = TestFixtures.event(
                title = title,
                startAt = now.plusDays((typeIndex + 1).toLong()),
                endAt = now.plusDays((typeIndex + 1).toLong()).plusHours(4),
                recruitmentStartAt = now.minusDays(10),
                recruitmentEndAt = now.plusDays(typeIndex.toLong()),
                uri = "https://example.com/event${typeIndex + 1}",
                isApproved = true,
                host = if (typeIndex % 2 == 0) host1 else host2,
                eventType = eventType,
                status = status,
                statusGroup = statusGroup,
            )
            events.add(event)
            entityManager.persist(event)
        }

        // EventStatus별로 추가 이벤트 생성 (상태 테스트를 위해)
        eventStatuses.forEachIndexed { statusIndex, status ->
            val statusGroup = when (status) {
                EventStatus.PENDING -> EventStatusGroup.PENDING
                EventStatus.FINISHED -> EventStatusGroup.FINISHED
                else -> EventStatusGroup.ACTIVE
            }

            val event = TestFixtures.event(
                title = "${status.description} 상태 이벤트",
                startAt = now.plusDays((statusIndex + 20).toLong()),
                endAt = now.plusDays((statusIndex + 20).toLong()).plusHours(3),
                recruitmentStartAt = now.minusDays(5),
                recruitmentEndAt = now.plusDays((statusIndex + 15).toLong()),
                uri = "https://example.com/status${statusIndex + 1}",
                isApproved = true,
                host = host1,
                eventType = EventType.SEMINAR,
                status = status,
                statusGroup = statusGroup,
            )
            events.add(event)
            entityManager.persist(event)
        }

        // View 생성 (조회수, 정렬 테스트용)
        events.forEachIndexed { index, event ->
            val view = TestFixtures.view(event = event, count = (100 + index * 50))
            entityManager.persist(view)
        }

        // Bookmark 생성 (일부 이벤트만)
        events.take(3).forEach { event ->
            entityManager.persist(TestFixtures.bookmark(user = user1, event = event))
        }

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("GET /api/v2/events - 행사 목록 조회")
    inner class GetEventsTests {

        @Nested
        @DisplayName("성공 케이스")
        inner class SuccessTests {

            @Nested
            @DisplayName("정렬 기능 (6개 PaginationField)")
            inner class SortingTests {

                @Test
                @DisplayName("ID 정렬 테스트")
                fun sortByIdTest() {
                    val result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("field", "ID")
                            .param("size", "5")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(0)))
                        .andReturn()

                    // ID가 내림차순으로 정렬되었는지 확인
                    val ids = extractValuesFromResponse(result, "id")
                    if (ids.isNotEmpty()) {
                        assertDescendingOrder(ids, "ID")
                    }
                }

                @Test
                @DisplayName("CREATED_AT 정렬 테스트 (기본값)")
                fun sortByCreatedAtTest() {
                    val result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("field", "CREATED_AT")
                            .param("size", "5")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(0)))
                        .andReturn()

                    // CREATED_AT이 최신순(내림차순)으로 정렬되었는지 확인
                    val createdDates = extractValuesFromResponse(result, "createdAt")
                    if (createdDates.isNotEmpty()) {
                        assertDateDescendingOrder(createdDates, "CREATED_AT")
                    }
                }

                @Test
                @DisplayName("START_DATE 정렬 테스트 - ACTIVE 상태")
                fun sortByStartDateActiveTest() {
                    val result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("field", "START_DATE")
                            .param("statusGroup", "ACTIVE")
                            .param("size", "5")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andReturn()

                    // ACTIVE 상태에서는 START_DATE가 오름차순으로 정렬되어야 함 (임박한 순서)
                    val startDates = extractValuesFromResponse(result, "startAt")
                    if (startDates.isNotEmpty()) {
                        assertDateAscendingOrder(startDates, "START_DATE (ACTIVE)")
                    }
                }

                @Test
                @DisplayName("START_DATE 정렬 테스트 - FINISHED 상태")
                fun sortByStartDateFinishedTest() {
                    val result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("field", "START_DATE")
                            .param("statusGroup", "FINISHED")
                            .param("size", "5")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andReturn()

                    // FINISHED 상태에서는 START_DATE가 내림차순으로 정렬되어야 함 (최근 종료 순서)
                    val startDates = extractValuesFromResponse(result, "startAt")
                    if (startDates.isNotEmpty()) {
                        assertDateDescendingOrder(startDates, "START_DATE (FINISHED)")
                    }
                }

                @Test
                @DisplayName("RECRUITMENT_DEADLINE 정렬 테스트")
                fun sortByRecruitmentDeadlineTest() {
                    val result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("field", "RECRUITMENT_DEADLINE")
                            .param("size", "5")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andReturn()

                    // RECRUITMENT_DEADLINE이 임박한 순서대로 정렬되었는지 확인
                    val recruitmentDeadlines = extractValuesFromResponse(result, "recruitmentEndAt")
                    if (recruitmentDeadlines.isNotEmpty()) {
                        assertDateAscendingOrder(recruitmentDeadlines, "RECRUITMENT_DEADLINE")
                    }
                }

                @Test
                @DisplayName("VIEW_COUNT 정렬 테스트 - H2 환경에서는 스킵")
                fun sortByViewCountSkipH2Test() {
                    // H2 환경에서는 MySQL Native SQL이 호환되지 않아 스킵
                    // 실제 MySQL 환경에서는 다음과 같이 테스트:
                    /*
                    val result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("field", "VIEW_COUNT")
                            .param("size", "5")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andReturn()

                    // VIEW_COUNT가 높은 순서대로 정렬되었는지 확인
                    val viewCounts = extractValuesFromResponse(result, "viewCount")
                    if (viewCounts.isNotEmpty()) {
                        assertDescendingOrder(viewCounts, "VIEW_COUNT")
                    }
                    */
                }
            }

            @Nested
            @DisplayName("EventType 필터링 (10개)")
            inner class EventTypeTests {

                @Test
                @DisplayName("CONFERENCE 필터링")
                fun filterByConferenceTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "CONFERENCE")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventType").value(everyItem(equalTo("CONFERENCE"))))
                }

                @Test
                @DisplayName("SEMINAR 필터링")
                fun filterBySeminarTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "SEMINAR")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventType").value(everyItem(equalTo("SEMINAR"))))
                }

                @Test
                @DisplayName("WEBINAR 필터링")
                fun filterByWebinarTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "WEBINAR")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventType").value(everyItem(equalTo("WEBINAR"))))
                }

                @Test
                @DisplayName("WORKSHOP 필터링")
                fun filterByWorkshopTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "WORKSHOP")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventType").value(everyItem(equalTo("WORKSHOP"))))
                }

                @Test
                @DisplayName("CONTEST 필터링")
                fun filterByContestTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "CONTEST")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventType").value(everyItem(equalTo("CONTEST"))))
                }

                @Test
                @DisplayName("CONTINUING_EDUCATION 필터링")
                fun filterByContinuingEducationTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "CONTINUING_EDUCATION")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventType").value(everyItem(equalTo("CONTINUING_EDUCATION"))))
                }

                @Test
                @DisplayName("EDUCATION 필터링")
                fun filterByEducationTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "EDUCATION")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventType").value(everyItem(equalTo("EDUCATION"))))
                }

                @Test
                @DisplayName("VOLUNTEER 필터링")
                fun filterByVolunteerTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "VOLUNTEER")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventType").value(everyItem(equalTo("VOLUNTEER"))))
                }

                @Test
                @DisplayName("TRAINING 필터링")
                fun filterByTrainingTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "TRAINING")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventType").value(everyItem(equalTo("TRAINING"))))
                }

                @Test
                @DisplayName("ETC 필터링")
                fun filterByEtcTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "ETC")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventType").value(everyItem(equalTo("ETC"))))
                }

                @Test
                @DisplayName("다중 EventType 조합 필터링")
                fun filterByMultipleTypesTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("types", "CONFERENCE,WORKSHOP,SEMINAR")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(
                            jsonPath("$.content[*].eventType").value(
                                everyItem(
                                    anyOf(
                                        equalTo("CONFERENCE"),
                                        equalTo("WORKSHOP"),
                                        equalTo("SEMINAR")
                                    )
                                )
                            )
                        )
                }
            }

            @Nested
            @DisplayName("EventStatus 필터링 (6개)")
            inner class EventStatusTests {

                @Test
                @DisplayName("PENDING 상태 필터링")
                fun filterByPendingStatusTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("status", "PENDING")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventStatus").value(everyItem(equalTo("PENDING"))))
                }

                @Test
                @DisplayName("RECRUITMENT_WAITING 상태 필터링")
                fun filterByRecruitmentWaitingStatusTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("status", "RECRUITMENT_WAITING")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventStatus").value(everyItem(equalTo("RECRUITMENT_WAITING"))))
                }

                @Test
                @DisplayName("RECRUITING 상태 필터링")
                fun filterByRecruitingStatusTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("status", "RECRUITING")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventStatus").value(everyItem(equalTo("RECRUITING"))))
                }

                @Test
                @DisplayName("EVENT_WAITING 상태 필터링")
                fun filterByEventWaitingStatusTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("status", "EVENT_WAITING")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventStatus").value(everyItem(equalTo("EVENT_WAITING"))))
                }

                @Test
                @DisplayName("ACTIVE 상태 필터링")
                fun filterByActiveStatusTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("status", "ACTIVE")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventStatus").value(everyItem(equalTo("ACTIVE"))))
                }

                @Test
                @DisplayName("FINISHED 상태 필터링")
                fun filterByFinishedStatusTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("status", "FINISHED")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventStatus").value(everyItem(equalTo("FINISHED"))))
                }
            }

            @Nested
            @DisplayName("EventStatusGroup 필터링 (3개)")
            inner class EventStatusGroupTests {

                @Test
                @DisplayName("PENDING 그룹 필터링")
                fun filterByPendingStatusGroupTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("statusGroup", "PENDING")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventStatusGroup").value(everyItem(equalTo("PENDING"))))
                }

                @Test
                @DisplayName("ACTIVE 그룹 필터링")
                fun filterByActiveStatusGroupTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("statusGroup", "ACTIVE")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventStatusGroup").value(everyItem(equalTo("ACTIVE"))))
                }

                @Test
                @DisplayName("FINISHED 그룹 필터링")
                fun filterByFinishedStatusGroupTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("statusGroup", "FINISHED")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].eventStatusGroup").value(everyItem(equalTo("FINISHED"))))
                }
            }

            @Nested
            @DisplayName("기타 필터링")
            inner class OtherFilterTests {

                @Test
                @DisplayName("searchKeyword 필터링")
                fun filterBySearchKeywordTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("searchKeyword", "개발")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].title").value(everyItem(containsString("개발"))))
                }

                @Test
                @DisplayName("hostId 필터링")
                fun filterByHostIdTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("hostId", host1.id.toString())
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content[*].host.id").value(everyItem(equalTo(host1.id?.toInt()))))
                }
            }

            @Nested
            @DisplayName("커서 페이지네이션")
            inner class CursorPaginationTests {

                @Test
                @DisplayName("첫 페이지 조회")
                fun cursorPaginationFirstPageTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("size", "3")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content.length()").value(lessThanOrEqualTo(3)))
                        .andExpect(jsonPath("$.pageInfo.hasNext").exists())
                        .andExpect(jsonPath("$.pageInfo.pageSize").value(3))
                }

                @Test
                @DisplayName("1, 2, 3번째 페이지 순차 조회")
                fun cursorPaginationMultiplePagesTest() {
                    // ACTIVE 그룹 11개: EventType별 7개 + EventStatus별 4개
                    // Page 1 (5개)
                    val page1Result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("size", "5")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content.length()").value(5))
                        .andExpect(jsonPath("$.pageInfo.hasNext").value(true))
                        .andExpect(jsonPath("$.pageInfo.nextCursor").exists())
                        .andReturn()

                    val cursor1 = extractCursorFromResponse(page1Result)
                    Assertions.assertNotNull(cursor1, "첫 페이지의 nextCursor가 null입니다")
                    val page1Ids = extractValuesFromResponse(page1Result, "id")

                    // Page 2 (5개)
                    val page2Result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("size", "5")
                            .param("cursor", cursor1!!)
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content.length()").value(5))
                        .andExpect(jsonPath("$.pageInfo.hasNext").value(true))
                        .andExpect(jsonPath("$.pageInfo.nextCursor").exists())
                        .andReturn()

                    val cursor2 = extractCursorFromResponse(page2Result)
                    val page2Ids = extractValuesFromResponse(page2Result, "id")

                    // Page 3 (마지막 페이지, 1개)
                    val page3Result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("size", "5")
                            .param("cursor", cursor2!!)
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.pageInfo.hasNext").value(false))
                        .andReturn()

                    val page3Ids = extractValuesFromResponse(page3Result, "id")

                    // 검증: 총 11개 조회, 중복 없음
                    val allIds = page1Ids + page2Ids + page3Ids
                    Assertions.assertEquals(11, allIds.size, "3페이지에서 총 11개의 이벤트를 조회해야 합니다")

                    val uniqueIds = allIds.toSet()
                    Assertions.assertEquals(
                        allIds.size,
                        uniqueIds.size,
                        "페이지 간 중복된 eventId가 존재합니다"
                    )
                }

                @Test
                @DisplayName("페이지 간 중복 없음 검증")
                fun cursorPaginationNoDuplicatesTest() {
                    val allEventIds = mutableListOf<Any>()
                    var currentCursor: String? = null
                    var hasNext = true

                    // hasNext가 false가 될 때까지 반복 조회
                    while (hasNext) {
                        val requestBuilder = get("/api/v2/events").param("size", "5")
                        if (currentCursor != null) {
                            requestBuilder.param("cursor", currentCursor!!)
                        }

                        val result = mockMvc.perform(requestBuilder)
                            .andDo(print())
                            .andExpect(status().isOk)
                            .andExpect(jsonPath("$.content").isArray)
                            .andReturn()

                        val pageIds = extractValuesFromResponse(result, "id")
                        allEventIds.addAll(pageIds)

                        currentCursor = extractCursorFromResponse(result)
                        hasNext = extractHasNextFromResponse(result)
                    }

                    // Cursor가 exclusive하게 동작하므로 중복이 없어야 함
                    val uniqueIds = allEventIds.toSet()
                    Assertions.assertEquals(
                        allEventIds.size,
                        uniqueIds.size,
                        "중복된 eventId가 발견되었습니다. 전체: ${allEventIds.size}, 고유: ${uniqueIds.size}"
                    )
                    Assertions.assertEquals(11, allEventIds.size, "총 11개 이벤트 조회 (ACTIVE 그룹)")
                }

                @Test
                @DisplayName("hasNext 플래그 정확성 검증")
                fun cursorPaginationHasNextConsistencyTest() {
                    var currentCursor: String? = null
                    val allEventIds = mutableListOf<Any>()

                    // ACTIVE 그룹 11개: 5 + 5 + 1
                    // Page 1 (5개)
                    var result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("size", "5")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.pageInfo.hasNext").value(true))
                        .andExpect(jsonPath("$.pageInfo.nextCursor").exists())
                        .andReturn()
                    allEventIds.addAll(extractValuesFromResponse(result, "id"))
                    currentCursor = extractCursorFromResponse(result)

                    // Page 2 (5개)
                    result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("size", "5")
                            .param("cursor", currentCursor!!)
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content.length()").value(5))
                        .andExpect(jsonPath("$.pageInfo.hasNext").value(true))
                        .andExpect(jsonPath("$.pageInfo.nextCursor").exists())
                        .andReturn()
                    allEventIds.addAll(extractValuesFromResponse(result, "id"))
                    currentCursor = extractCursorFromResponse(result)

                    // Page 3 (마지막 페이지, 1개)
                    result = mockMvc.perform(
                        get("/api/v2/events")
                            .param("size", "5")
                            .param("cursor", currentCursor!!)
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content.length()").value(1))
                        .andExpect(jsonPath("$.pageInfo.hasNext").value(false))
                        .andReturn()
                    allEventIds.addAll(extractValuesFromResponse(result, "id"))

                    // 전체 개수 검증: 11개 unique events (ACTIVE 그룹만, 중복 없음)
                    val uniqueIds = allEventIds.toSet()
                    Assertions.assertEquals(11, allEventIds.size, "총 11개의 이벤트를 조회해야 합니다")
                    Assertions.assertEquals(11, uniqueIds.size, "중복 없이 11개 unique 이벤트 조회")
                }

                @Test
                @DisplayName("정렬 순서 일관성 검증 (CREATED_AT)")
                fun cursorPaginationSortOrderConsistencyTest() {
                    val allCreatedDates = mutableListOf<Any>()
                    var currentCursor: String? = null

                    // 3페이지 반복 조회
                    repeat(3) { pageIndex ->
                        val requestBuilder = get("/api/v2/events")
                            .param("size", "5")
                            .param("field", "CREATED_AT")
                        if (currentCursor != null) {
                            requestBuilder.param("cursor", currentCursor!!)
                        }

                        val result = mockMvc.perform(requestBuilder)
                            .andDo(print())
                            .andExpect(status().isOk)
                            .andExpect(jsonPath("$.content").isArray)
                            .andReturn()

                        val pageCreatedDates = extractValuesFromResponse(result, "createdAt")
                        allCreatedDates.addAll(pageCreatedDates)

                        currentCursor = extractCursorFromResponse(result)
                    }

                    // 전체 정렬 순서 검증 (내림차순)
                    assertDateDescendingOrder(allCreatedDates, "CREATED_AT (across pages)")
                }
            }

            @Nested
            @DisplayName("복합 조합")
            inner class CombinationTests {

                @Test
                @DisplayName("모든 필터 조합 테스트")
                fun allFiltersCombinationTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("searchKeyword", "세미나")
                            .param("types", "SEMINAR,WEBINAR")
                            .param("statusGroup", "ACTIVE")
                            .param("hostId", host1.id.toString())
                            .param("field", "CREATED_AT")
                            .param("size", "5")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                }

                @Test
                @DisplayName("빈 결과 처리")
                fun emptyResultsTest() {
                    mockMvc.perform(
                        get("/api/v2/events")
                            .param("searchKeyword", "존재하지않는키워드")
                            .param("types", "CONTEST")
                            .param("statusGroup", "FINISHED")
                            .param("size", "10")
                    )
                        .andDo(print())
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.content").isArray)
                        .andExpect(jsonPath("$.content.length()").value(0))
                        .andExpect(jsonPath("$.pageInfo.hasNext").value(false))
                        .andExpect(jsonPath("$.pageInfo.pageSize").value(0))
                }
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        inner class FailureTests {

            @Test
            @DisplayName("size 범위 초과 시 에러 처리 확인")
            fun sizeExceedsMaximumTest() {
                mockMvc.perform(
                    get("/api/v2/events")
                        .param("size", "101")
                )
                    .andDo(print())
                    // require() 검증 실패 시 GlobalExceptionHandler가 500으로 처리
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("status와 statusGroup 동시 사용 시 500 에러")
            fun statusAndStatusGroupConflictTest() {
                mockMvc.perform(
                    get("/api/v2/events")
                        .param("status", "RECRUITING")
                        .param("statusGroup", "ACTIVE")
                        .param("size", "10")
                )
                    .andDo(print())
                    // require() 검증 실패 시 GlobalExceptionHandler가 500으로 처리
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("잘못된 정렬 필드 전달 시 처리")
            fun invalidSortFieldTest() {
                mockMvc.perform(
                    get("/api/v2/events")
                        .param("field", "INVALID_FIELD")
                        .param("size", "10")
                )
                    .andDo(print())
                    // 잘못된 enum 값은 Spring에서 400으로 처리됨
                    .andExpect(status().isBadRequest)
            }
        }
    }
}
