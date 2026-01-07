# Spring Boot 성능 비교 보고서: MVC vs WebFlux

## 1. 개요 (Overview)
본 보고서는 다양한 부하 시나리오에서 **Spring Boot MVC (Blocking I/O)**와 **Spring Boot WebFlux (Non-Blocking I/O)**의 성능 차이를 비교 분석합니다.  
각 스택의 아키텍처적 한계점과 자원 사용 효율성을 파악하는 것이 목적입니다.

## 2. 테스트 환경 (Environment)
- **CPU**: 1.0 vCPU (Docker 제한)
- **Memory**: 512MB
- **Database**: PostgreSQL (Docker)
- **Load Testing Tool**: k6

---

## 3. 실험 1: I/O 중심 부하 (I/O Bound Workload)
**시나리오**: 1초가 걸리는 느린 DB 쿼리에 수천 명의 동시 접속자가 몰리는 상황.
- **Endpoint**: `GET /api/v1/simulate-delay` (1초 지연)
- **설정**: MVC (쓰레드 200개) vs WebFlux (1 Core)

### 결과 (Results)
| 프레임워크 | 동시 접속자 (VUs) | 처리량 (RPS) | 응답 속도 (P95) |
| :--- | :--- | :--- | :--- |
| **MVC** | 3,000 | ~190 | ~15초 (Timeout) |
| **WebFlux** | 3,000 | ~1200 | ~2.5초 |

### 분석 (Analysis)
- **MVC**: 쓰레드 200개가 순식간에 고갈되었습니다. 1초가 걸리는 작업이므로 초당 최대 200개밖에 처리하지 못하며, 나머지 2,800명은 대기열에서 기다리다가 타임아웃이 발생했습니다.
- **WebFlux**: 이벤트 루프 구조 덕분에 쓰레드 개수(200개)에 얽매이지 않고 수천 개의 요청을 효율적으로 처리했습니다.

---

## 4. 실험 2: 혼합 부하 (CPU + I/O Mixed Workload)
**시나리오**: CPU를 많이 쓰는 작업(해싱)과 DB를 조회하는 작업이 섞여 있는 현실적인 상황.
- **부하**: 100명(DB 조회) + 100명(SHA-256 해싱) 동시 접속.
- **목표**: "CPU 작업 때문에 DB 조회까지 느려지는가?" 확인.

### 결과 (Results)
| 지표 | MVC | WebFlux | 차이 |
| :--- | :--- | :--- | :--- |
| **처리량 (RPS)** | 38.9 | 50.3 | **WebFlux +29%** |
| **응답 속도 (Avg)** | 4.14초 | 2.74초 | **WebFlux 34% 더 빠름** |

### 분석 (Analysis)
- **MVC (연좌제 발생)**: CPU 작업이 쓰레드를 독점하면서, 단순한 DB 조회 요청까지 처리할 쓰레드가 없어 대기해야 했습니다. (Global Slowdown)
- **WebFlux (격리 성공)**: CPU 작업은 별도의 전용 스케줄러(`boundedElastic`)로 격리하고, 메인 이벤트 루프는 I/O 처리에 집중했습니다. 덕분에 전체적인 시스템 안정성이 유지되었습니다.

---

## 5. 실험 3: 이중 병목 (DB Connection Pool Bottleneck)
**시나리오**: DB 커넥션 풀이 부족할 때, 웹 서버의 쓰레드를 늘리는 것이 효과가 있을까?
- **제약**: HikariCP Pool Size = 10 (고정).
- **비교**: Tomcat 쓰레드 50개 vs 500개.

### 결과 (Results)
| 지표 | 50 Threads | 500 Threads |
| :--- | :--- | :--- |
| **처리량 (RPS)** | 9.63 | 9.61 |
| **최대 지연 (Max)** | 10.83초 | 10.90초 |

### 분석 (Analysis)
- **결과**: 쓰레드를 10배 늘려도 성능은 **단 1%도 오르지 않았습니다.**
- **이유**: 병목 지점은 웹 서버(Tomcat)가 아니라 DB(HikariCP)였기 때문입니다. 주차장(DB) 자리가 10개뿐이면 입구(Thread)를 아무리 넓혀도 소용없습니다.
- **교훈**: 무작정 쓰레드를 늘리는 것은 메모리 낭비일 뿐, 병목 구간(Bottleneck) 파악이 우선입니다.

---

## 6. 실험 4: 장애 격리 (Fault Isolation - The Titanic Strategy)
**시나리오**: **"느린 API 하나 때문에 전체 서버가 죽을까?"**
- **공격**: `/faulty-service` (5초 지연)에 200명이 몰려옴.
- **피해자**: `/health` (0.01초 핑)를 호출하는 일반 유저.

### 결과 (Results)
| 지표 (/health) | MVC (Blocking) | WebFlux (Non-Blocking) | 비고 |
| :--- | :--- | :--- | :--- |
| **P90 응답 속도** | ~75ms | **~14ms** | **WebFlux 5배 빠름** |
| **최대 지연** | 1,052ms | 949ms | - |

### 분석 (Analysis)
- **MVC (침몰)**: 공격자 200명이 쓰레드 200개를 모두 점유해버려, 일반 유저(Health Check)가 들어갈 공간이 없습니다.
- **WebFlux (생존)**: 느린 요청은 비동기로 처리해두고, 이벤트 루프는 즉시 다음 요청(Health Check)을 처리했습니다. 이로써 **장애가 전파되지 않고 격리됨**을 증명했습니다.

---

## 7. 결론 (Conclusion)
1.  **I/O 효율성**: 단순 대용량 트래픽 처리는 **WebFlux**가 압도적으로 유리합니다. (Experiment 1)
2.  **안정성 (Resilience)**: WebFlux는 특정 기능의 장애가 전체 시스템으로 번지는 것을 막아줍니다. (Experiment 2, 4)
3.  **튜닝의 정석**: MVC를 사용한다면 무거운 작업은 반드시 `@Async` 등으로 격리해야 하며, 쓰레드 풀 튜닝 전에 **DB 커넥션 풀** 같은 하위 리소스를 먼저 확인해야 합니다. (Experiment 3)
