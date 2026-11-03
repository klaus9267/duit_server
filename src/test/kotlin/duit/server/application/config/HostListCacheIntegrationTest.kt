package duit.server.application.config

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.host.dto.HostUpdateRequest
import duit.server.domain.host.entity.Host
import duit.server.domain.host.service.HostService
import duit.server.support.IntegrationTestSupport
import duit.server.support.JdbcCallCounter
import duit.server.support.JdbcCallCounterConfig
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

/**
 * 캐시를 안 타던 목록 2종 (performance/decisions.md D-026).
 * - 주최 필터 행사 목록(행사 상세의 "이 주최 다른 행사", 트래픽 ~10%)
 * - 주최기관 전체 목록(page 0~3 × size 100 을 매번 통째로, ~7%, 정렬에 인덱스를 못 써 전체를 읽음 — H-05)
 * 요청 단위 SQL 을 세려고 테스트 트랜잭션 없이 돌리고 픽스처는 직접 지운다.
 */
@Import(JdbcCallCounterConfig::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("주최 필터 목록·주최기관 목록 캐시")
class HostListCacheIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var hostService: HostService

    private lateinit var hostA: Host
    private lateinit var hostB: Host
    private lateinit var eventA: Event
    private lateinit var eventB: Event

    @BeforeEach
    fun setUpCommittedFixtures() {
        transactionTemplate.executeWithoutResult {
            hostA = TestFixtures.host(name = "캐시 주최 A").also(entityManager::persist)
            hostB = TestFixtures.host(name = "캐시 주최 B").also(entityManager::persist)
            eventA = activeEvent("A 행사", hostA).also(entityManager::persist)
            eventB = activeEvent("B 행사", hostB).also(entityManager::persist)
        }
    }

    @AfterEach
    fun deleteCommittedFixtures() {
        transactionTemplate.executeWithoutResult {
            val eventIds = listOf(eventA.id, eventB.id)
            entityManager.createQuery("DELETE FROM View v WHERE v.event.id IN :ids").setParameter("ids", eventIds).executeUpdate()
            entityManager.createQuery("DELETE FROM Event e WHERE e.id IN :ids").setParameter("ids", eventIds).executeUpdate()
            entityManager.createQuery("DELETE FROM Host h WHERE h.id IN :ids").setParameter("ids", listOf(hostA.id, hostB.id)).executeUpdate()
        }
    }

    @Test
    fun `주최 필터 목록은 두 번째 요청부터 SQL 없이 응답한다`() {
        hostEvents(hostA).andExpect(jsonPath("$.content[0].id").value(eventA.id))

        val statements = countStatements { hostEvents(hostA).andExpect(jsonPath("$.content[0].id").value(eventA.id)) }

        assertEquals(0, statements)
    }

    @Test
    fun `주최마다 따로 캐시한다`() {
        hostEvents(hostA)

        hostEvents(hostB)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(eventB.id))
    }

    @Test
    fun `주최기관 목록은 두 번째 요청부터 SQL 없이 응답한다`() {
        hosts()

        val statements = countStatements { hosts() }

        assertEquals(0, statements)
    }

    @Test
    fun `주최기관을 고치면 주최기관 목록과 행사 목록 캐시가 비워진다`() {
        hosts()
        hostEvents(hostA)

        hostService.updateHost(hostA.id!!, HostUpdateRequest(name = "바뀐 주최 A"), null)

        hosts().andExpect(jsonPath("$.content[?(@.id == ${hostA.id})].name").value("바뀐 주최 A"))
        hostEvents(hostA).andExpect(jsonPath("$.content[0].host.name").value("바뀐 주최 A"))
    }

    private fun activeEvent(title: String, host: Host) =
        TestFixtures.event(title = title, status = EventStatus.RECRUITING, statusGroup = EventStatusGroup.ACTIVE, host = host)

    private fun hostEvents(host: Host): ResultActions =
        mockMvc.perform(
            get("/api/v2/events")
                .param("field", "CREATED_AT")
                .param("hostId", host.id.toString())
                .param("size", "4")
                .param("statusGroup", "ACTIVE")
        ).andExpect(status().isOk)

    private fun hosts(): ResultActions =
        mockMvc.perform(
            get("/api/v1/hosts")
                .param("field", "NAME")
                .param("page", "0")
                .param("size", "100")
                .param("sortDirection", "ASC")
        ).andExpect(status().isOk)

    private fun countStatements(request: () -> Unit): Int {
        JdbcCallCounter.reset()
        request()
        return JdbcCallCounter.snapshot()["prepareStatement"] ?: 0
    }
}
