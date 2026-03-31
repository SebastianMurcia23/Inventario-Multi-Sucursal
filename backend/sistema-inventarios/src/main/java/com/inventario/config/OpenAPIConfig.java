package com.inventario.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI 3.0 (Swagger) para documentación de la API.
 * Define la información de la API, esquema de seguridad JWT y otros detalles.
 */
@Configuration
public class OpenAPIConfig {

    /**
     * Configura el swagger de OpenAPI con información de la API.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OptiPlant - Sistema de Inventario Multi-Sucursal")
                        .version("1.0.0")
                        .description("API REST para gestión de inventario, compras, ventas y transferencias entre sucursales")
                        .contact(new Contact()
                                .name("OptiPlant Consultores")
                                .email("info@optiplan.com")
                                .url("https://optiplan.com")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer JWT",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Introduce el JWT sin 'Bearer '")
                        )
                );
    }
}
