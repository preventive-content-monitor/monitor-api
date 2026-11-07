package br.com.guardian.backend.dominio.modelo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@JsonIgnoreProperties("hibernateLazyInitializer", "handler")
@Entity
@Table(name = "dispositivos")
class Dispositivo(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependente_id", nullable = false)
    val dependente: Dependente,

    @Column(name = "nome_dispositivo", nullable = false)
    val nomeDispositivo: String,

    @Column(name = "vinculado_em", nullable = false)
    var vinculadoEm: Instant = Instant.now(),

    @Column(name = "ultimo_acesso_em")
    var ultimoAcessoEm: Instant? = null,

    /** Último "sinal de vida" enviado pela extensão. */
    @Column(name = "ultimo_sinal_em")
    var ultimoSinalEm: Instant? = null,

    /** Se a extensão também está ativa nas janelas anônimas. */
    @Column(name = "anonimo_permitido")
    var anonimoPermitido: Boolean? = null,

    @Column(name = "versao_extensao", length = 20)
    var versaoExtensao: String? = null,

    /** Quando a extensão foi removida do navegador (detectado pela URL de desinstalação). */
    @Column(name = "removido_em")
    var removidoEm: Instant? = null,

    /** Identificador do computador/perfil do Chrome, mantido mesmo se a extensão for reinstalada. */
    @Column(name = "computador_id", length = 64)
    var computadorId: String? = null
)