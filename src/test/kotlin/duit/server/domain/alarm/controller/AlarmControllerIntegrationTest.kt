package duit.server.domain.alarm.controller

import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Alarm API 통합 테스트")
class AlarmControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var user: User
    private lateinit var host: Host
    private lateinit var event: Event
    private lateinit var unreadAlarm: Alarm
    private lateinit var readAlarm: Alarm

    @BeforeEach
    fun setUp() {
        user = TestFixtures.user(nickname = "알람유저", providerId = "alarm-provider")
        entityManager.persist(user)

        host = TestFixtures.host(name = "알람테스트주최")
        entityManager.persist(host)

        event = TestFixtures.event(
            title = "알람 테스트 행사",
            host = host,
            isApproved = true,
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
}
