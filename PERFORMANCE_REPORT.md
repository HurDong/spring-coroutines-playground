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
- 다만, **WebFlux는 1000명의 동시 접속에도 서버가 뻗지 않고(에러율 0%) 요청을 큐잉**했다는 점에서 안정성을 입증했습니다.

## 5. Scenario 2: Simulation Mode (순수 처리량 테스트)
> **목적**: DB 커넥션 풀이라는 물리적 제약을 제거하고, **서버가 얼마나 많은 스레드(요청)를 동시에 버틸 수 있는지** 확인합니다.
> **방법**: 각 요청마다 DB 조회 대신 `1초 대기(Sleep)`를 수행합니다.
> - **MVC**: `Thread.sleep(1000)` (스레드 블로킹)
> - **WebFlux**: `Mono.delay(1s)` (논블로킹)

### Results Summary (결과 요약)
| Metric | MVC (Blocking) | WebFlux (Non-Blocking) | Comparison |
| :--- | :--- | :--- | :--- |
| **Target VUs** | 300명 | **1000명** | WebFlux에 3배 부하 투입 |
| **RPS (Throughput)** | **186.7 req/s** | **495.1 req/s** | **WebFlux가 2.65배 더 빠름** |
| **P95 Latency** | 1.75 s | 4.48 s | 부하가 높아서 Latency는 증가함 |
| **Resource Limit** | 1.0 CPU | 1.0 CPU | 동일 CPU에서 효율 차이 증명 |

### Comparison Analysis (비교 분석)
1.  **MVC의 한계 (Thread Pool Limit)**:
    - 톰캣(Tomcat)의 기본 스레드 풀 크기는 **200개**입니다.
    - 요청 하나당 1초를 자게 되므로, 스레드 200개가 모두 잠자는데 쓰이면 더 이상 요청을 처리할 수 없습니다.
    - 결과적으로 RPS가 **약 187 req/s**에서 물리적인 한계에 부딪혔습니다. (200을 절대 넘을 수 없음)

2.  **WebFlux의 효율성 (Event Loop)**:
    - WebFlux는 스레드를 잠재우지 않고, 1개의 스레드가 여러 요청의 타이머를 관리합니다.
    - 덕분에 **1000명의 동시 접속**을 처리하면서도 MVC보다 **2.6배 높은 처리량(495 RPS)**을 기록했습니다.
    - 1.0 CPU라는 극한의 자원 제한 속에서도 스레드 컨텍스트 스위칭 비용이 적어 훨씬 더 효율적으로 동작했습니다.

## 6. Optimization Experiment: Tomcat Thread Pool Tuning
> **목적**: MVC(Blocking I/O) 성능을 끌어올리기 위한 **최적의 쓰레드 수 찾기**
> **환경**: 1.0 CPU, 512MB RAM, 300 VUs (1s Sleep)

### Execution Results (실행 결과)
| Config | Target VUs | Threads | **RPS** | Avg Latency | Analysis |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Test 1** | 300 | 200 (Default) | **185.5** | 1.51s | **Thread Starvation**. 쓰레드 부족으로 병목 발생. |
| **Test 2** | 300 | 500 | **276.2** | 1.04s | **Max Performance**. 300 VUs를 거의 다 소화함 (+49% 성능 향상). |
| **Test 3** | 300 | 1000 | **275.4** | 1.05s | **Saturation**. 500개일 때와 차이 없음 (Diminishing Return). |

### Conclusion (최적화 결론)
1.  **쓰레드 풀 증설 효과**: Blocking I/O 위주의 작업(DB 조회, 외부 API 대기)이 많다면, 기본값(200)은 **유휴 자원(CPU)을 낭비**하게 만듭니다. 이 경우 쓰레드를 늘리는 것만으로도 처리량을 대폭 개선할 수 있습니다.
2.  **Sweet Spot 발견**: 1.0 CPU 환경에서 1초 대기 기준, **500 쓰레드**까지는 확실한 성능 향상이 있었습니다. 하지만 **1000개까지 늘려도 추가 이득은 없었습니다.**
3.  **권장 설정**: 1 Core / 1s Latency 기준, **300~500 Threads**가 가장 가성비 높은 설정입니다.

## 7. Challenge 1: CPU vs I/O Mixed Workload (Real-world Scenario)
> **시나리오**: "DB 조회하는 유저(I/O, 100명)와 암호화 연산하는 유저(CPU, 100명)가 동시에 몰린다면?"
> **환경**: 1.0 CPU, 100 IO VUs + 100 CPU VUs (Hell Mode 🔥)

### Execution Results (지옥의 혼합 부하 테스트)
| Metric | MVC (Blocking) | WebFlux (Non-Blocking) | Winner |
| :--- | :--- | :--- | :--- |
| **Total RPS** | 38.9 req/s | **50.3 req/s** | 🏆 WebFlux (+29%) |
| **Avg Latency** | 4.14s | **2.74s** | 🏆 WebFlux (34% Faster) |
| **P90 Latency** | 10.5s | **9.9s** | 🏆 WebFlux |
| **Why?** | 쓰레드 200개가 CPU 1개를 놓고 **개싸움(Thrashing)**을 벌임. | **Backpressure**. `boundedElastic`이 CPU 작업을 10개로 제한하여 **과부하 방지**. |

### Conclusion (교훈)
1.  **MVC의 취약점**: 쓰레드 풀은 "공유 자원"입니다. CPU를 많이 쓰는 API 하나가 터지면, **DB만 조회하는 선량한 API들도 같이 느려집니다.** (전파 장애)
2.  **WebFlux의 격리 전략**: WebFlux는 CPU 작업을 별도의 풀(`boundedElastic`)로 격리하고, 동시 실행 수를 제한(Cap)합니다. 덕분에 중요 I/O(Event Loop)가 완전히 멈추는 것을 막아줍니다.
3.  **현업 적용**: MVC를 쓴다면 무거운 작업은 반드시 `@Async`나 별도 쓰레드 풀로 격리해야 합니다. 안 그러면 **"로그인 요청" 때문에 "결제 서버"가 죽는** 사태가 벌어집니다.

## 8. Challenge 2: Dual Resource Pooling (The Illusion of Capacity)
> **시나리오**: "쓰레드는 500명인데, DB 문(Connection)은 10개뿐이라면?"
> **환경**: 1.0 CPU, HikariCP Pool=10 (Fixed), Tomcat Threads (50 vs 500)

### Execution Results (이중 병목 테스트)
| Metric | Test A (50 Threads) | Test B (500 Threads) | Result |
| :--- | :--- | :--- | :--- |
| **RPS** | 9.63 req/s | 9.61 req/s | **No Change** (Same) |
| **Max Latency** | 10.83s | 10.90s | **No Change** (Same) |
| **Analysis** | DB Pool(10개)에 막혀서 RPS는 10을 넘을 수 없음. | 쓰레드를 10배 늘렸지만, **주차장(DB) 자리가 없어서** 차들이 입구에서 10초씩 대기함. |

### Conclusion (교훈)
1.  **병목의 이동**: 성능 튜닝은 **"가장 좁은 병목"**을 찾는 게임입니다. DB Connection이 10개라면, Tomcat 쓰레드를 10,000개로 늘려도 성능은 1도 오르지 않습니다.
2.  **자원 낭비**: 오히려 500개의 쓰레드는 메모리(Stack)를 점유하고, 컨텍스트 스위칭 비용만 늘립니다. (이번 테스트에선 CPU가 널널해서 티가 덜 났지만, 메모리 부족(OOM)으로 이어질 수 있음)
3.  **공식**: `Appropriate Threads` ≈ `DB Connections` + `Spare (for non-DB tasks)`.
    *   무작정 쓰레드 늘리는 것보다 **DB Pool 사이징**이 선행되어야 합니다.

