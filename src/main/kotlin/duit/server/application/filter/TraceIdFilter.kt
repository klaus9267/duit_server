package duit.server.application.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

/**
 * 요청마다 추적 ID를 생성하고 MDC에 설정하는 필터
 */
@Component
@Order(1)
class TraceIdFilter : OncePerRequestFilter() {
    
    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_ID_MDC_KEY = "traceId"
    }
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // 요청 헤더에서 추적 ID를 가져오거나 새로 생성
            val traceId = request.getHeader(TRACE_ID_HEADER) ?: generateTraceId()
            
            // MDC에 추적 ID 설정
            MDC.put(TRACE_ID_MDC_KEY, traceId)
            
            // 응답 헤더에 추적 ID 추가
            response.setHeader(TRACE_ID_HEADER, traceId)
            
            // 다음 필터 체인 실행
            filterChain.doFilter(request, response)
            
        } finally {
            // MDC 정리
            MDC.clear()
        }
    }
    
    /**
     * 추적 ID 생성 (16자리 랜덤 문자열)
     */
    private fun generateTraceId(): String {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }
}
