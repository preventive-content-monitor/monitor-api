package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.persistencia.DependenteRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.dominio.excecao.DependenteNaoEncontradoExcecao
import br.com.guardian.backend.dominio.excecao.CodigoVinculacaoInvalidoExcecao
import br.com.guardian.backend.dominio.modelo.Dispositivo
import br.com.guardian.backend.dominio.modelo.SeveridadeAlerta
import br.com.guardian.backend.dominio.modelo.TipoAlerta
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class ServicoVinculacaoDispositivo(
    private val dependenteRepositorio: DependenteRepositorio,
    private val dispositivoRepositorio: DispositivoRepositorio,
    private val servicoAlerta: ServicoAlerta
) {

    private val codigosVinculacao = ConcurrentHashMap<String, Pair<UUID, Instant>>()
    private val aleatorio = SecureRandom()
    private val alfabeto = "abcdefghjkmnpqrstuvwxyz23456789"

    fun gerarCodigoVinculacao(dependenteId: UUID, usuarioId: UUID): String {
        val dependente = dependenteRepositorio.findById(dependenteId)
            .orElseThrow { DependenteNaoEncontradoExcecao() }
        if (dependente.usuarioGuardian.id != usuarioId) throw DependenteNaoEncontradoExcecao()
        if (dependente.monitoramentoEncerrado) {
            throw br.com.guardian.backend.dominio.excecao.RequisicaoInvalidaExcecao(
                "${dependente.apelido} completou ${br.com.guardian.backend.dominio.modelo.Dependente.IDADE_LIMITE} anos; o monitoramento foi encerrado."
            )
        }

        val agora = Instant.now()
        codigosVinculacao.entries.removeIf { it.value.second.isBefore(agora) }
        val entrada = Pair(dependenteId, agora.plusSeconds(300))
        while (true) {
            val codigo = buildString {
                repeat(6) { append(alfabeto[aleatorio.nextInt(alfabeto.length)]) }
            }
            if (codigosVinculacao.putIfAbsent(codigo, entrada) == null) return codigo
        }
    }

    fun vincularDispositivo(
        codigo: String,
        nomeDispositivo: String,
        usuarioId: UUID,
        computadorId: String? = null
    ): Dispositivo {
        val idComputador = computadorId?.trim()?.takeIf { ID_COMPUTADOR.matches(it) }
        val entrada = codigosVinculacao[codigo]
            ?: throw CodigoVinculacaoInvalidoExcecao("Código de vinculação inválido")

        val (dependenteId, expiracao) = entrada

        if (Instant.now().isAfter(expiracao)) {
            codigosVinculacao.remove(codigo)
            throw CodigoVinculacaoInvalidoExcecao("Código de vinculação expirado")
        }

        val dependente = dependenteRepositorio.findById(dependenteId)
            .orElseThrow { DependenteNaoEncontradoExcecao() }
        if (dependente.usuarioGuardian.id != usuarioId || !codigosVinculacao.remove(codigo, entrada)) {
            throw CodigoVinculacaoInvalidoExcecao("Código de vinculação inválido")
        }

        // Reinstalar a extensão no mesmo computador reaproveita o cadastro dele. A identificação
        // é pelo computadorId (nunca pelo nome, que se repete entre PCs); sem ele, cria um novo.
        val reinstalado = idComputador?.let { id ->
            dispositivoRepositorio.findAllByDependenteId(dependenteId)
                .filter { it.computadorId == id }
                .maxByOrNull { it.vinculadoEm }
        }

        val dispositivo = reinstalado?.apply {
            vinculadoEm = Instant.now()
            removidoEm = null
            ultimoSinalEm = null
            versaoExtensao = null
            anonimoPermitido = null
        } ?: Dispositivo(
            dependente = dependente,
            nomeDispositivo = nomeDispositivo,
            computadorId = idComputador
        )

        val salvo = dispositivoRepositorio.save(dispositivo)

        // Informativo: aparece na Central de Alertas mas não dispara email,
        // já que a vinculação foi iniciada pelo próprio responsável.
        servicoAlerta.registrar(
            usuario    = dependente.usuarioGuardian,
            dependente = dependente,
            tipo       = TipoAlerta.NOVO_DISPOSITIVO,
            severidade = SeveridadeAlerta.INFO,
            titulo     = "Novo dispositivo conectado",
            mensagem   = "$nomeDispositivo foi vinculado a ${dependente.apelido}.",
            referencia = salvo.id.toString()
        )

        return salvo
    }

    private companion object {
        val ID_COMPUTADOR = Regex("^[A-Za-z0-9-]{16,64}$")
    }
}