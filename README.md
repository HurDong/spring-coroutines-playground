# Spring Boot Coroutines Playground

This repository contains experiments comparing the performance and architecture of Spring Boot MVC (Blocking) and WebFlux + Coroutines (Non-Blocking).

## 📊 Performance Report
Detailed benchmarks and analysis are available in the [PERFORMANCE_REPORT.md](./PERFORMANCE_REPORT.md).

## Experiments
- **Experiment 1 (I/O Bound)**: Thread Pool Saturation vs Event Loop efficiency.
- **Experiment 2 (Mixed Workload)**: Impact of CPU-bound tasks on I/O throughput.
- **Experiment 3 (DB Bottleneck)**: The ineffectiveness of thread scaling under connection pool limits.

## Getting Started
(Instructions to run the app...)
