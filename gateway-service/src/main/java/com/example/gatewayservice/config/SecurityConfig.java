package com.example.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Solo crea el decoder cuando la propiedad existe.
     * Así el gateway puede arrancar en local incluso sin Keycloak/config-server.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri
    ) {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri
    ) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Usará el CorsWebFilter definido en CorsConfig
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        // El navegador manda preflight OPTIONS; debe estar permitido.
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Rutas públicas típicas (ajusta según tu necesidad)
                        .pathMatchers("/actuator/**", "/eureka/**", "/public/**").permitAll()
                        // Para evitar bloquear el frontend mientras no haya auth configurada
                        .pathMatchers("/tool-service/**").permitAll()
                        .anyExchange().authenticated()
                );

        // Activa JWT solo si hay jwk-set-uri configurado.
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        }

        return http.build();
    }
}
