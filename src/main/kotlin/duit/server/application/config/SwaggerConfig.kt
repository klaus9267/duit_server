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
}
