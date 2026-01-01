package com.example.performance.webflux.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@RestController
public class AggregateController {

    private final WebClient webClient;
    private final MeterRegistry meterRegistry;

    public AggregateController(WebClient.Builder webClientBuilder, MeterRegistry meterRegistry) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082").build();
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/api/v1/aggregate")
    public Mono<Map<String, Object>> aggregate(@RequestParam(defaultValue = "3") int fanout,
                                               @RequestParam(defaultValue = "200") long delayMs) {
        long start = System.currentTimeMillis();

        return Flux.range(1, fanout)
                .flatMap(id -> callDownstream(id, delayMs))
                .collectList()
                .map(results -> {
                    long totalLatency = System.currentTimeMillis() - start;
                    
                    Timer.builder("aggregate.total.latency")
                            .register(meterRegistry)
                            .record(totalLatency, TimeUnit.MILLISECONDS);

                    Map<String, Object> response = new HashMap<>();
                    response.put("results", results);
                    response.put("totalLatency", totalLatency);
                    response.put("thread", Thread.currentThread().getName());
                    return response;
                });
    }

    private Mono<Map<String, Object>> callDownstream(int id, long delayMs) {
        long start = System.currentTimeMillis();
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/downstream/{id}")
                        .queryParam("delayMs", delayMs)
                        .build(id))
                .retrieve()
                .bodyToMono(String.class)
                .map(result -> {
                    long latency = System.currentTimeMillis() - start;
                    
                    Timer.builder("downstream.latency.tagged")
                            .tag("id", String.valueOf(id))
                            .register(meterRegistry)
                            .record(latency, TimeUnit.MILLISECONDS);

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", id);
                    map.put("result", result);
                    map.put("latency", latency);
                    map.put("thread", Thread.currentThread().getName());
                    return map;
                });
    }
}
