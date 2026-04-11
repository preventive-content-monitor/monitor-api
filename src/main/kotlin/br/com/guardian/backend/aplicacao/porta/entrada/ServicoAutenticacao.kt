package br.com.guardian.backend.aplicacao.porta.entrada

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaAutenticacao
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoLogin
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoRegistro

interface ServicoAutenticacao {
    fun registrar(requisicao: RequisicaoRegistro)
    fun autenticar(requisicao: RequisicaoLogin): RespostaAutenticacao
}
