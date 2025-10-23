package duit.server.application.config

import duit.server.domain.common.dto.ErrorResponse
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@OpenAPIDefinition(
    info = Info(
        title = "DuIt API 명세서",
        version = "v1.0.0",
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@Configuration
class SwaggerConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
    }

    @Bean
    fun openApi(): GroupedOpenApi {
        val paths = arrayOf("/**")

        return GroupedOpenApi.builder()
            .group("DuIt OPEN API v1")
            .pathsToMatch(*paths)
            .addOperationCustomizer(commonApiResponseCustomizer())
            .addOperationCustomizer(authApiResponseCustomizer())
            .build()
    }

    /**
     * 모든 API 엔드포인트에 공통 응답(400, 500)을 자동으로 추가
     * @CommonApiResponses 어노테이션 대체
     */
    @Bean
    fun commonApiResponseCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, _ ->
            val responses = operation.responses

            // 400 Bad Request
            val badRequestExample = Example().apply {
                value = mapOf(
                    "code" to "COMMON_002",
                    "message" to "잘못된 요청입니다.",
                    "timestamp" to "2024-01-01T10:00:00"
                )
            }

            responses.addApiResponse(
                "400",
                ApiResponse()
                    .description("잘못된 요청")
                    .content(
                        Content().addMediaType(
                            "application/json",
                            MediaType()
                                .schema(Schema<ErrorResponse>().`$ref`("#/components/schemas/ErrorResponse"))
                                .addExamples("잘못된 요청", badRequestExample)
                        )
                    )
            )

            // 500 Internal Server Error
            val serverErrorExample = Example().apply {
                value = mapOf(
                    "code" to "COMMON_001",
                    "message" to "서버 내부 오류가 발생했습니다.",
                    "timestamp" to "2024-01-01T10:00:00"
                )
            }

            responses.addApiResponse(
                "500",
                ApiResponse()
                    .description("서버 내부 오류")
                    .content(
                        Content().addMediaType(
                            "application/json",
                            MediaType()
                                .schema(Schema<ErrorResponse>().`$ref`("#/components/schemas/ErrorResponse"))
                                .addExamples("서버 내부 오류", serverErrorExample)
                        )
                    )
            )

            operation
        }
    }

    /**
     * 인증이 필요한 API 엔드포인트에 인증 관련 응답(401, 403)을 자동으로 추가
     * @AuthApiResponses 어노테이션 대체
     */
    @Bean
    fun authApiResponseCustomizer(): OperationCustomizer {
        // SecurityConfig의 permitAll() 경로 패턴 (인증 불필요)
        val publicPathPrefixes = setOf(
            "/h2-console",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/actuator",
            "/api/v1/users/check-nickname",
            "/api/v1/webhooks",
            "/api/v1/hosts",
            "/uploads",
            "/api/v1/auth",
            "/api/v1/admin/auth"
        )

        return OperationCustomizer { operation, handlerMethod ->
            // 현재 경로가 public인지 확인
            val operationPath = handlerMethod.method.declaringClass.getAnnotation(org.springframework.web.bind.annotation.RequestMapping::class.java)
                ?.value?.firstOrNull() ?: ""
            val methodPath = (handlerMethod.method.getAnnotation(org.springframework.web.bind.annotation.GetMapping::class.java)?.value?.firstOrNull()
                ?: handlerMethod.method.getAnnotation(org.springframework.web.bind.annotation.PostMapping::class.java)?.value?.firstOrNull()
                ?: handlerMethod.method.getAnnotation(org.springframework.web.bind.annotation.PatchMapping::class.java)?.value?.firstOrNull()
                ?: handlerMethod.method.getAnnotation(org.springframework.web.bind.annotation.DeleteMapping::class.java)?.value?.firstOrNull()
                ?: "")
            val fullPath = operationPath + methodPath

            // Public path가 아닌 경우에만 401, 403 추가
            val isPublicPath = publicPathPrefixes.any { fullPath.startsWith(it) }

            if (!isPublicPath) {
                val responses = operation.responses

                // 401 Unauthorized
                val unauthorizedExample = Example().apply {
                    value = mapOf(
                        "code" to "COMMON_003",
                        "message" to "인증이 필요합니다.",
                        "timestamp" to "2024-01-01T10:00:00"
                    )
                }

                responses.addApiResponse(
                    "401",
                    ApiResponse()
                        .description("인증 필요")
                        .content(
                            Content().addMediaType(
                                "application/json",
                                MediaType()
                                    .schema(Schema<ErrorResponse>().`$ref`("#/components/schemas/ErrorResponse"))
                                    .addExamples("인증 필요", unauthorizedExample)
                            )
                        )
                )

                // 403 Forbidden
                val forbiddenExample = Example().apply {
                    value = mapOf(
                        "code" to "COMMON_004",
                        "message" to "접근 권한이 없습니다.",
                        "timestamp" to "2024-01-01T10:00:00"
                    )
                }

                responses.addApiResponse(
                    "403",
                    ApiResponse()
                        .description("접근 권한 없음")
                        .content(
                            Content().addMediaType(
                                "application/json",
                                MediaType()
                                    .schema(Schema<ErrorResponse>().`$ref`("#/components/schemas/ErrorResponse"))
                                    .addExamples("접근 권한 없음", forbiddenExample)
                            )
                        )
                )
            }

            operation
        }
    }
}
