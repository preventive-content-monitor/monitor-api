package br.com.guardian.backend.domain.policy

import br.com.guardian.backend.domain.dependent.DependenteRepositorio
import br.com.guardian.backend.domain.device.DispositivoRepositorio
import br.com.guardian.backend.exception.DependenteNaoEncontradoExcecao
import br.com.guardian.backend.exception.DispositivoNaoEncontradoExcecao
import br.com.guardian.backend.exception.PoliticaNaoEncontradaExcecao
import org.springframework.stereotype.Service
import java.util.*

@Service
class ServicoPoliticaImpl(
    private val politicaRepositorio: PoliticaRepositorio,
    private val dependenteRepositorio: DependenteRepositorio,
    private val dispositivoRepositorio: DispositivoRepositorio,
    private val serializadorJson: SerializadorJson,
    private val estrategiaCalculoPolitica: EstrategiaCalculoPolitica,
    private val calculadorIdade: CalculadorIdade
) : ServicoPolitica {

    override fun criarPoliticaPadraoSeNaoExiste(dependenteId: UUID) {
        if (politicaRepositorio.findByDependenteId(dependenteId) != null) return

        val dependente = dependenteRepositorio.findById(dependenteId)
            .orElseThrow { DependenteNaoEncontradoExcecao() }

        val idade = calculadorIdade.calcularIdade(dependente)
        val config = estrategiaCalculoPolitica.calcularPolitica(idade)

        val politica = Politica(
            dependente = dependente,
            modo = config.modo,
            limiteRisco = config.limiteRisco,
            dominiosBloqueados = serializadorJson.serializar(emptyList<String>())
        )

        politicaRepositorio.save(politica)
    }

    override fun buscarPoliticaPorDispositivo(dispositivoId: UUID): Politica {
        val dispositivo = dispositivoRepositorio.findById(dispositivoId)
            .orElseThrow { DispositivoNaoEncontradoExcecao() }

        val dependenteId = dispositivo.dependente.id

        criarPoliticaPadraoSeNaoExiste(dependenteId)

        return politicaRepositorio.findByDependenteId(dependenteId)
            ?: throw PoliticaNaoEncontradaExcecao()
    }

    override fun atualizarPolitica(dispositivoId: UUID, modo: ModoPolitica, limiteRisco: Int, dominiosBloqueados: List<String>, dominiosPermitidos: List<String>, modoEscolaAtivo: Boolean, escolaInicio: String?, escolaFim: String?): Politica {
        val dispositivo = dispositivoRepositorio.findById(dispositivoId)
            .orElseThrow { DispositivoNaoEncontradoExcecao() }

        val dependenteId = dispositivo.dependente.id

        val politica = politicaRepositorio.findByDependenteId(dependenteId)
            ?: throw PoliticaNaoEncontradaExcecao()

        politica.modo = modo
        politica.limiteRisco = limiteRisco
        politica.dominiosBloqueados = serializadorJson.serializar(dominiosBloqueados)
        politica.dominiosPermitidos = serializadorJson.serializar(dominiosPermitidos)
        politica.modoEscolaAtivo = modoEscolaAtivo
        politica.escolaInicio = escolaInicio
        politica.escolaFim = escolaFim

        return politicaRepositorio.save(politica)
    }

    override fun deveBloquear(dominio: String, pontuacaoRisco: Int, dispositivoId: UUID): Boolean {
        val politica = buscarPoliticaPorDispositivo(dispositivoId)

        val listaBloqueados: List<String> = serializadorJson.desserializarLista(politica.dominiosBloqueados, String::class.java)

        if (listaBloqueados.contains(dominio)) return true

        return pontuacaoRisco >= politica.limiteRisco && politica.modo == ModoPolitica.BLOCK
    }
}
