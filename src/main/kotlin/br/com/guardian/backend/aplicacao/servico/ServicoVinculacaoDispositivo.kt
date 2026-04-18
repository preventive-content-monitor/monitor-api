package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.persistencia.DependenteRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.dominio.excecao.DependenteNaoEncontradoExcecao
import br.com.guardian.backend.dominio.excecao.CodigoVinculacaoInvalidoExcecao
import br.com.guardian.backend.dominio.modelo.Dispositivo
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class ServicoVinculacaoDispositivo(
    private val dependenteRepositorio: DependenteRepositorio,
    private val dispositivoRepositorio: DispositivoRepositorio
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

        return dispositivoRepositorio.save(dispositivo)
    }
}