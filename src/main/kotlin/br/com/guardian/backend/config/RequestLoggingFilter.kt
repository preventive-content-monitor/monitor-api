package br.com.guardian.backend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val uri = request.requestURI
        val method = request.method
        val authHeader = request.getHeader("Authorization")
        
        log.info(">>> REQUEST: {} {} | Auth header present: {}", method, uri, authHeader != null)
        
        filterChain.doFilter(request, response)
        
        log.info("<<< RESPONSE: {} {} | Status: {}", method, uri, response.status)
    }
}
