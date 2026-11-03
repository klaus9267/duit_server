package duit.server.application.config

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.host.entity.Host
import duit.server.domain.job.entity.JobPosting
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

/**
 * 상세 조회의 SQL 수 (performance/decisions.md D-026).
 * A-2 이후 남은 요청당 SQL 1.92개 중 0.82개가 상세 조회다: 채용 상세는 공고·회사·첨부·키워드를 따로 4번, 행사 상세는 행사·주최를 따로 2번 읽었다.
 * 다중 주최(event_hosts) 이후에는 주최 목록을 따로 1번 더 읽었다 — 주최 연결·주최까지 한 번에 읽는다.
 * 요청 단위 SQL 을 세려면 테스트 트랜잭션 밖에서 돌려야 하므로 픽스처는 커밋하고 직접 지운다.
 */
@Import(JdbcCallCounterConfig::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("상세 조회 SQL 수")
class DetailQueryCountIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var host: Host
    private lateinit var coHost: Host
    private lateinit var event: Event
    private lateinit var coHostedEvent: Event
    private lateinit var jobPosting: JobPosting

    @BeforeEach
    fun setUpCommittedFixtures() {
        transactionTemplate.executeWithoutResult {
            host = TestFixtures.host(name = "상세 조회 주최").also(entityManager::persist)
            event = TestFixtures.event(
                title = "상세 조회 행사",
                status = EventStatus.RECRUITING,
                statusGroup = EventStatusGroup.ACTIVE,
                host = host,
            ).also(entityManager::persist)
            coHost = TestFixtures.host(name = "상세 조회 공동 주최").also(entityManager::persist)
            coHostedEvent = TestFixtures.event(
                title = "상세 조회 공동 주최 행사",
                status = EventStatus.RECRUITING,
                statusGroup = EventStatusGroup.ACTIVE,
                hosts = listOf(host, coHost),
            ).also(entityManager::persist)
            jobPosting = TestFixtures.jobPosting(companyName = "상세 조회 병원").apply {
                corpAttachList += listOf("https://example.com/a.hwp", "https://example.com/b.pdf")
                keywordList += listOf("간호사", "병동")
            }.also(entityManager::persist)
        }
    }

    @AfterEach
    fun deleteCommittedFixtures() {
        transactionTemplate.executeWithoutResult {
            val posting = entityManager.find(JobPosting::class.java, jobPosting.id)
            val company = posting.company
            entityManager.remove(posting)
            company?.let { entityManager.remove(entityManager.merge(it)) }
            val eventIds = listOf(event.id, coHostedEvent.id)
            entityManager.createQuery("DELETE FROM View v WHERE v.event.id IN :ids").setParameter("ids", eventIds).executeUpdate()
            entityManager.createQuery("DELETE FROM EventHost eh WHERE eh.event.id IN :ids").setParameter("ids", eventIds).executeUpdate()
            entityManager.createQuery("DELETE FROM Event e WHERE e.id IN :ids").setParameter("ids", eventIds).executeUpdate()
            entityManager.createQuery("DELETE FROM Host h WHERE h.id IN :ids").setParameter("ids", listOf(host.id, coHost.id)).executeUpdate()
        }
    }

    @Test
    fun `행사 상세는 행사와 주최를 SQL 1개로 읽는다`() {
        val statements = countStatements {
            mockMvc.perform(get("/api/v2/events/${event.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.host.name").value(host.name))
        }

        assertEquals(1, statements)
    }

    @Test
    fun `여러 주최 행사도 행사와 주최 전부를 등록 순서대로 SQL 1개로 읽는다`() {
        val statements = countStatements {
            mockMvc.perform(get("/api/v2/events/${coHostedEvent.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.host.name").value(host.name))
                .andExpect(jsonPath("$.hosts.length()").value(2))
                .andExpect(jsonPath("$.hosts[0].name").value(host.name))
                .andExpect(jsonPath("$.hosts[1].name").value(coHost.name))
        }

        assertEquals(1, statements)
    }

    @Test
    fun `채용 상세는 공고·회사·첨부·키워드를 SQL 2개로 읽는다`() {
        val statements = countStatements {
            mockMvc.perform(get("/api/v1/job-postings/${jobPosting.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.company.corpNm").value("상세 조회 병원"))
                .andExpect(jsonPath("$.corpAttachList.length()").value(2))
                .andExpect(jsonPath("$.keywordList.length()").value(2))
        }

        assertEquals(2, statements)
    }

    private fun countStatements(request: () -> Unit): Int {
        JdbcCallCounter.reset()
        request()
        return JdbcCallCounter.snapshot()["prepareStatement"] ?: 0
    }
}
