package duit.server.domain.event.repository

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.host.entity.Host
import duit.server.support.IntegrationTestSupport
import duit.server.support.RequiresDocker
import duit.server.support.fixture.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 조회수 정렬은 MySQL 전용 native SQL(FORCE INDEX)이라 H2 테스트로는 돌지 않는다.
 * 같은 날짜 범위 필터를 QueryDSL 경로(ID 정렬)와 native 경로(조회수 정렬)에 걸어 결과가 같은지 실제 MySQL 에서 본다.
 */
@RequiresDocker
@Testcontainers
@DisplayName("행사 날짜 범위 필터 (MySQL)")
class EventDateRangeFilterMySqlTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var eventRepository: EventRepository

    private lateinit var host: Host
    private val ids = mutableMapOf<String, Long>()

    @BeforeEach
    fun setUpEvents() {
        host = TestFixtures.host(name = "MySQL 날짜필터 주최").also(entityManager::persist)
        persist("11/01~11/03", views = 50, startAt = at(11, 1), endAt = at(11, 3))
        persist("11/05 하루", views = 40, startAt = at(11, 5))
        persist("10/25~11/10", views = 30, startAt = at(10, 25), endAt = at(11, 10), recruitmentEndAt = at(10, 20))
        persist("11/20~11/22", views = 20, startAt = at(11, 20), endAt = at(11, 22), recruitmentStartAt = at(10, 15))
        persist("모집 10/01~10/10", views = 10, startAt = at(12, 1), recruitmentStartAt = at(10, 1), recruitmentEndAt = at(10, 10))
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    fun `조회수 정렬도 행사일 범위 필터를 ID 정렬과 똑같이 적용한다`() {
        val range = arrayOf(LocalDate.of(2031, 11, 4), LocalDate.of(2031, 11, 6))

        val byViews = find(PaginationField.VIEW_COUNT, eventDateFrom = range[0], eventDateTo = range[1])

        assertThat(byViews).containsExactly(ids["11/05 하루"], ids["10/25~11/10"])
        assertThat(find(PaginationField.ID, eventDateFrom = range[0], eventDateTo = range[1])).containsExactlyInAnyOrderElementsOf(byViews)
    }

    @Test
    fun `조회수 정렬도 모집일 범위 필터를 ID 정렬과 똑같이 적용한다`() {
        val from = LocalDate.of(2031, 10, 12)
        val to = LocalDate.of(2031, 10, 16)

        val byViews = find(PaginationField.VIEW_COUNT, recruitmentDateFrom = from, recruitmentDateTo = to)

        // 모집 일정이 없는 행사도 행사 시작까지 포함한다
        assertThat(byViews).containsExactly(ids["11/01~11/03"], ids["11/05 하루"], ids["10/25~11/10"], ids["11/20~11/22"])
        assertThat(find(PaginationField.ID, recruitmentDateFrom = from, recruitmentDateTo = to)).containsExactlyInAnyOrderElementsOf(byViews)
    }

    @Test
    fun `모집 마감 대체 시각이 자정이면 그 날짜까지 양쪽 정렬에 포함한다`() {
        persist("자정 일정 없음", views = 60, startAt = LocalDateTime.of(2031, 10, 25, 0, 0))
        persist("자정 시작만", views = 55, startAt = LocalDateTime.of(2031, 10, 25, 0, 0), recruitmentStartAt = at(10, 15))
        entityManager.flush()
        entityManager.clear()
        val date = LocalDate.of(2031, 10, 25)
        val byViews = find(PaginationField.VIEW_COUNT, recruitmentDateFrom = date, recruitmentDateTo = date)
        assertThat(byViews).containsExactly(ids["자정 일정 없음"], ids["자정 시작만"], ids["11/01~11/03"], ids["11/05 하루"], ids["11/20~11/22"])
        assertThat(find(PaginationField.ID, recruitmentDateFrom = date, recruitmentDateTo = date)).containsExactlyInAnyOrderElementsOf(byViews)
        val nextDay = date.plusDays(1)
        assertThat(find(PaginationField.VIEW_COUNT, recruitmentDateFrom = nextDay)).doesNotContain(ids["자정 일정 없음"], ids["자정 시작만"])
    }

    @Test
    fun `날짜 필터가 없으면 조회수 정렬 결과가 그대로다`() {
        assertThat(find(PaginationField.VIEW_COUNT)).containsExactly(
            ids["11/01~11/03"], ids["11/05 하루"], ids["10/25~11/10"], ids["11/20~11/22"], ids["모집 10/01~10/10"]
        )
    }

    private fun find(
        field: PaginationField,
        eventDateFrom: LocalDate? = null,
        eventDateTo: LocalDate? = null,
        recruitmentDateFrom: LocalDate? = null,
        recruitmentDateTo: LocalDate? = null,
    ): List<Long?> = eventRepository.findEvents(
        EventCursorPaginationParam(
            field = field,
            size = 50,
            hostId = host.id,
            eventDateFrom = eventDateFrom,
            eventDateTo = eventDateTo,
            recruitmentDateFrom = recruitmentDateFrom,
            recruitmentDateTo = recruitmentDateTo,
        ),
        null,
    ).map { it.id }

    private fun at(month: Int, day: Int): LocalDateTime = LocalDateTime.of(2031, month, day, 10, 0)

    private fun persist(
        key: String,
        views: Int,
        startAt: LocalDateTime,
        endAt: LocalDateTime? = null,
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
    ) {
        val event = TestFixtures.event(
            title = "MySQL 날짜필터 $key",
            startAt = startAt,
            endAt = endAt,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
            status = EventStatus.EVENT_WAITING,
            statusGroup = EventStatusGroup.ACTIVE,
            host = host,
        )
        event.view.count = views
        entityManager.persist(event)
        ids[key] = event.id!!
    }

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.0")

        @DynamicPropertySource
        @JvmStatic
        fun mysqlProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            registry.add("spring.datasource.driver-class-name") { mysql.driverClassName }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.MySQLDialect" }
        }
    }
}
