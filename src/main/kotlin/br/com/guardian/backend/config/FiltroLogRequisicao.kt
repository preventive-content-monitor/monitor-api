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
class FiltroLogRequisicao : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(FiltroLogRequisicao::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val uri = request.requestURI
        val metodo = request.method
        val cabecalhoAuth = request.getHeader("Authorization")

        log.info(">>> REQUISIÇÃO: {} {} | Cabeçalho Auth presente: {}", metodo, uri, cabecalhoAuth != null)

        filterChain.doFilter(request, response)

        log.info("<<< RESPOSTA: {} {} | Status: {}", metodo, uri, response.status)
    }
}
