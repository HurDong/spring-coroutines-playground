package com.example.performance.webflux.controller;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    @GetMapping("/simulate-delay")
    public Mono<String> simulateDelay() {
        log.info("▶️ [Start] Non-Blocking Simulation Request on Thread: {}", Thread.currentThread().getName());
        return Mono.delay(java.time.Duration.ofMillis(1000))
                .doOnNext(l -> log.info("⏹️ [End]   Simulation Finished on Thread:  {}",
                        Thread.currentThread().getName()))
                .map(l -> "Non-blocking Simulation Response (1s delay)");
    }

    @GetMapping("/simulate-cpu")
    public Mono<String> simulateCpu() {
        return Mono.fromCallable(() -> {
            log.info("🔥 [Start] CPU Request (Offloaded) on Thread: {}", Thread.currentThread().getName());
            long start = System.currentTimeMillis();
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                for (int i = 0; i < 500000; i++) {
                    md.update("cpu-bound-work".getBytes());
                    md.digest();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            long duration = System.currentTimeMillis() - start;
            log.info("🔥 [End]   CPU Request finished in {}ms on Thread: {}", duration,
                    Thread.currentThread().getName());
            return "CPU Non-Blocking Response (Duration: " + duration + "ms)";
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/faulty-service")
    public Mono<String> faultyService() {
        // 5 seconds delay (Non-blocking)
        return Mono.delay(java.time.Duration.ofSeconds(5))
                .map(i -> "Faulty Service Response");
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}
