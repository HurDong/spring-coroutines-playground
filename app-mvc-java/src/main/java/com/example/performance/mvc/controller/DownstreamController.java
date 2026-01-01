package com.example.performance.mvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DownstreamController {

    @GetMapping("/downstream/{id}")
    public String downstream(@PathVariable String id, @RequestParam(defaultValue = "200") long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "response-" + id;
    }
}
