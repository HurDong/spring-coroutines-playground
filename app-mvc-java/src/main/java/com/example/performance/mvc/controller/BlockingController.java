package com.example.performance.mvc.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BlockingController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BlockingController.class);
    private final JdbcTemplate jdbcTemplate;

    public BlockingController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/blocking-db")
    public String blockingDb() {
        log.info("▶️ [Start] Blocking DB Request on Thread: {}", Thread.currentThread().getName());

        // Force DB to sleep for 1 second
        jdbcTemplate.execute("SELECT pg_sleep(1)");

        log.info("⏹️ [End]   Blocking DB Request on Thread: {}", Thread.currentThread().getName());
        return "Blocking Response (1s delay)";
    }

    @GetMapping("/simulate-delay")
    public String simulateDelay() {
        log.info("▶️ [Start] Blocking Simulation Request on Thread: {}", Thread.currentThread().getName());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("⏹️ [End]   Blocking Simulation Request on Thread: {}", Thread.currentThread().getName());
        return "Blocking Simulation Response (1s delay)";
    }
}
