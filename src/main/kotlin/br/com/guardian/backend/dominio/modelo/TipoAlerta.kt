package br.com.guardian.backend.dominio.modelo

/**
 * Categoria do alerta. Cada tipo é governado por uma preferência do responsável
 * (ver [PreferenciaNotificacao]) que decide se o email correspondente é enviado.
 */
enum class TipoAlerta {
    /** Dependente tentou acessar conteúdo já bloqueado pela política. */
    TENTATIVA_BLOQUEIO,

    /** IA classificou um acesso acima do limite de risco da política. */
    CONTEUDO_SENSIVEL,

    /** Novo dispositivo vinculado a um dependente. */
    NOVO_DISPOSITIVO,

    /** Resumo diário consolidado enviado por email. */
    RESUMO_DIARIO
}
