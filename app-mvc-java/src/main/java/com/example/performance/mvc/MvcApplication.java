package com.example.performance.mvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

@SpringBootApplication
public class MvcApplication {

    public static void main(String[] args) {
        System.setProperty("server.port", "8081");
        SpringApplication.run(MvcApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(java.time.Duration.ofSeconds(3))
                .setReadTimeout(java.time.Duration.ofSeconds(3))
                .build();
    }
}
