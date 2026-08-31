package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.persistencia.DependenteRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.dominio.excecao.DependenteNaoEncontradoExcecao
import br.com.guardian.backend.dominio.excecao.CodigoVinculacaoInvalidoExcecao
import br.com.guardian.backend.dominio.modelo.Dispositivo
import br.com.guardian.backend.dominio.modelo.SeveridadeAlerta
import br.com.guardian.backend.dominio.modelo.TipoAlerta
import org.springframework.stereotype.Service
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

    fun gerarCodigoVinculacao(dependenteId: UUID): String {
        val codigo = UUID.randomUUID().toString().substring(0, 6)
        codigosVinculacao[codigo] = Pair(dependenteId, Instant.now().plusSeconds(300))
        return codigo
    }

    fun vincularDispositivo(codigo: String, nomeDispositivo: String): Dispositivo {
        val entrada = codigosVinculacao[codigo]
            ?: throw CodigoVinculacaoInvalidoExcecao("Código de vinculação inválido")

        val (dependenteId, expiracao) = entrada

        if (Instant.now().isAfter(expiracao)) {
            codigosVinculacao.remove(codigo)
            throw CodigoVinculacaoInvalidoExcecao("Código de vinculação expirado")
        }

        val dependente = dependenteRepositorio.findById(dependenteId)
            .orElseThrow { DependenteNaoEncontradoExcecao() }

        val dispositivo = Dispositivo(
            dependente = dependente,
            nomeDispositivo = nomeDispositivo
        )

        codigosVinculacao.remove(codigo)

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
}