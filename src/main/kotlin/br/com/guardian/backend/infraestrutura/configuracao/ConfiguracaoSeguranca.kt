package br.com.guardian.backend.infraestrutura.configuracao

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.config.Customizer
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher

@Configuration
@EnableWebSecurity
class ConfiguracaoSeguranca {

    private fun matcherRequisicaoPublica() = OrRequestMatcher(
        AntPathRequestMatcher("/health"),
        AntPathRequestMatcher("/api/autenticacao/**"),
        AntPathRequestMatcher("/api/eventos/**"),
        AntPathRequestMatcher("/api/dispositivos/vincular"),
        AntPathRequestMatcher("/api/politica/atual"),
        AntPathRequestMatcher("/v3/api-docs/**"),
        AntPathRequestMatcher("/v3/api-docs"),
        AntPathRequestMatcher("/swagger-ui.html"),
        AntPathRequestMatcher("/swagger-ui/**"),
        AntPathRequestMatcher("/swagger-resources/**"),
        AntPathRequestMatcher("/webjars/**"),
        AntPathRequestMatcher("/h2-console"),
        AntPathRequestMatcher("/h2-console/**"),
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
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            }

        return http.build()
    }

    @Bean
    fun fonteConfiguracaoCors(): CorsConfigurationSource {
        val configuracao = CorsConfiguration()
        configuracao.allowedOriginPatterns = listOf(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "chrome-extension://*",
            "moz-extension://*"
        )
        configuracao.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuracao.allowedHeaders = listOf("*")
        configuracao.allowCredentials = true
        val fonte = UrlBasedCorsConfigurationSource()
        fonte.registerCorsConfiguration("/**", configuracao)
        return fonte
    }

    @Bean
    fun codificadorSenha(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun gerenciadorAutenticacao(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun decodificadorJwt(@Value("\${guardian.security.jwt.secret}") segredo: String): JwtDecoder {
        val chave = SecretKeySpec(segredo.toByteArray(), "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(chave).build()
    }
}
