package duit.server.application.security

import com.fasterxml.jackson.databind.ObjectMapper
import duit.server.application.common.ErrorCode
import duit.server.application.controller.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * JWT 인증 실패 시 처리하는 EntryPoint
 */
@Component
class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {
    
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val errorResponse = ErrorResponse(
            code = ErrorCode.UNAUTHORIZED.code,
            message = ErrorCode.UNAUTHORIZED.message,
            fieldErrors = emptyList(),
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = generateTraceId()
        )
        
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.characterEncoding = "UTF-8"
        
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
    
    private fun generateTraceId(): String {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8)
    }
}
