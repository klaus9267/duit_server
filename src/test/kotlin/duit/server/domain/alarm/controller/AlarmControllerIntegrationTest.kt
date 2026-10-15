package duit.server.domain.alarm.controller

import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.host.entity.Host
import duit.server.domain.job.entity.SalaryType
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Alarm API 통합 테스트")
class AlarmControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var user: User
    private lateinit var otherUser: User
    private lateinit var host: Host
    private lateinit var event: Event
    private lateinit var unreadAlarm: Alarm
    private lateinit var readAlarm: Alarm

    @BeforeEach
    fun setUp() {
        user = TestFixtures.user(nickname = "알람유저", providerId = "alarm-provider")
        entityManager.persist(user)

        otherUser = TestFixtures.user(nickname = "다른유저", providerId = "other-provider", email = "other@example.com")
        entityManager.persist(otherUser)

        host = TestFixtures.host(name = "알람테스트주최")
        entityManager.persist(host)

        event = TestFixtures.event(
            title = "알람 테스트 행사",
            host = host,
            status = EventStatus.ACTIVE,
            statusGroup = EventStatusGroup.ACTIVE
        )
        entityManager.persist(event)

        unreadAlarm = TestFixtures.alarm(user = user, event = event, type = AlarmType.EVENT_START, isRead = false)
        readAlarm = TestFixtures.alarm(user = user, event = event, type = AlarmType.RECRUITMENT_END, isRead = true)
        entityManager.persist(unreadAlarm)
        entityManager.persist(readAlarm)

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("GET /api/v1/alarms - 알림 목록 조회")
    inner class GetAlarmsTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("전체 알림 목록을 조회한다")
            fun getAllAlarms() {
                mockMvc.perform(
                    get("/api/v1/alarms")
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].type").exists())
                    .andExpect(jsonPath("$.content[0].isRead").exists())
                    .andExpect(jsonPath("$.content[0].event").exists())
                    .andExpect(jsonPath("$.content[0].event.title").value("알람 테스트 행사"))
            }

            @Test
            @DisplayName("읽지 않은 알림만 필터링하여 조회한다")
            fun getUnreadAlarms() {
                mockMvc.perform(
                    get("/api/v1/alarms")
                        .header("Authorization", authHeader(user.id!!))
                        .param("isRead", "false")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(1))
            }

            @Test
            @DisplayName("읽은 알림만 필터링하여 조회한다")
            fun getReadAlarms() {
                mockMvc.perform(
                    get("/api/v1/alarms")
                        .header("Authorization", authHeader(user.id!!))
                        .param("isRead", "true")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(1))
            }

            @Test
            @DisplayName("페이지네이션으로 조회한다")
            fun getAlarmsWithPagination() {
                mockMvc.perform(
                    get("/api/v1/alarms")
                        .header("Authorization", authHeader(user.id!!))
                        .param("page", "0")
                        .param("size", "1")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.pageInfo.totalElements").value(2))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(get("/api/v1/alarms"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/alarms/{alarmId}/read - 알림 읽음 처리")
    inner class MarkAsReadTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("알림을 읽음 처리한다")
            fun markAsRead() {
                mockMvc.perform(
                    patch("/api/v1/alarms/{alarmId}/read", unreadAlarm.id!!)
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
            @DisplayName("존재하지 않는 alarmId로 요청하면 에러를 반환한다")
            fun notFoundAlarm() {
                mockMvc.perform(
                    patch("/api/v1/alarms/{alarmId}/read", 999999)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("다른 유저의 알림을 읽음 처리하면 에러를 반환한다")
            fun otherUsersAlarm() {
                mockMvc.perform(
                    patch("/api/v1/alarms/{alarmId}/read", unreadAlarm.id!!)
                        .header("Authorization", authHeader(otherUser.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(patch("/api/v1/alarms/{alarmId}/read", unreadAlarm.id!!))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/alarms/read-all - 전체 알림 읽음 처리")
    inner class MarkAllAsReadTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("전체 알림을 읽음 처리한다")
            fun markAllAsRead() {
                mockMvc.perform(
                    patch("/api/v1/alarms/read-all")
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
                mockMvc.perform(patch("/api/v1/alarms/read-all"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/alarms/{alarmId} - 알림 삭제")
    inner class DeleteAlarmTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("알림을 삭제한다")
            fun deleteAlarm() {
                mockMvc.perform(
                    delete("/api/v1/alarms/{alarmId}", readAlarm.id!!)
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
            @DisplayName("존재하지 않는 alarmId로 요청하면 에러를 반환한다")
            fun notFoundAlarm() {
                mockMvc.perform(
                    delete("/api/v1/alarms/{alarmId}", 999999)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("다른 유저의 알림을 삭제하면 에러를 반환한다")
            fun otherUsersAlarm() {
                mockMvc.perform(
                    delete("/api/v1/alarms/{alarmId}", readAlarm.id!!)
                        .header("Authorization", authHeader(otherUser.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(delete("/api/v1/alarms/{alarmId}", unreadAlarm.id!!))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/alarms - 알림 전체 삭제")
    inner class DeleteAllAlarmsTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("전체 알림을 삭제한다")
            fun deleteAllAlarms() {
                mockMvc.perform(
                    delete("/api/v1/alarms")
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isNoContent)
            }

            @Test
            @DisplayName("읽은 알림만 삭제한다")
            fun deleteReadOnlyAlarms() {
                mockMvc.perform(
                    delete("/api/v1/alarms")
                        .header("Authorization", authHeader(user.id!!))
                        .param("readOnly", "true")
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
                mockMvc.perform(delete("/api/v1/alarms"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v2/alarms - 알림 목록 조회 (V2)")
    inner class GetAlarmsV2Tests {

        @Test
        @DisplayName("최저임금 미달로 바뀐 채용공고의 알람은 목록과 개수에서 제외한다")
        fun excludesBelowMinimumWagePostingAlarms() {
            val visiblePosting = TestFixtures.jobPosting(title = "알람 정상 공고").also(entityManager::persist)
            val flaggedPosting = TestFixtures.jobPosting(
                title = "알람 최저임금미달 공고",
                salaryType = SalaryType.HOURLY,
                salaryMin = 9_000L * SalaryType.HOURLY.annualMultiplier,
                salaryMax = null,
            ).also {
                assertThat(it.belowMinimumWage).isTrue()
                entityManager.persist(it)
            }
            val alarmUser = entityManager.getReference(User::class.java, user.id!!)
            listOf(visiblePosting, flaggedPosting).forEach { posting ->
                entityManager.persist(
                    Alarm(user = alarmUser, jobPosting = posting, type = AlarmType.JOB_SUBSCRIPTION_KEYWORD)
                )
            }
            entityManager.flush()
            entityManager.clear()

            mockMvc.perform(
                get("/api/v2/alarms")
                    .header("Authorization", authHeader(user.id!!))
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.pageInfo.totalElements").value(3))
                .andExpect(jsonPath("$.content[?(@.target.jobPosting.wantedTitle == '알람 정상 공고')]").isNotEmpty)
                .andExpect(jsonPath("$.content[?(@.target.jobPosting.wantedTitle == '알람 최저임금미달 공고')]").isEmpty)

            mockMvc.perform(
                get("/api/v2/alarms")
                    .header("Authorization", authHeader(user.id!!))
                    .param("isRead", "false")
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.pageInfo.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.target.jobPosting.wantedTitle == '알람 최저임금미달 공고')]").isEmpty)
        }
    }

    @Nested
    @DisplayName("POST /api/v1/alarms/test/** - 제거된 테스트 푸시 API")
    inner class RemovedTestPushTests {

        @Test
        @DisplayName("인증된 사용자도 테스트 푸시 API를 호출할 수 없다")
        fun removedEndpoints() {
            mockMvc.perform(
                post("/api/v1/alarms/test/custom")
                    .header("Authorization", authHeader(user.id!!))
                    .param("title", "제목")
                    .param("body", "내용")
            )
                .andDo(print())
                .andExpect(status().isNotFound)

            mockMvc.perform(
                post("/api/v1/alarms/test/custom2")
                    .header("Authorization", authHeader(user.id!!))
                    .param("alarmType", AlarmType.EVENT_START.name)
                    .param("eventId", event.id!!.toString())
            )
                .andDo(print())
                .andExpect(status().isNotFound)
        }
    }
}
