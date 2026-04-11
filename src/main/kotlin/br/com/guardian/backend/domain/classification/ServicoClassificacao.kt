package br.com.guardian.backend.domain.classification

import br.com.guardian.backend.domain.events.Evento
import org.springframework.stereotype.Service

@Service
class ServicoClassificacao(
    private val clienteClassificador: ClienteClassificador,
    private val classificacaoRepositorio: ClassificacaoRepositorio
) {

    fun classificar(evento: Evento): ResultadoClassificacao {

        val (rotulo, risco) =
            clienteClassificador.classificar(evento.titulo, evento.urlHost)

        val resultado = ResultadoClassificacao(
            evento = evento,
            modelo = "mock-v1",
            rotulo = rotulo,
            pontuacaoRisco = risco,
            justificativa = "Classificação automática baseada em heurística inicial"
        )

        return classificacaoRepositorio.save(resultado)
    }
}