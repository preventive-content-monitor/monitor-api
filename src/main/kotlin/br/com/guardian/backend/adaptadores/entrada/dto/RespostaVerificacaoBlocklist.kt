package br.com.guardian.backend.adaptadores.entrada.dto

/**
 * Resposta do endpoint GET /api/blocklist/verificar
 *
 * [acao]:
 *   - ALLOW   → conteúdo seguro, liberar acesso
 *   - BLOCK   → bloquear (politica.modo = BLOCK)
 *   - WARN    → mostrar aviso (politica.modo = WARN)
 *   - EDUCATE → mostrar conteúdo educativo (politica.modo = EDUCATE)
 *   - UNKNOWN → ainda não analisado pela IA
 */
data class RespostaVerificacaoBlocklist(
    val acao: String,
    val motivo: String,
    val pontuacaoRisco: Int? = null,
    val rotulo: String? = null
)
