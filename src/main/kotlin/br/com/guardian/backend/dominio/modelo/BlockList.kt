package br.com.guardian.backend.dominio.modelo

import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * Tabela neutra de classificações conhecidas — fonte de verdade para bloqueios/liberações.
 * Não tem FK para outras tabelas. Alimentada pelo pipeline de classificação.
 * Exportada periodicamente para S3 (para consumo offline da extensão).
 */
@Entity
@Table(
    name = "block_list",
    indexes = [
        Index(name = "idx_block_list_url_conteudo", columnList = "url_conteudo"),
        Index(name = "idx_block_list_rotulo", columnList = "rotulo"),
        Index(name = "idx_block_list_pontuacao", columnList = "pontuacao_risco")
    ]
)
class BlockList(

    @Id
    val id: UUID = UUID.randomUUID(),

    /**
     * Chave de conteúdo granular:
     * - Sites normais: igual ao domínio (ex: "pornhub.com")
     * - Plataformas mistas: URL do conteúdo (ex: "youtube.com/watch?v=VIDEO_ID")
     */
    @Column(name = "url_conteudo", length = 500, nullable = false, unique = true)
    val urlConteudo: String,

    @Column(name = "rotulo", nullable = false, length = 50)
    var rotulo: String,

    @Column(name = "pontuacao_risco", nullable = false)
    var pontuacaoRisco: Int,

    @Column(name = "modelo", length = 100)
    var modelo: String? = null,

    @Column(name = "atualizado_em", nullable = false)
    var atualizadoEm: Instant = Instant.now(),

    @Column(name = "criado_em", nullable = false)
    val criadoEm: Instant = Instant.now()
)
