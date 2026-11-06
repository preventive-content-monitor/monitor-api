package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.SessaoCredencialRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.UsuarioRepositorio
import br.com.guardian.backend.dominio.excecao.ConflitoExcecao
import br.com.guardian.backend.dominio.excecao.DispositivoNaoEncontradoExcecao
import br.com.guardian.backend.dominio.excecao.RequisicaoInvalidaExcecao
import br.com.guardian.backend.dominio.excecao.UsuarioNaoEncontradoExcecao
import br.com.guardian.backend.dominio.modelo.Dispositivo
import br.com.guardian.backend.dominio.modelo.SeveridadeAlerta
import br.com.guardian.backend.dominio.modelo.TipoAlerta
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import br.com.guardian.backend.dominio.modelo.UsuarioGuardian
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.text.Normalizer
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Proteção contra burla da extensão: palavra-chave do responsável,
 * sinal de vida (heartbeat) e detecção de remoção.
 */
@Service
class ServicoProtecao(
    private val usuarioRepositorio: UsuarioRepositorio,
    private val dispositivoRepositorio: DispositivoRepositorio,
    private val sessoes: SessaoCredencialRepositorio,
    private val codificador: PasswordEncoder,
    private val servicoAlerta: ServicoAlerta,
    @Value("\${guardian.security.jwt.secret}") private val segredo: String
) {
    companion object {
        const val MAX_TENTATIVAS = 5
        val TEMPO_BLOQUEIO: Duration = Duration.ofMinutes(5)
        const val ALERTA_APOS_ERROS = 3
    }

    private data class Tentativas(var erros: Int = 0, var bloqueadoAte: Instant? = null)

    private val tentativas = ConcurrentHashMap<UUID, Tentativas>()

    data class ResultadoVerificacao(
        val valida: Boolean,
        val tentativasRestantes: Int,
        val bloqueadoAte: Instant?
    )

    // ================= Palavra-chave =================

    fun normalizar(palavra: String): String {
        val semAcento = Normalizer.normalize(palavra, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        return semAcento.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    @Transactional(readOnly = true)
    fun palavraChaveDefinida(usuarioId: UUID): Boolean = buscarUsuario(usuarioId).palavraChaveHash != null

    @Transactional
    fun definirPalavraChave(usuarioId: UUID, palavra: String, podeSubstituir: Boolean) {
        val usuario = buscarUsuario(usuarioId)
        if (usuario.palavraChaveHash != null && !podeSubstituir)
            throw ConflitoExcecao("A palavra-chave já foi definida. Altere-a pelo painel do responsável.")
        val normalizada = normalizar(palavra)
        if (normalizada.length < 4)
            throw RequisicaoInvalidaExcecao("A palavra-chave precisa ter pelo menos 4 letras.")
        if (normalizada.length > 60)
            throw RequisicaoInvalidaExcecao("A palavra-chave pode ter no máximo 60 caracteres.")
        usuario.palavraChaveHash = codificador.encode(normalizada)
        usuarioRepositorio.save(usuario)
        tentativas.remove(usuarioId)
    }

    @Transactional
    fun verificar(dispositivoId: UUID, palavra: String): ResultadoVerificacao {
        val dispositivo = buscarDispositivo(dispositivoId)
        val usuario = dispositivo.dependente.usuarioGuardian
        val hash = usuario.palavraChaveHash
            ?: return ResultadoVerificacao(false, 0, null)

        val estado = tentativas.computeIfAbsent(usuario.id) { Tentativas() }
        synchronized(estado) {
            val agora = Instant.now()
            estado.bloqueadoAte?.let { if (it.isAfter(agora)) return ResultadoVerificacao(false, 0, it) }
            if (estado.bloqueadoAte != null) { estado.bloqueadoAte = null; estado.erros = 0 }

            if (codificador.matches(normalizar(palavra), hash)) {
                tentativas.remove(usuario.id)
                return ResultadoVerificacao(true, MAX_TENTATIVAS, null)
            }

            estado.erros++
            if (estado.erros == ALERTA_APOS_ERROS || estado.erros >= MAX_TENTATIVAS) {
                servicoAlerta.registrar(
                    usuario = usuario,
                    dependente = dispositivo.dependente,
                    tipo = TipoAlerta.PROTECAO,
                    severidade = SeveridadeAlerta.ATENCAO,
                    titulo = "Palavra-chave errada no computador de ${dispositivo.dependente.apelido}",
                    mensagem = "Alguém errou a palavra-chave ${estado.erros} vezes tentando abrir as configurações do GuardIAn. " +
                        "Se não foi você, converse com ${dispositivo.dependente.apelido}.",
                    referencia = "palavra-chave:${dispositivo.id}:${estado.erros}"
                )
            }
            if (estado.erros >= MAX_TENTATIVAS) {
                estado.bloqueadoAte = agora.plus(TEMPO_BLOQUEIO)
                return ResultadoVerificacao(false, 0, estado.bloqueadoAte)
            }
            return ResultadoVerificacao(false, MAX_TENTATIVAS - estado.erros, null)
        }
    }

    /** Desconecta a extensão somente após a palavra-chave correta. */
    @Transactional
    fun desconectar(dispositivoId: UUID, palavra: String): ResultadoVerificacao {
        val resultado = verificar(dispositivoId, palavra)
        if (!resultado.valida) return resultado
        val dispositivo = buscarDispositivo(dispositivoId)
        sessoes.revogarSessoes(dispositivoId, TipoCredencial.DISPOSITIVO, Instant.now())
        servicoAlerta.registrar(
            usuario = dispositivo.dependente.usuarioGuardian,
            dependente = dispositivo.dependente,
            tipo = TipoAlerta.PROTECAO,
            severidade = SeveridadeAlerta.INFO,
            titulo = "Proteção desligada com a palavra-chave",
            mensagem = "O GuardIAn foi desconectado de \"${dispositivo.nomeDispositivo}\" usando a palavra-chave correta.",
            referencia = "desconectado:${dispositivo.id}"
        )
        return resultado
    }

    // ================= Sinal de vida e remoção =================

    @Transactional
    fun registrarSinal(dispositivoId: UUID, versao: String?, anonimoPermitido: Boolean?): String {
        val dispositivo = buscarDispositivo(dispositivoId)
        val anterior = dispositivo.anonimoPermitido
        val agora = Instant.now()
        dispositivo.ultimoSinalEm = agora
        dispositivo.ultimoAcessoEm = agora
        dispositivo.versaoExtensao = versao?.take(20)
        dispositivo.removidoEm = null
        if (anonimoPermitido != null) dispositivo.anonimoPermitido = anonimoPermitido
        dispositivoRepositorio.save(dispositivo)

        if (anonimoPermitido == false && anterior != false) {
            servicoAlerta.registrar(
                usuario = dispositivo.dependente.usuarioGuardian,
                dependente = dispositivo.dependente,
                tipo = TipoAlerta.PROTECAO,
                severidade = SeveridadeAlerta.ATENCAO,
                titulo = "Guia anônima sem proteção no computador de ${dispositivo.dependente.apelido}",
                mensagem = "A guia anônima do Chrome não está protegida. Ative \"Permitir no modo anônimo\" na extensão " +
                    "ou aplique a Blindagem do Windows pelo painel.",
                referencia = "anonimo:${dispositivo.id}"
            )
        }
        return tokenRemocao(dispositivoId)
    }

    @Transactional
    fun registrarRemocao(dispositivoId: UUID, token: String): Boolean {
        val esperado = tokenRemocao(dispositivoId)
        if (!MessageDigest.isEqual(esperado.toByteArray(), token.toByteArray())) return false
        val dispositivo = dispositivoRepositorio.findById(dispositivoId).orElse(null) ?: return false
        if (dispositivo.removidoEm != null) return true
        dispositivo.removidoEm = Instant.now()
        dispositivoRepositorio.save(dispositivo)
        sessoes.revogarSessoes(dispositivoId, TipoCredencial.DISPOSITIVO, Instant.now())
        // Após a maioridade, remover a extensão é esperado e não deve alarmar o responsável.
        if (dispositivo.dependente.monitoramentoEncerrado) return true
        servicoAlerta.registrar(
            usuario = dispositivo.dependente.usuarioGuardian,
            dependente = dispositivo.dependente,
            tipo = TipoAlerta.PROTECAO,
            severidade = SeveridadeAlerta.CRITICO,
            titulo = "GuardIAn foi removido do computador de ${dispositivo.dependente.apelido}",
            mensagem = "A extensão GuardIAn foi removida do navegador \"${dispositivo.nomeDispositivo}\". " +
                "Este navegador está sem proteção. Reinstale a extensão e converse com ${dispositivo.dependente.apelido}.",
            referencia = "removido:${dispositivo.id}"
        )
        return true
    }

    fun tokenRemocao(dispositivoId: UUID): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(segredo.toByteArray(), "HmacSHA256"))
        val assinatura = mac.doFinal("remocao:$dispositivoId".toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura).take(32)
    }

    // ================= Utilitários =================

    fun usuarioDoDispositivo(dispositivoId: UUID): UUID = buscarDispositivo(dispositivoId).dependente.usuarioGuardian.id

    private fun buscarUsuario(id: UUID): UsuarioGuardian =
        usuarioRepositorio.findById(id).orElseThrow { UsuarioNaoEncontradoExcecao() }

    private fun buscarDispositivo(id: UUID): Dispositivo =
        dispositivoRepositorio.findById(id).orElseThrow { DispositivoNaoEncontradoExcecao() }
}
