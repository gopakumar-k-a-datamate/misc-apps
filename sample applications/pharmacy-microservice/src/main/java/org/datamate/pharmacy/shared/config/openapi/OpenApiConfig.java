package org.datamate.pharmacy.shared.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for the Pharmacy Microservice.
 * Provides Swagger UI at /swagger-ui.html with JWT Bearer auth.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI pharmacyOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token for authentication")))
                .info(new Info()
                        .title("Pharmacy Microservice API")
                        .description("Pharmacy microservice with OPA sidecar-based authorization. "
                                + "Reference implementation for the Datamate federated authorization architecture.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Datamate Development Team")
                                .email("dev@datamate.org")));
    }
}
