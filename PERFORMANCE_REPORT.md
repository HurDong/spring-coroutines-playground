# Performance Comparison Report: Blocking (MVC) vs Non-Blocking (WebFlux)

## 1. Test Environment
- **Date**: (2026-01-06)
- **Tool**: k6
- **Hardware Constraints (Docker)**:
    - CPU: 1.0 Core
    - Memory: 512 MB
- **Network**: Localhost (Docker Internal Network)

## 2. Test Scenario
- **Target Logic**: Database Query Simulation (JDBC vs R2DBC)
- **Virtual Users (VUs)**: 
    - Blocking (MVC): 300
    - Non-Blocking (WebFlux): 1000
- **Duration**: 15s

## 3. Results Summary (결과 요약)

| Metric | MVC (Blocking) | WebFlux (Non-Blocking) | Comparison |
| :--- | :--- | :--- | :--- |
| **P95 Latency** | 30.09 s | 42.66 s | WebFlux가 더 느림 (대기열 증가) |
| **AVG Latency** | 20.58 s | 24.89 s | 비슷함 (둘 다 매우 느림) |
| **RPS (Throughput)** | 9.7 req/s | 8.9 req/s | 비슷함 (DB 병목) |
| **Max VUs** | 300 | 1000 | WebFlux가 3.3배 더 많은 유저 수용 시도 |
| **Failure Rate** | 0.23% | 0.00% | WebFlux가 에러 처리에 더 안정적 |

## 4. Detailed Analysis (상세 분석)

### Observations (관측 결과)
1. **RPS가 둘 다 약 10 req/s로 제한됨**:
   - MVC와 WebFlux 모두 초당 약 10개의 요청만 처리했습니다.
   - 이는 **DB Connection Pool (HikariCP / R2DBC Pool)**의 기본 설정(약 10개)이 병목이 되었기 때문입니다.
   - `pg_sleep(1)` 쿼리는 1초 동안 DB 커넥션을 점유하므로, 커넥션이 10개라면 최대 처리량은 물리적으로 10 RPS를 넘을 수 없습니다.

2. **WebFlux의 지연 시간이 더 긴 이유**:
   - WebFlux는 1000명의 유저(VU)를 투입했고, MVC는 300명을 투입했습니다.
   - 처리 속도(RPS)는 똑같이 10인데 대기 줄(VU)이 3배 더 기니, WebFlux 쪽의 대기 시간(Latency)이 수학적으로 더 길어질 수밖에 없었습니다.

### Conclusion (결론)
- **DB Connection이 병목인 상황**에서는 Blocking(MVC)과 Non-Blocking(WebFlux)의 성능 차이가 거의 없습니다.
- 오히려 WebFlux가 더 많은 요청을 받아들여(Backpressure 없이), 응답 시간이 길어지는 현상이 발생했습니다.
- WebFlux의 진가를 확인하려면 **DB 커넥션 풀을 늘리거나**, DB가 아닌 **외부 API 호출(WebClient)** 시나리오로 테스트해야 합니다.
# Spring Boot Performance Report: Blocking (MVC) vs Non-Blocking (WebFlux)

## 1. Overview
This report documents the performance comparison between Spring Boot MVC (Blocking I/O) and Spring Boot WebFlux (Non-Blocking I/O) under various workload scenarios. The objective is to identify the architectural thresholds and resource utilization characteristics of each stack.

## 2. Environment
- **CPU**: 1.0 vCPU (Simulated via Docker)
- **Memory**: 512MB
- **Database**: PostgreSQL (Docker)
- **Load Testing Tool**: k6

---

## 3. Experiment 1: I/O Bound Workload
**Scenario**: Simulating high-concurrency DB queries with 1s latency.
- **Endpoint**: `GET /api/v1/simulate-delay` (1s delay)
- **Configuration**: MVC (200 Threads) vs WebFlux (1 Core)

### Results
| Framework | VUs (Users) | RPS (Avg) | Latency (P95) |
| :--- | :--- | :--- | :--- |
| **MVC** | 3,000 | ~190 | ~15s |
| **WebFlux** | 3,000 | ~1200 | ~2.5s |

### Analysis
- **MVC**: Thread exhaustion occurred immediately. With 200 threads, the theoretical max RPS is capped at 200 (1s latency). Excess requests queue up, causing latency to spike linearly.
- **WebFlux**: efficiently handled thousands of concurrent connections using event loops, maintaining low latency and high throughput limited only by CPU/Network, not thread count.

---

## 4. Experiment 2: Mixed Workload (CPU + I/O)
**Scenario**: Simulating a realistic environment where intensive CPU tasks (hashing) coexist with I/O tasks.
- **Load**: 100 I/O Users (DB fetch) + 100 CPU Users (SHA-256 Hashing).
- **Metric**: Impact of CPU contention on I/O performance.

### Results
| Metric | MVC | WebFlux | Diff |
| :--- | :--- | :--- | :--- |
| **Throughput (RPS)** | 38.9 | 50.3 | WebFlux +29% |
| **Latency (Avg)** | 4.14s | 2.74s | WebFlux -34% |

### Analysis
- **MVC**: CPU-bound threads held onto CPU resources, causing starvation for I/O threads. The shared thread pool model led to a cascading degradation where simple I/O requests were blocked by CPU tasks.
- **WebFlux**: By offloading CPU tasks to a dedicated scheduler (`boundedElastic`) and limiting their concurrency, the main event loop remained responsive for I/O tasks. This isolation architecture proved superior in maintaining overall system stability.

---

## 5. Experiment 3: Resource Bottleneck (Connection Pool)
**Scenario**: Evaluating the impact of Thread Pool scaling when the Database is the bottleneck.
- **Constraints**: HikariCP Maximum Pool Size = 10.
- **Comparison**: Tomcat Threads 50 vs 500.

### Results
| Metric | 50 Threads | 500 Threads |
| :--- | :--- | :--- |
| **RPS** | 9.63 | 9.61 |
| **Latency (Max)** | 10.83s | 10.90s |

### Analysis
- **Resource Saturation**: The throughput was strictly limited by the DB connection pool (10 connections * 1s query time ≈ 10 RPS).
- **Thread Impact**: Increasing threads from 50 to 500 provided **zero performance benefit**.
- **Conclusion**: In I/O bottlenecked downstream systems, increasing the upstream concurrency limit (Thread Pool) only increases queueing and memory overhead without improving throughput.

---

## 6. Conclusion
1.  **I/O Efficiency**: WebFlux demonstrates superior scalability for high-concurrency I/O workloads.
2.  **Resilience**: WebFlux's ability to isolate blocking tasks prevents total system degradation under mixed workloads.
3.  **Resource Planning**: For Blocking APIs (MVC), simply increasing threads is ineffective if downstream resources (DB Pool) are saturated. Capacity planning must align with the slowest bottleneck.
