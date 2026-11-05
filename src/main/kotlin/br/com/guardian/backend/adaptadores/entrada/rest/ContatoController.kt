package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.adaptadores.saida.email.ClienteEmail
import br.com.guardian.backend.adaptadores.saida.email.ModelosEmail
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

data class RequisicaoContato(
    val nome: String? = null,
    val email: String? = null,
    val mensagem: String? = null,
    // Campo invisível para pessoas; robôs que o preenchem são descartados.
    val site: String? = null
)

@RestController
@RequestMapping("/api/contato")
@Tag(name = "Contato", description = "Formulário público \"Entre em contato\" da landing page")
class ContatoController(
    private val clienteEmail: ClienteEmail,
    private val modelosEmail: ModelosEmail,
    @Value("\${guardian.contato.destino:}") private val destino: String
) {
    private val logger = LoggerFactory.getLogger(ContatoController::class.java)
    private val enviosPorIp = ConcurrentHashMap<String, ConcurrentLinkedDeque<Instant>>()
    private val enviosGlobais = ConcurrentLinkedDeque<Instant>()

    companion object {
        private val JANELA: Duration = Duration.ofHours(1)
        private const val LIMITE_POR_IP = 5
        private const val LIMITE_GLOBAL = 60
        private val EMAIL_VALIDO = Regex("^[^\\s@<>\"]+@[^\\s@<>\"]+\\.[^\\s@<>\"]{2,}$")
    }

    @PostMapping
    @Operation(summary = "Enviar mensagem de contato", description = "Encaminha a mensagem da landing para o email da equipe.")
    fun enviar(@RequestBody req: RequisicaoContato, http: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        if (!req.site.isNullOrBlank()) {
            return ResponseEntity.ok(mapOf("enviado" to true))
        }

        val nome = req.nome?.trim().orEmpty().replace(Regex("[\\r\\n]+"), " ")
        val email = req.email?.trim().orEmpty()
        val mensagem = req.mensagem?.trim().orEmpty()

        if (nome.length !in 2..120 || email.length > 254 || !EMAIL_VALIDO.matches(email) || mensagem.length !in 5..4000) {
            return ResponseEntity.badRequest().body(
                mapOf("enviado" to false, "mensagem" to "Preencha nome, um e-mail válido e uma mensagem (até 4000 caracteres).")
            )
        }

        if (destino.isBlank() || !clienteEmail.habilitado) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf("enviado" to false, "mensagem" to "O envio de mensagens está indisponível no momento. Escreva para nós por e-mail.")
            )
        }

        if (!registrarEnvio(ipCliente(http))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                mapOf("enviado" to false, "mensagem" to "Muitas mensagens em pouco tempo. Tente novamente mais tarde.")
            )
        }

        val enviado = clienteEmail.enviarHtml(
            destinatario = destino,
            assunto = "[Guardian] Contato pelo site — ${nome.take(60)}",
            corpoHtml = modelosEmail.contatoLanding(nome, email, mensagem),
            responderPara = email
        )

        return if (enviado) {
            logger.info("[Contato] Mensagem encaminhada para a equipe")
            ResponseEntity.ok(mapOf("enviado" to true))
        } else {
            ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                mapOf("enviado" to false, "mensagem" to "Não foi possível enviar agora. Tente novamente em alguns minutos.")
            )
        }
    }

    private fun ipCliente(http: HttpServletRequest): String =
        http.getHeader("X-Real-IP")?.takeIf { it.isNotBlank() } ?: http.remoteAddr ?: "desconhecido"

    @Synchronized
    private fun registrarEnvio(ip: String): Boolean {
        val agora = Instant.now()
        val limite = agora.minus(JANELA)

        while (enviosGlobais.peekFirst()?.isBefore(limite) == true) enviosGlobais.pollFirst()
        enviosPorIp.entries.removeIf { (_, fila) ->
            while (fila.peekFirst()?.isBefore(limite) == true) fila.pollFirst()
            fila.isEmpty()
        }

        val doIp = enviosPorIp.computeIfAbsent(ip) { ConcurrentLinkedDeque() }
        if (doIp.size >= LIMITE_POR_IP || enviosGlobais.size >= LIMITE_GLOBAL) return false

        doIp.addLast(agora)
        enviosGlobais.addLast(agora)
        return true
    }
}
