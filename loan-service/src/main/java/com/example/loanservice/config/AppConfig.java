package com.example.loanservice.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate rt = new RestTemplate();

        // Propaga Authorization: Bearer ... a microservicios (si existe en la request actual)
        rt.getInterceptors().add((request, body, execution) -> {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                String auth = sra.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null && !auth.isBlank()) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, auth);
                }
            }
            return execution.execute(request, body);
        });

        return rt;
    }
}