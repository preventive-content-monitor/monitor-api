package br.com.guardian.backend.domain.policy

import java.util.*

interface ServicoPolitica {
    fun criarPoliticaPadraoSeNaoExiste(dependenteId: UUID)
    fun buscarPoliticaPorDispositivo(dispositivoId: UUID): Politica
    fun atualizarPolitica(dispositivoId: UUID, modo: ModoPolitica, limiteRisco: Int, dominiosBloqueados: List<String>, dominiosPermitidos: List<String>, modoEscolaAtivo: Boolean, escolaInicio: String?, escolaFim: String?): Politica
    fun deveBloquear(dominio: String, pontuacaoRisco: Int, dispositivoId: UUID): Boolean
}
