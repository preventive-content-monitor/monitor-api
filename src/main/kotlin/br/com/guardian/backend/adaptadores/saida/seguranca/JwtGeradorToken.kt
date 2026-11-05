package br.com.guardian.backend.adaptadores.saida.seguranca

import br.com.guardian.backend.dominio.modelo.UsuarioGuardian
import br.com.guardian.backend.dominio.modelo.SessaoCredencial
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import br.com.guardian.backend.dominio.porta.GeradorToken
import br.com.guardian.backend.adaptadores.saida.persistencia.SessaoCredencialRepositorio
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Component
class JwtGeradorToken(
    @Value("\${guardian.security.jwt.secret}")
    private val jwtSegredo: String,
    private val sessaoRepositorio: SessaoCredencialRepositorio
) : GeradorToken {

    @Transactional
    override fun gerarToken(usuario: UsuarioGuardian, tipo: TipoCredencial): String {
        require(tipo != TipoCredencial.DISPOSITIVO)
        return emitir(usuario.id, tipo, usuario.papel.name)
    }

    @Transactional
    override fun gerarTokenDispositivo(dispositivoId: UUID): String =
        emitir(dispositivoId, TipoCredencial.DISPOSITIVO, null)

    @Transactional
    override fun revogarToken(sessaoId: UUID) {
        val sessao = sessaoRepositorio.findById(sessaoId).orElse(null) ?: return
        if (sessao.revogadaEm == null) {
            sessao.revogadaEm = Instant.now()
            sessaoRepositorio.save(sessao)
        }
    }

    private fun emitir(sujeitoId: UUID, tipo: TipoCredencial, papel: String?): String {
        val agora = Instant.now()
        val expiraEm = agora.plus(tipo.duracao)
        val sessaoId = UUID.randomUUID()
        sessaoRepositorio.save(SessaoCredencial(sessaoId, sujeitoId, tipo, expiraEm))
        val chave = SecretKeySpec(jwtSegredo.toByteArray(), SignatureAlgorithm.HS256.jcaName)
        val builder = Jwts.builder()
            .setId(sessaoId.toString())
            .setSubject(sujeitoId.toString())
            .claim("scope", tipo.escopo)
            .setIssuedAt(Date.from(agora))
            .setExpiration(Date.from(expiraEm))
        if (papel != null) builder.claim("role", papel)
        return builder
            .signWith(chave, SignatureAlgorithm.HS256)
            .compact()
    }
}
