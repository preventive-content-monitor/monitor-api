package br.com.guardian.backend.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ConfiguracaoOpenApi {

    @Bean
    fun openApiPersonalizado(): OpenAPI {
        val nomeEsquemaSeguranca = "bearerAuth"

        return OpenAPI()
            .info(
                Info()
                    .title("Guardian API")
                    .version("v1")
                    .description("API do Guardian - Controle Parental")
            )
            .addSecurityItem(SecurityRequirement().addList(nomeEsquemaSeguranca))
            .components(
                Components()
                    .addSecuritySchemes(
                        nomeEsquemaSeguranca,
                        SecurityScheme()
                            .name(nomeEsquemaSeguranca)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
    }
}
