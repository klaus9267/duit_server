package duit.server.domain.host.controller

import duit.server.domain.event.entity.Event
import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Host API 통합 테스트")
class HostControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var user: User
    private lateinit var nonAdminUser: User
    private lateinit var host1: Host
    private lateinit var host2: Host
    private lateinit var host3: Host

    @BeforeEach
    fun setUp() {
        user = TestFixtures.user(nickname = "호스트테스트관리자", providerId = "host-admin-1")
        nonAdminUser = TestFixtures.user(
            nickname = "일반사용자",
            providerId = "host-non-admin-1",
            email = "non-admin@example.com",
        )
        entityManager.persist(user)
        entityManager.persist(nonAdminUser)
        // user 만 관리자로 등록 (nonAdminUser 는 403 검증용)
        entityManager.persist(TestFixtures.admin(user = user))

        host1 = TestFixtures.host(name = "주최기관A")
        host2 = TestFixtures.host(name = "주최기관B")
        host3 = TestFixtures.host(name = "주최기관C")
        entityManager.persist(host1)
        entityManager.persist(host2)
        entityManager.persist(host3)

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("GET /api/v1/hosts - 주최측 목록 조회")
    inner class GetHostsTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("기본 페이지네이션으로 목록을 조회한다")
            fun getHostsDefault() {
                mockMvc.perform(get("/api/v1/hosts"))
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(3)))
            }

            @Test
            @DisplayName("page와 size를 지정하여 조회한다")
            fun getHostsWithPageSize() {
                mockMvc.perform(
                    get("/api/v1/hosts")
                        .param("page", "0")
                        .param("size", "2")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.pageInfo.pageSize").value(2))
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/hosts - 주최측 생성")
    inner class CreateHostTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("name만으로 주최측을 생성한다")
            fun createHostWithNameOnly() {
                mockMvc.perform(
                    multipart("/api/v1/hosts")
                        .param("name", "새로운 주최기관")
                        .header("Authorization", authHeader(user.id!!))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                    .andDo(print())
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.name").value("새로운 주최기관"))
            }

            @Test
            @DisplayName("name과 thumbnail로 주최측을 생성한다")
            fun createHostWithThumbnail() {
                val thumbnail = MockMultipartFile(
                    "thumbnail", "logo.png", "image/png", "fake-image".toByteArray()
                )

                mockMvc.perform(
                    multipart("/api/v1/hosts")
                        .file(thumbnail)
                        .param("name", "썸네일 주최기관")
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.name").value("썸네일 주최기관"))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(
                    multipart("/api/v1/hosts")
                        .param("name", "새 주최기관")
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }

            @Test
            @DisplayName("관리자가 아닌 사용자는 403을 반환한다")
            fun forbiddenForNonAdmin() {
                mockMvc.perform(
                    multipart("/api/v1/hosts")
                        .param("name", "새 주최기관")
                        .header("Authorization", authHeader(nonAdminUser.id!!))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                    .andDo(print())
                    .andExpect(status().isForbidden)
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/hosts/{hostId} - 주최측 수정")
    inner class UpdateHostTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("주최측 이름을 수정한다")
            fun updateHostName() {
                val data = MockMultipartFile(
                    "data", "", "application/json",
                    objectMapper.writeValueAsBytes(mapOf("name" to "수정된 주최기관", "deleteThumbnail" to false))
                )

                mockMvc.perform(
                    multipart("/api/v1/hosts/{hostId}", host1.id!!)
                        .file(data)
                        .with { it.method = "PUT"; it }
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.name").value("수정된 주최기관"))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("존재하지 않는 hostId로 수정하면 404를 반환한다")
            fun notFoundHost() {
                val data = MockMultipartFile(
                    "data", "", "application/json",
                    objectMapper.writeValueAsBytes(mapOf("name" to "수정", "deleteThumbnail" to false))
                )

                mockMvc.perform(
                    multipart("/api/v1/hosts/{hostId}", 999999)
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
                val data = MockMultipartFile(
                    "data", "", "application/json",
                    objectMapper.writeValueAsBytes(mapOf("name" to "수정", "deleteThumbnail" to false))
                )

                mockMvc.perform(
                    multipart("/api/v1/hosts/{hostId}", host1.id!!)
                        .file(data)
                        .with { it.method = "PUT"; it }
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }

            @Test
            @DisplayName("관리자가 아닌 사용자는 403을 반환한다")
            fun forbiddenForNonAdmin() {
                val data = MockMultipartFile(
                    "data", "", "application/json",
                    objectMapper.writeValueAsBytes(mapOf("name" to "수정", "deleteThumbnail" to false))
                )

                mockMvc.perform(
                    multipart("/api/v1/hosts/{hostId}", host1.id!!)
                        .file(data)
                        .with { it.method = "PUT"; it }
                        .header("Authorization", authHeader(nonAdminUser.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isForbidden)
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/hosts/{hostId} - 주최측 단건 삭제")
    inner class DeleteHostTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("연결된 행사가 없으면 주최측을 삭제한다")
            fun deleteHost() {
                mockMvc.perform(
                    delete("/api/v1/hosts/{hostId}", host3.id!!)
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
            @DisplayName("존재하지 않는 hostId로 삭제하면 404를 반환한다")
            fun notFoundHost() {
                mockMvc.perform(
                    delete("/api/v1/hosts/{hostId}", 999999)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isNotFound)
            }

            @Test
            @DisplayName("연결된 행사가 있으면 409를 반환한다")
            fun conflictWhenEventLinked() {
                val event: Event = TestFixtures.event(host = host1)
                entityManager.persist(event)
                entityManager.flush()
                entityManager.clear()

                mockMvc.perform(
                    delete("/api/v1/hosts/{hostId}", host1.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isConflict)
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
            }

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(delete("/api/v1/hosts/{hostId}", host1.id!!))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }

            @Test
            @DisplayName("관리자가 아닌 사용자는 403을 반환한다")
            fun forbiddenForNonAdmin() {
                mockMvc.perform(
                    delete("/api/v1/hosts/{hostId}", host3.id!!)
                        .header("Authorization", authHeader(nonAdminUser.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isForbidden)
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/hosts - 주최측 일괄 삭제 (부분 성공)")
    inner class BatchDeleteHostTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("연결된 행사가 없는 주최측들은 모두 삭제되고 blockedHosts는 비어있다")
            fun allDeletable() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("hostIds" to listOf(host1.id!!, host2.id!!))
                )

                mockMvc.perform(
                    delete("/api/v1/hosts")
                        .header("Authorization", authHeader(user.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.deletedCount").value(2))
                    .andExpect(jsonPath("$.blockedHosts").isArray)
                    .andExpect(jsonPath("$.blockedHosts").value(hasSize<Any>(0)))
            }

            @Test
            @DisplayName("일부에 행사가 있으면 가능한 것만 삭제하고 차단된 주최측은 blockedHosts에 포함된다")
            fun partialDeletable() {
                val event: Event = TestFixtures.event(host = host2)
                entityManager.persist(event)
                entityManager.flush()
                entityManager.clear()

                val requestBody = objectMapper.writeValueAsString(
                    mapOf("hostIds" to listOf(host1.id!!, host2.id!!, host3.id!!))
                )

                mockMvc.perform(
                    delete("/api/v1/hosts")
                        .header("Authorization", authHeader(user.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.deletedCount").value(2))
                    .andExpect(jsonPath("$.blockedHosts").value(hasSize<Any>(1)))
                    .andExpect(jsonPath("$.blockedHosts[0].id").value(host2.id!!.toInt()))
                    .andExpect(jsonPath("$.blockedHosts[0].name").value("주최기관B"))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("hostIds" to listOf(host1.id!!))
                )

                mockMvc.perform(
                    delete("/api/v1/hosts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }

            @Test
            @DisplayName("관리자가 아닌 사용자는 403을 반환한다")
            fun forbiddenForNonAdmin() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("hostIds" to listOf(host1.id!!))
                )

                mockMvc.perform(
                    delete("/api/v1/hosts")
                        .header("Authorization", authHeader(nonAdminUser.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isForbidden)
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            }

            @Test
            @DisplayName("hostIds가 비어있으면 400을 반환한다")
            fun emptyHostIds() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("hostIds" to emptyList<Long>())
                )

                mockMvc.perform(
                    delete("/api/v1/hosts")
                        .header("Authorization", authHeader(user.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }
        }
    }
}
