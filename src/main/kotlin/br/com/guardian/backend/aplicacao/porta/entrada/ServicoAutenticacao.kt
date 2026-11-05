package br.com.guardian.backend.aplicacao.porta.entrada

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaAutenticacao
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoLogin
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoRegistro
import br.com.guardian.backend.dominio.modelo.TipoCredencial

interface ServicoAutenticacao {
    fun registrar(requisicao: RequisicaoRegistro)
    fun autenticar(
        requisicao: RequisicaoLogin,
        tipo: TipoCredencial = TipoCredencial.RESPONSAVEL
    ): RespostaAutenticacao
}
