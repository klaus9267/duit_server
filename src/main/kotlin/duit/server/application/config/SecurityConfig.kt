package duit.server.application.config

import duit.server.application.security.JwtAuthenticationEntryPoint
import duit.server.application.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.observation.SecurityObservationSettings
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    /**
     * Spring Security 의 필터·인증·인가 관측을 끈다. 켜 두면 보안 필터마다 Observation 을 만들어 요청 스레드 CPU 의 ~7% 를 쓴다.
     * 이 메트릭(spring_security_*)을 쓰는 대시보드는 없고, HTTP 요청 메트릭(http_server_requests)은 그대로 남는다 (performance D-019)
     */
    @Bean
    fun securityObservationSettings(): SecurityObservationSettings = SecurityObservationSettings.noObservations()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .httpBasic { it.disable() }
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // 공개 경로 - 인증 불필요
                    .requestMatchers(
                        "/h2-console/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/actuator/**",
                        "/api/v1/users/check-nickname",
                        "/api/v1/webhooks/**",
                        "/api/v1/dummy/**",
                        "/api/v2/events/count",
                        "/api/v1/events/migrate-status",
                        "/api/v1/scheduler/**",
                        "/api/v1/views/**",
                        "/actuator/**",
                        "/uploads/**",
                    ).permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/social",
                        "/api/v1/auth/token",
                        "/api/v1/auth/id-token",
                        "/api/v1/events",
                        "/api/v1/admin/auth/login",
                        "/api/v1/alarms/test"
                    ).permitAll()
                    // 회사 북마크 목록은 인증 필요 — `{companyId}` 와일드카드보다 먼저 평가되어야 함
                    .requestMatchers(HttpMethod.GET, "/api/v1/companies/bookmarked")
                    .authenticated()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/events",
                        "/",
                        "/api/v2/events",
                        "/api/v2/events/{eventId}",
                        "/api/v1/hosts",
                        "/api/v1/job-postings",
                        "/api/v1/job-postings/{jobPostingId}",
                        "/api/v1/companies/{companyId}",
                    ).permitAll()

                    // CORS preflight 요청 (OPTIONS) 허용
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // 나머지 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() } // h2
            }
            // JWT 인증 실패 시 처리
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }
            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:*",
            "https://duit-admin.vercel.app",
            "https://klaus9267.duckdns.org",
            "https://api.dutyit.net",
            "https://dutyit.net",
            "https://www.dutyit.net",
            "https://dutyit.apps.tossmini.com",
            "https://dutyit.private-apps.tossmini.com",
            "https://dutyit.web.tossmini.com",
            "https://dutyit.private-web.tossmini.com",
            "https://*.ba3a06dea2.us-east-1-01.gitpot.dev"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.exposedHeaders = listOf("X-Trace-Id")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
