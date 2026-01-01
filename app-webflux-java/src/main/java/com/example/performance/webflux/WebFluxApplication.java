package com.example.performance.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class WebFluxApplication {

    public static void main(String[] args) {
        System.setProperty("server.port", "8082");
        System.setProperty("reactor.netty.ioWorkerCount", "16"); // Optional tuning
        SpringApplication.run(WebFluxApplication.class, args);
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
