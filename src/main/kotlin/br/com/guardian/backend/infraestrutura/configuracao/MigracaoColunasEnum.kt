package br.com.guardian.backend.infraestrutura.configuracao

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * O Hibernate cria colunas de enum como ENUM nativo do MySQL e o ddl-auto=update
 * não as altera depois. Converte alertas.tipo para VARCHAR para aceitar novos tipos.
 */
@Component
class MigracaoColunasEnum(private val jdbc: JdbcTemplate) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(MigracaoColunasEnum::class.java)

    override fun run(args: ApplicationArguments) {
        try {
            val tipo = jdbc.queryForList(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'alertas' AND COLUMN_NAME = 'tipo'",
                String::class.java
            ).firstOrNull()
            if (tipo.equals("enum", ignoreCase = true)) {
                jdbc.execute("ALTER TABLE alertas MODIFY tipo VARCHAR(40) NOT NULL")
                logger.info("[Migracao] alertas.tipo convertido para VARCHAR(40)")
            }
        } catch (e: Exception) {
            logger.warn("[Migracao] Nao foi possivel converter alertas.tipo: {}", e.message)
        }
    }
}
