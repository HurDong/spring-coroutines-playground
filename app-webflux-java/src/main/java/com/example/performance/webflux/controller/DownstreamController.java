package com.example.performance.webflux.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class DownstreamController {

    @GetMapping("/downstream/{id}")
    public Mono<String> downstream(@PathVariable String id, @RequestParam(defaultValue = "200") long delayMs) {
        return Mono.delay(Duration.ofMillis(delayMs))
                .map(i -> "response-" + id);
    }
}
