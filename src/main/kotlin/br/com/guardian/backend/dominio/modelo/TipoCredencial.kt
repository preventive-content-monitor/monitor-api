package br.com.guardian.backend.dominio.modelo

import java.time.Duration

enum class TipoCredencial(val escopo: String, val duracao: Duration) {
    RESPONSAVEL("responsavel", Duration.ofHours(24)),
    CONFIGURACAO("configuracao", Duration.ofMinutes(30)),
    VINCULACAO("vinculacao", Duration.ofMinutes(10)),
    DISPOSITIVO("dispositivo", Duration.ofDays(365))
}
