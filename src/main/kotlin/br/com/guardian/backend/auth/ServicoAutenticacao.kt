package br.com.guardian.backend.auth

import br.com.guardian.backend.api.dto.RespostaAutenticacao
import br.com.guardian.backend.api.dto.RequisicaoLogin
import br.com.guardian.backend.api.dto.RequisicaoRegistro

interface ServicoAutenticacao {
    fun registrar(requisicao: RequisicaoRegistro)
    fun autenticar(requisicao: RequisicaoLogin): RespostaAutenticacao
}
