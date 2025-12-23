package duit.server.application.config

import io.micrometer.core.instrument.config.MeterFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig {

    // HTTP 요청 메트릭을 특정 경로로만 제한
    // - 수집 대상: /api/ 및 /upload/ 하위 경로
    // - 제외 대상: /actuator/, /swagger-ui/, /v3/api-docs/ (헬스체크, 모니터링, 문서화)
    @Bean
    fun httpRequestMeterFilter(): MeterFilter {
        return MeterFilter.deny { id ->
            if (id.name == "http.server.requests") {
                val uri = id.getTag("uri")
                // /api/ 또는 /upload/로 시작하지 않으면 거부
                uri != null && !uri.startsWith("/api/") && !uri.startsWith("/uploads/")
            } else {
                false  // 다른 메트릭(JVM, 시스템 등)은 그대로 허용
            }
        }
    }
}
