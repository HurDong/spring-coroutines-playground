# Spring Boot Coroutines Playground

이 저장소는 Spring Boot MVC (Blocking)와 WebFlux + Coroutines (Non-Blocking)의 성능 및 아키텍처 차이를 비교 실험한 프로젝트입니다.

## 📊 성능 보고서
상세한 벤치마크 결과와 분석 내용은 [PERFORMANCE_REPORT.md](./PERFORMANCE_REPORT.md)에서 확인하실 수 있습니다.

## 주요 실험 내용
- **실험 1 (I/O Bound)**: 쓰레드 풀 고갈(Saturation) vs 이벤트 루프의 효율성 비교
- **실험 2 (Mixed Workload)**: CPU 작업이 섞여 있을 때 I/O 처리량이 어떻게 무너지는가?
- **실험 3 (DB Bottleneck)**: DB 커넥션이 부족할 때 쓰레드를 늘리는 것이 의미가 있는가?
- **실험 4 (Resilience)**: 장애 격리(Fault Isolation) - 느린 API 하나가 전체 서버를 죽이는가?

## 시작하기
(애플리케이션 실행 방법...)
