package org.exemplo.bellory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bellory API")
                        .version("1.0")
                        .description("API do sistema Bellory - Plataforma SaaS para gestão de salões e negócios de serviços")
                        .contact(new Contact()
                                .name("Bellory")
                                .email("contato@bellory.com.br")));
    }

    /**
     * Configuração para ordenar os tags/grupos na documentação Scalar.
     * Os tags são exibidos na ordem em que aparecem nesta lista.
     * Para alterar a ordem, basta reorganizar os itens do array.
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("bellory-api")
                .pathsToMatch("/api/**")
                // ===================================================
                // EXEMPLO: Para ocultar endpoints específicos da documentação,
                // descomente a linha abaixo e adicione os paths que deseja esconder:
                //
                // .pathsToExclude("/api/test/**", "/api/webhook/**")
                //
                // Você também pode excluir controllers inteiros:
                // .pathsToExclude("/api/email/**")
                // ===================================================
                .build();
    }
}
