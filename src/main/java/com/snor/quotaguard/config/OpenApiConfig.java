package com.snor.quotaguard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI quotaGuardOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("QuotaGuard API")
                        .version("1.0.0")
                        .description("""
                                Domain-neutral backend system for adaptive quota enforcement,
                                progressive penalties, usage tracking, session regulation,
                                and behavioral analytics.
                                """)
                        .contact(new Contact()
                                .name("QuotaGuard API")
                                .url("https://github.com/YOUR_USERNAME/quotaguard-api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/license/mit")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")
                ))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME));
    }
}