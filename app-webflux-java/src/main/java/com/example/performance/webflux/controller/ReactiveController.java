package com.example.performance.webflux.controller;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class ReactiveController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReactiveController.class);
    private final DatabaseClient databaseClient;

    public ReactiveController(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @GetMapping("/non-blocking-db")
    public Mono<String> nonBlockingDb() {
        log.info("▶️ [Start] Non-Blocking Request on Thread: {}", Thread.currentThread().getName());

        return databaseClient.sql("SELECT pg_sleep(1)")
                .fetch()
                .rowsUpdated()
                .doOnNext(count -> log.info("⏹️ [End]   DB Result Arrived on Thread:    {}",
                        Thread.currentThread().getName()))
                .map(count -> "Non-blocking Response (1s delay)")
                .defaultIfEmpty("Non-blocking Response (1s delay)");
    }
}
