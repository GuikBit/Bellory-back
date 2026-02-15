package org.exemplo.bellory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class SwaggerConfig {

    private final BuildProperties buildProperties;

    public SwaggerConfig(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties.orElse(null);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        String version = buildProperties != null ? buildProperties.getVersion() : "dev";
        String buildTime = buildProperties != null && buildProperties.getTime() != null
                ? buildProperties.getTime().toString()
                : "N/A";

        return new OpenAPI()
                .info(new Info()
                        .title("Bellory API")
                        .version(version)
                        .description("API do sistema Bellory - Plataforma SaaS para gestao de saloes e negocios de servicos\n\n" +
                                "**Build:** " + buildTime)
                        .contact(new Contact()
                                .name("Bellory")
                                .email("contato@bellory.com.br")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("bellory-api")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
