package com.example.gatewayservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SecurityConfigTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void actuatorHealth_isPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                // si actuator está y se expone health, debe ser 200 sin auth
                .expectStatus().isOk();
    }

    @Test
    void otherEndpoints_requireAuth() {
        webTestClient.get()
                .uri("/some-protected")
                .exchange()
                // No hay route real; al menos debe bloquear antes de 404
                .expectStatus().isUnauthorized();
    }
}

