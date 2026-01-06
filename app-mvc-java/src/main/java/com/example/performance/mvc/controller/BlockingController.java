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

    @GetMapping("/simulate-cpu")
    public String simulateCpu() {
        log.info("🔥 [Start] CPU Blocking Request on Thread: {}", Thread.currentThread().getName());
        long start = System.currentTimeMillis();
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            // 500,000 hashes to burn comparable CPU to a small request
            for (int i = 0; i < 500000; i++) {
                md.update("cpu-bound-work".getBytes());
                md.digest();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long duration = System.currentTimeMillis() - start;
        log.info("🔥 [End]   CPU Blocking Request finished in {}ms on Thread: {}", duration,
                Thread.currentThread().getName());
        return "CPU Blocking Response (Duration: " + duration + "ms)";
    }
}
