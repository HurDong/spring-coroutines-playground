# Spring Boot Performance Playground: MVC vs WebFlux vs Coroutines

이 프로젝트는 **Blocking I/O (Spring MVC)** 와 **Non-blocking I/O (Spring WebFlux + Kotlin Coroutines)** 의 성능 및 아키텍처 차이를 직접 비교 실험하기 위해 만들어졌습니다.

## 🏗️ 내부 구조 (Internal Architecture)

외부 API 서버를 따로 띄우지 않고, **하나의 앱 안에서 모의(Simulation)** 하기 위해 2개의 컨트롤러를 만들었습니다.

### 1. `AggregateController` (주인공)
*   **역할**: 우리가 테스트하는 **진짜 서버**입니다.
*   **하는 일**: 사용자 요청을 받으면, `DownstreamController`를 3번 호출하고 결과를 모아서 응답합니다.
*   **부하 테스트 대상**: k6는 이곳(`8080/api/aggregate`)을 공격합니다.

### 2. `DownstreamController` (조연)
*   **역할**: 외부 API (Google, Naver, DB 등)를 **흉내내는 가짜 서버**입니다.
*   **하는 일**: 아무 일도 안 하고 **200ms 동안 잠만 잡니다** (Delay Simulation).
*   **이유**: 네트워크 I/O 지연 시간을 강제로 만들기 위함입니다.

```mermaid
graph LR
    User[사용자 (k6)] -->|요청| Aggregate[Aggregate Controller]
    Aggregate -->|호출 1| Downstream[Downstream Controller (200ms 지연)]
    Aggregate -->|호출 2| Downstream
    Aggregate -->|호출 3| Downstream
```

---

## 🚀 실행 방법 (How to Run)

Java나 Gradle 설치가 필요 없습니다. **Docker**만 있으면 됩니다.

### 1. 환경 실행
```bash
./run-docker.sh
```
*   모든 애플리케이션을 빌드합니다.
*   앱 3개, Prometheus, Grafana를 실행합니다.

### 2. 부하 테스트 실행 (Load Test)
테스트 폴더로 이동하여 원하는 테스트를 실행하세요:
```bash
cd loadtest-k6

# 전체 순차 실행 (추천)
./run-test.sh all

# 개별 실행
./run-test.sh mvc
./run-test.sh kotlin
```

### 3. 결과 분석
*   **Grafana**: [http://localhost:3000](http://localhost:3000) (로그인: `admin` / `admin`)
    *   대시보드: **Spring Playground Performance**
*   **테스트 리포트**: `loadtest-k6/reports/` 폴더에 JSON 결과 파일이 저장됩니다.

---

## 📊 핵심 결과 (Key Findings)

**동시 접속자 1,000명(VUs)** 상황에서 스트레스 테스트를 진행한 결과입니다:

### 1. JVM 스레드 효율성 (결정적 차이)
*   **MVC**: 스레드 개수가 **400개 이상** (Tomcat Max)으로 치솟았습니다. 결국 스레드가 고갈되어 요청들이 대기열에서 기다려야 했습니다.
*   **WebFlux/Kotlin**: 스레드 개수가 **약 34개**로 유지되었습니다. 1,000명의 요청을 처리하면서도 스레드를 거의 추가로 생성하지 않았습니다.

### 2. 응답 지연 (Latency)
*   **MVC**: 스트레스 구간에서 응답 속도가 **1.0초 ~ 5.0초**까지 느려졌습니다. (Blocking Queue 대기 현상)
*   **WebFlux/Kotlin**: 응답 속도가 **약 200ms**로 매우 안정적이었습니다. (Non-blocking 처리)

### 3. 개발 생산성 (Java vs Kotlin)
*   **WebFlux Java**: 성능은 훌륭하지만, Reactor의 함수형 연산자(`flatMap`, `zip`) 체이닝으로 인해 코드가 복잡합니다.
*   **WebFlux Kotlin**: **최고의 선택.** Java WebFlux와 성능은 동일하면서도, 코루틴 덕분에 동기 코드처럼 직관적이고 간결한 코드를 작성할 수 있습니다.

---

## 🛠️ 프로젝트 구조

```
spring-coroutines-playground/
├── app-mvc-java/           # Blocking MVC 앱
├── app-webflux-java/       # Non-blocking Reactor 앱
├── app-webflux-kotlin/     # Non-blocking Coroutines 앱
├── loadtest-k6/            # k6 부하 테스트 스크립트
│   ├── run-test.sh         # 테스트 실행 도우미 스크립트
│   └── script.js           # 테스트 시나리오
├── observability/          # 모니터링 환경 설정
│   ├── docker-compose.yml
│   └── grafana/            # 대시보드 설정
└── run-docker.sh           # 전체 실행 스크립트
```
