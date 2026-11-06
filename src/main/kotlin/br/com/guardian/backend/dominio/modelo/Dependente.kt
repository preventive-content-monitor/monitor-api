package br.com.guardian.backend.dominio.modelo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

@JsonIgnoreProperties("hibernateLazyInitializer", "handler")
@Entity
@Table(name = "dependentes")
class Dependente(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_guardian_id", nullable = false)
    val usuarioGuardian: UsuarioGuardian,

    @Column(name = "apelido", nullable = false)
    var apelido: String,

    @Column(name = "ano_nascimento", nullable = false)
    var anoNascimento: Int,

    @Column(name = "data_nascimento")
    var dataNascimento: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", length = 10)
    var sexo: SexoDependente? = null,

    @Column(name = "criado_em", nullable = false)
    val criadoEm: Instant = Instant.now(),

    /** Quando o monitoramento foi encerrado por maioridade. */
    @Column(name = "monitoramento_encerrado_em")
    var monitoramentoEncerradoEm: Instant? = null,

    /** Quando o responsável foi avisado de que a maioridade está próxima. */
    @Column(name = "aviso_maioridade_em")
    var avisoMaioridadeEm: Instant? = null
) {
    companion object {
        /** Idade em que o dependente deixa de ser menor (ECA/LGPD art. 14). */
        const val IDADE_LIMITE = 18
        /** Dias de antecedência do aviso de maioridade. */
        const val DIAS_AVISO = 30L
        /** Dias em que o histórico fica disponível após o encerramento. */
        const val DIAS_RETENCAO = 30L
        val FUSO: ZoneId = ZoneId.of("America/Sao_Paulo")

        fun idadeEm(nascimento: LocalDate, dia: LocalDate): Int = Period.between(nascimento, dia).years
    }

    private val nascimento: LocalDate get() = dataNascimento ?: LocalDate.of(anoNascimento, 1, 1)

    val idade: Int get() = idadeEm(nascimento, LocalDate.now(FUSO))

    val maioridadeEm: LocalDate get() = nascimento.plusYears(IDADE_LIMITE.toLong())

    val diasParaMaioridade: Long get() = ChronoUnit.DAYS.between(LocalDate.now(FUSO), maioridadeEm)

    val monitoramentoEncerrado: Boolean get() = monitoramentoEncerradoEm != null

    val exclusaoAgendadaEm: Instant? get() = monitoramentoEncerradoEm?.plus(DIAS_RETENCAO, ChronoUnit.DAYS)
}