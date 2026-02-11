package duit.server.domain.host.controller

import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
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
    private lateinit var host1: Host
    private lateinit var host2: Host
    private lateinit var host3: Host

    @BeforeEach
    fun setUp() {
        user = TestFixtures.user(nickname = "호스트테스트유저")
        entityManager.persist(user)

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
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/hosts/{hostId} - 주최측 삭제")
    inner class DeleteHostTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("주최측을 삭제한다")
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
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(delete("/api/v1/hosts/{hostId}", host1.id!!))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/hosts/batch - 주최측 일괄 삭제")
    inner class BatchDeleteHostTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("여러 주최측을 일괄 삭제한다")
            fun batchDeleteHosts() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("hostIds" to listOf(host1.id!!, host2.id!!))
                )

                mockMvc.perform(
                    delete("/api/v1/hosts/batch")
                        .header("Authorization", authHeader(user.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
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
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("hostIds" to listOf(host1.id!!))
                )

                mockMvc.perform(
                    delete("/api/v1/hosts/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }
}
