package br.com.guardian.backend.infraestrutura.configuracao

import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.SessaoCredencialRepositorio
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.config.Customizer
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import java.time.Instant
import java.util.UUID

@Configuration
@EnableWebSecurity
class ConfiguracaoSeguranca {

    private fun matcherRequisicaoPublica() = OrRequestMatcher(
        AntPathRequestMatcher("/health"),
        AntPathRequestMatcher("/api/autenticacao/registrar"),
        AntPathRequestMatcher("/api/autenticacao/entrar"),
        AntPathRequestMatcher("/api/autenticacao/entrar-configuracao"),
        AntPathRequestMatcher("/api/autenticacao/entrar-vinculo"),
        AntPathRequestMatcher("/api/contato", "POST"),
        AntPathRequestMatcher("/api/protecao/removida", "GET"),
        AntPathRequestMatcher("/v3/api-docs/**"),
        AntPathRequestMatcher("/v3/api-docs"),
        AntPathRequestMatcher("/swagger-ui.html"),
        AntPathRequestMatcher("/swagger-ui/**"),
        AntPathRequestMatcher("/swagger-resources/**"),
        AntPathRequestMatcher("/webjars/**"),
        AntPathRequestMatcher("/error")
    )

    @Bean
    @Order(1)
    fun cadeiaFiltroPublica(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(matcherRequisicaoPublica())
            .cors { it.configurationSource(fonteConfiguracaoCors()) }
            .csrf { it.disable() }
            .headers { headers -> headers.frameOptions { it.disable() } }
            .authorizeHttpRequests { it.anyRequest().permitAll() }

        return http.build()
    }

    @Bean
    @Order(2)
    fun cadeiaFiltroProtegida(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(fonteConfiguracaoCors()) }
            .csrf { it.disable() }
            .headers { headers -> headers.frameOptions { it.disable() } }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/api/autenticacao/sair").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/autenticacao/sessao-menor")
                    .hasAuthority("SCOPE_responsavel")
                    .requestMatchers(HttpMethod.POST, "/api/autenticacao/retomar-responsavel")
                    .hasAuthority("SCOPE_configuracao")
                    .requestMatchers(HttpMethod.POST, "/api/autenticacao/autorizacao-vinculo")
                    .hasAnyAuthority("SCOPE_responsavel", "SCOPE_configuracao")
                    .requestMatchers(HttpMethod.POST, "/api/dispositivos/vincular", "/api/dispositivos/*/credencial")
                    .hasAuthority("SCOPE_vinculacao")
                    .requestMatchers(HttpMethod.POST, "/api/dispositivos/renovar-credencial")
                    .hasAuthority("SCOPE_dispositivo")
                    .requestMatchers(HttpMethod.GET, "/api/dependentes")
                    .hasAnyAuthority("SCOPE_responsavel", "SCOPE_configuracao")
                    .requestMatchers(HttpMethod.POST, "/api/dependentes", "/api/dispositivos/gerar-codigo/*")
                    .hasAnyAuthority("SCOPE_responsavel", "SCOPE_configuracao")
                    .requestMatchers(HttpMethod.GET, "/api/politica/atual")
                    .hasAnyAuthority("SCOPE_responsavel", "SCOPE_dispositivo")
                    .requestMatchers(HttpMethod.GET, "/api/protecao/palavra-chave")
                    .hasAnyAuthority("SCOPE_responsavel", "SCOPE_configuracao", "SCOPE_vinculacao", "SCOPE_dispositivo")
                    .requestMatchers(HttpMethod.PUT, "/api/protecao/palavra-chave")
                    .hasAnyAuthority("SCOPE_responsavel", "SCOPE_configuracao", "SCOPE_vinculacao")
                    .requestMatchers(HttpMethod.POST, "/api/protecao/verificar", "/api/protecao/desconectar", "/api/protecao/sinal")
                    .hasAuthority("SCOPE_dispositivo")
                    .requestMatchers("/api/eventos/**", "/api/blocklist/**")
                    .hasAuthority("SCOPE_dispositivo")
                    .anyRequest().hasAuthority("SCOPE_responsavel")
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            }

        return http.build()
    }

    @Bean
    fun fonteConfiguracaoCors(): CorsConfigurationSource {
        val configuracao = CorsConfiguration()
        configuracao.allowedOriginPatterns = listOf("*")
        configuracao.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuracao.allowedHeaders = listOf("*")
        // allowCredentials NAO definido: JWT usa header Authorization, nao cookies
        val fonte = UrlBasedCorsConfigurationSource()
        fonte.registerCorsConfiguration("/**", configuracao)
        return fonte
    }

    @Bean
    fun codificadorSenha(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun decodificadorJwt(
        @Value("\${guardian.security.jwt.secret}") segredo: String,
        sessoes: SessaoCredencialRepositorio,
        dispositivos: DispositivoRepositorio
    ): JwtDecoder {
        val chave = SecretKeySpec(segredo.toByteArray(), "HmacSHA256")
        val decoder = NimbusJwtDecoder.withSecretKey(chave).build()
        val validadorSessao = OAuth2TokenValidator<Jwt> { jwt ->
            val sessaoId = uuidOuNulo(jwt.id)
            val sujeitoId = uuidOuNulo(jwt.subject)
            val sessao = sessaoId?.let { sessoes.findById(it).orElse(null) }
            val valida = sessao != null &&
                sujeitoId != null &&
                sessao.sujeitoId == sujeitoId &&
                sessao.tipo.escopo == jwt.getClaimAsString("scope") &&
                sessao.revogadaEm == null &&
                sessao.expiraEm.isAfter(Instant.now()) &&
                (sessao.tipo != TipoCredencial.DISPOSITIVO || dispositivos.existsById(sujeitoId))
            if (valida) OAuth2TokenValidatorResult.success()
            else OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "Sessão inválida ou encerrada", null))
        }
        decoder.setJwtValidator(DelegatingOAuth2TokenValidator(JwtValidators.createDefault(), validadorSessao))
        return decoder
    }

    private fun uuidOuNulo(valor: String?): UUID? = try {
        valor?.let(UUID::fromString)
    } catch (e: IllegalArgumentException) {
        null
    }
}
