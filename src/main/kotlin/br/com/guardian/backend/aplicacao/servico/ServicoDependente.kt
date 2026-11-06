package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.persistencia.AlertaRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.ClassificacaoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.DependenteRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.EventoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.PoliticaRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.SessaoCredencialRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.UsuarioRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.VulnerabilidadeRepositorio
import br.com.guardian.backend.dominio.excecao.DependenteNaoEncontradoExcecao
import br.com.guardian.backend.dominio.excecao.RequisicaoInvalidaExcecao
import br.com.guardian.backend.dominio.excecao.UsuarioNaoEncontradoExcecao
import br.com.guardian.backend.dominio.modelo.Dependente
import br.com.guardian.backend.dominio.modelo.SexoDependente
import br.com.guardian.backend.dominio.modelo.SeveridadeAlerta
import br.com.guardian.backend.dominio.modelo.TipoAlerta
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Cadastro, edição e exclusão de dependentes, e o ciclo de maioridade:
 * aviso prévio, encerramento do monitoramento aos 18 anos e exclusão dos dados
 * após o período de retenção.
 */
@Service
class ServicoDependente(
    private val dependentes: DependenteRepositorio,
    private val usuarios: UsuarioRepositorio,
    private val dispositivos: DispositivoRepositorio,
    private val sessoes: SessaoCredencialRepositorio,
    private val classificacoes: ClassificacaoRepositorio,
    private val eventos: EventoRepositorio,
    private val alertas: AlertaRepositorio,
    private val politicas: PoliticaRepositorio,
    private val vulnerabilidades: VulnerabilidadeRepositorio,
    private val servicoAlerta: ServicoAlerta
) {
    private val logger = LoggerFactory.getLogger(ServicoDependente::class.java)
    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // ================= CRUD =================

    @Transactional
    fun criar(usuarioId: UUID, apelido: String, dataNascimento: LocalDate, sexo: SexoDependente): Dependente {
        val usuario = usuarios.findById(usuarioId).orElseThrow { UsuarioNaoEncontradoExcecao() }
        val nome = validarApelido(apelido)
        validarNascimento(dataNascimento)
        return dependentes.save(
            Dependente(
                usuarioGuardian = usuario,
                apelido = nome,
                anoNascimento = dataNascimento.year,
                dataNascimento = dataNascimento,
                sexo = sexo
            )
        )
    }

    @Transactional
    fun atualizar(usuarioId: UUID, id: UUID, apelido: String, dataNascimento: LocalDate, sexo: SexoDependente): Dependente {
        val dependente = buscarDoUsuario(usuarioId, id)
        dependente.apelido = validarApelido(apelido)
        if (dependente.monitoramentoEncerrado) {
            if (dataNascimento != dependente.dataNascimento) {
                throw RequisicaoInvalidaExcecao("O monitoramento de ${dependente.apelido} já foi encerrado por maioridade; a data de nascimento não pode ser alterada.")
            }
        } else {
            validarNascimento(dataNascimento)
            dependente.dataNascimento = dataNascimento
            dependente.anoNascimento = dataNascimento.year
            dependente.avisoMaioridadeEm = null
        }
        dependente.sexo = sexo
        return dependentes.save(dependente)
    }

    @Transactional
    fun excluir(usuarioId: UUID, id: UUID) {
        excluirDados(buscarDoUsuario(usuarioId, id))
    }

    @Transactional(readOnly = true)
    fun buscarDoUsuario(usuarioId: UUID, id: UUID): Dependente {
        val dependente = dependentes.findById(id).orElseThrow { DependenteNaoEncontradoExcecao() }
        if (dependente.usuarioGuardian.id != usuarioId) throw DependenteNaoEncontradoExcecao()
        return dependente
    }

    private fun validarApelido(apelido: String): String {
        val nome = apelido.trim()
        if (nome.isEmpty()) throw RequisicaoInvalidaExcecao("Informe o nome ou apelido do dependente.")
        if (nome.length > 60) throw RequisicaoInvalidaExcecao("O nome do dependente pode ter no máximo 60 caracteres.")
        return nome
    }

    private fun validarNascimento(dataNascimento: LocalDate) {
        val hoje = LocalDate.now(Dependente.FUSO)
        if (dataNascimento.isAfter(hoje)) throw RequisicaoInvalidaExcecao("A data de nascimento não pode estar no futuro.")
        if (Dependente.idadeEm(dataNascimento, hoje) >= Dependente.IDADE_LIMITE) {
            throw RequisicaoInvalidaExcecao("O GuardIAn protege apenas menores de ${Dependente.IDADE_LIMITE} anos.")
        }
    }

    /** Apaga o dependente e tudo o que depende dele, respeitando as chaves estrangeiras. */
    private fun excluirDados(dependente: Dependente) {
        val id = dependente.id
        val idsDispositivos = dispositivos.findAllByDependenteId(id).map { it.id }
        if (idsDispositivos.isNotEmpty()) sessoes.excluirPorSujeitos(idsDispositivos)
        classificacoes.excluirPorDependente(id)
        eventos.excluirPorDependente(id)
        alertas.excluirPorDependente(id)
        politicas.excluirPorDependente(id)
        vulnerabilidades.excluirPorDependente(id)
        dispositivos.excluirPorDependente(id)
        dependentes.delete(dependente)
        logger.info("[Dependente] {} excluído com {} dispositivo(s)", id, idsDispositivos.size)
    }

    // ================= Maioridade =================

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun verificarAoIniciar() = processarMaioridade()

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Sao_Paulo")
    @Transactional
    fun processarMaioridade() {
        val agora = Instant.now()
        dependentes.findAll().forEach { dependente ->
            try {
                when {
                    dependente.monitoramentoEncerrado ->
                        if (dependente.exclusaoAgendadaEm!!.isBefore(agora)) excluirDados(dependente)
                    dependente.idade >= Dependente.IDADE_LIMITE -> encerrarMonitoramento(dependente, agora)
                    dependente.diasParaMaioridade <= Dependente.DIAS_AVISO && dependente.avisoMaioridadeEm == null ->
                        avisarMaioridadeProxima(dependente, agora)
                }
            } catch (e: Exception) {
                logger.error("[Maioridade] Falha ao processar dependente {}: {}", dependente.id, e.message)
            }
        }
    }

    private fun avisarMaioridadeProxima(dependente: Dependente, agora: Instant) {
        dependente.avisoMaioridadeEm = agora
        dependentes.save(dependente)
        servicoAlerta.registrar(
            usuario = dependente.usuarioGuardian,
            dependente = dependente,
            tipo = TipoAlerta.MAIORIDADE,
            severidade = SeveridadeAlerta.INFO,
            titulo = "${dependente.apelido} faz ${Dependente.IDADE_LIMITE} anos em ${dependente.diasParaMaioridade} dia(s)",
            mensagem = "Em ${dependente.maioridadeEm.format(formatoData)} ${dependente.apelido} deixa de ser menor de idade " +
                "e o monitoramento será encerrado automaticamente. É um bom momento para conversar sobre autonomia e segurança on-line.",
            referencia = "maioridade-aviso:${dependente.id}"
        )
    }

    private fun encerrarMonitoramento(dependente: Dependente, agora: Instant) {
        dispositivos.findAllByDependenteId(dependente.id).forEach {
            sessoes.revogarSessoes(it.id, TipoCredencial.DISPOSITIVO, agora)
        }
        dependente.monitoramentoEncerradoEm = agora
        dependentes.save(dependente)
        servicoAlerta.registrar(
            usuario = dependente.usuarioGuardian,
            dependente = dependente,
            tipo = TipoAlerta.MAIORIDADE,
            severidade = SeveridadeAlerta.INFO,
            titulo = "${dependente.apelido} completou ${Dependente.IDADE_LIMITE} anos — monitoramento encerrado",
            mensagem = "Os computadores de ${dependente.apelido} foram desconectados e nenhum novo acesso será registrado. " +
                "O histórico fica disponível até ${dependente.exclusaoAgendadaEm!!.atZone(Dependente.FUSO).format(formatoData)} " +
                "e depois é apagado automaticamente.",
            referencia = "maioridade:${dependente.id}"
        )
        logger.info("[Maioridade] Monitoramento encerrado para {}", dependente.id)
    }
}
