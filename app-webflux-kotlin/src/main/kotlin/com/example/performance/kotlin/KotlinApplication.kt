package com.example.performance.kotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@SpringBootApplication
class KotlinApplication {

    @Bean
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
    }
}

fun main(args: Array<String>) {
    System.setProperty("server.port", "8083")
    System.setProperty("reactor.netty.ioWorkerCount", "16")
    runApplication<KotlinApplication>(*args)
}
