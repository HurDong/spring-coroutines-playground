package com.example.performance.mvc.controller;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RestController
public class AggregateController {

    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    private final MeterRegistry meterRegistry;

    public AggregateController(RestTemplate restTemplate, MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        // Simulating a dedicated thread pool for downstream calls to avoid blocking the main server threads too much,
        // although in pure blocking MVC often the servlet container threads are the bottleneck.
        // We will strictly follow the "Blocking IO" nature by using a cached thread pool or similar,
        // but to handle fan-out we need concurrency.
        this.executorService = Executors.newFixedThreadPool(200);
    }

    @GetMapping("/api/v1/aggregate")
    public Map<String, Object> aggregate(@RequestParam(defaultValue = "3") int fanout,
                                         @RequestParam(defaultValue = "200") long delayMs) {
        long start = System.currentTimeMillis();
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();

        for (int i = 1; i <= fanout; i++) {
            int id = i;
            futures.add(CompletableFuture.supplyAsync(() -> callDownstream(id, delayMs), executorService));
        }

        List<Map<String, Object>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        long totalLatency = System.currentTimeMillis() - start;

        // Custom Metric
        Timer.builder("aggregate.total.latency")
                .register(meterRegistry)
                .record(totalLatency, TimeUnit.MILLISECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("totalLatency", totalLatency);
        response.put("thread", Thread.currentThread().getName());
        return response;
    }

    private Map<String, Object> callDownstream(int id, long delayMs) {
        long start = System.currentTimeMillis();
        String url = "http://localhost:8081/downstream/" + id + "?delayMs=" + delayMs;
        // In a real scenario, we might use the same app or different app.
        // The requirements say "inside the same app dummy endpoint".
        
        String result = restTemplate.getForObject(url, String.class);
        
        long latency = System.currentTimeMillis() - start;
        
        // Custom Metric for downstream
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
    }
}
