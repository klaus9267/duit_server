package duit.server.application.security

import duit.server.application.common.RequireAdmin
import duit.server.support.IntegrationTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@DisplayName("관리자 경로 접근 제어 테스트")
class AdminEndpointAccessTest : IntegrationTestSupport() {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    private val openAdminEndpoints = setOf("POST /api/v1/admin/auth/login")

    private val adminEndpoints: Map<String, Boolean> by lazy {
        handlerMapping.handlerMethods
            .flatMap { (info, method) ->
                val paths = info.pathPatternsCondition?.patternValues.orEmpty()
                val httpMethods = info.methodsCondition.methods.map { it.name }.ifEmpty { listOf("ANY") }
                val requiresAdmin = method.getMethodAnnotation(RequireAdmin::class.java) != null
                paths.filter { it.startsWith("/api/v1/admin") }
                    .flatMap { path -> httpMethods.map { "$it $path" to requiresAdmin } }
            }
            .toMap()
    }

    @Test
    @DisplayName("관리자 경로의 엔드포인트는 로그인을 제외하고 모두 @RequireAdmin 이 필요하다")
    fun adminEndpointsRequireAdmin() {
        assertTrue(adminEndpoints.isNotEmpty(), "관리자 경로 엔드포인트를 찾지 못했습니다")

        val unguarded = adminEndpoints
            .filter { (endpoint, requiresAdmin) -> !requiresAdmin && endpoint !in openAdminEndpoints }
            .keys

        assertEquals(emptySet<String>(), unguarded, "@RequireAdmin 이 없는 관리자 경로 엔드포인트")
    }

    @Test
    @DisplayName("관리자 목록 조회·해제는 @RequireAdmin 으로 보호된다")
    fun adminManagementEndpointsAreGuarded() {
        listOf(
            "GET /api/v1/admins",
            "DELETE /api/v1/admins/{id}",
        ).forEach { endpoint ->
            assertEquals(true, adminEndpoints[endpoint], "$endpoint 에 @RequireAdmin 이 필요합니다")
        }
    }
}
