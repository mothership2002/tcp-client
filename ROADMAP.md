# Roadmap

TCP Client Spring Boot Starter의 향후 개발 계획입니다.

## v1.2.0 - Performance Optimization (예정)

### 주요 목표: 성능 최적화 및 확장성 개선

#### High Priority (P0)

- [ ] **리플렉션 캐싱** - 성능 90% 이상 개선
  - TcpMessageSerializer에 필드 메타데이터 캐싱 추가
  - TcpMessageDeserializer에 필드 메타데이터 캐싱 추가
  - `ConcurrentHashMap<Class<?>, List<FieldMetadata>>` 패턴 적용
  - 중복 리플렉션 제거 (deserialize + validateChecksums)
  - 성능 벤치마크 테스트 추가

- [ ] **부하 테스트 추가**
  - JMH (Java Microbenchmark Harness) 통합
  - 1,000 TPS, 10,000 TPS 시나리오 테스트
  - 메모리 사용량 프로파일링
  - CPU 사용률 측정

#### Medium Priority (P1)

- [ ] **문서 개선**
  - 외부 서버 연결 가이드 추가
  - 실제 사용 사례 예제 (은행, 결제 등)
  - 트러블슈팅 가이드 확장
  - 프로덕션 체크리스트

- [ ] **모니터링 강화**
  - 리플렉션 호출 횟수 메트릭
  - TCP 연결 풀 통계
  - 메시지 처리 시간 히스토그램
  - 에러율 추적

#### Low Priority (P2)

- [ ] **MethodHandle 도입** (선택적)
  - Java 7+ MethodHandle API 사용
  - 리플렉션 대비 3-5배 성능 향상
  - JIT 컴파일러 최적화 활용
  - 호환성 검증

### 예상 성능 개선

| 항목 | 현재 | v1.2.0 목표 |
|------|------|-------------|
| 메시지 처리 시간 | ~10ms | ~1ms |
| CPU 사용률 (10,000 TPS) | 미측정 | 50% 이하 |
| 메모리 사용량 (1,000 동시) | ~100MB | ~80MB |
| 리플렉션 오버헤드 | 100% | 10% 이하 |

---

## v1.3.0 - Connection Management (예정)

### 주요 목표: 연결 관리 및 안정성 개선

#### Features

- [ ] **연결 풀링**
  - 재사용 가능한 Channel 풀 구현
  - 설정 가능한 최소/최대 연결 수
  - 유휴 연결 타임아웃
  - 연결 헬스 체크

- [ ] **재연결 로직**
  - 연결 실패 시 자동 재시도
  - Exponential backoff 전략
  - Circuit breaker 패턴
  - 설정 가능한 재시도 정책

- [ ] **헬스 체크**
  - TCP keepalive 활용
  - 주기적 ping/pong 메시지
  - 연결 상태 모니터링
  - Actuator health indicator 통합

---

## v2.0.0 - Advanced Features (장기)

### 주요 목표: 고급 기능 및 확장성

#### New Features

- [ ] **동적 길이 프로토콜 지원**
  - 가변 길이 메시지
  - Length-prefixed 프로토콜
  - Delimiter-based 프로토콜

- [ ] **SSL/TLS 지원**
  - Netty SslHandler 통합
  - 인증서 관리
  - 양방향 TLS (mTLS)

- [ ] **메시지 압축**
  - GZIP, LZ4 지원
  - 설정 가능한 압축 레벨
  - 자동 압축/해제

- [ ] **멀티플렉싱**
  - 하나의 연결로 여러 메시지 동시 처리
  - Request ID 기반 매칭
  - 순서 보장 옵션

- [ ] **스트리밍 지원**
  - 대용량 파일 전송
  - 청크 단위 처리
  - Reactive Streams 통합

---

## 커뮤니티 요청 기능

사용자 요청에 따라 우선순위가 변경될 수 있습니다.

### 검토 중

- [ ] WebSocket fallback
- [ ] HTTP/2 over TCP
- [ ] 프로토콜 버전 관리
- [ ] 스키마 진화 (schema evolution)
- [ ] 자동 코드 생성 (annotation processor)

---

## 배포 및 에코시스템

### v1.2.0
- [ ] JitPack 배포 자동화
- [ ] Maven Central 등록 (선택)
- [ ] Gradle Plugin 제공 (선택)

### v2.0.0
- [ ] Spring Boot Starter 공식 제안
- [ ] Spring Cloud 통합
- [ ] Micrometer Tracing 통합

---

## 성능 목표

| 버전 | TPS | 레이턴시 (P99) | 메모리 (1K 연결) |
|------|-----|----------------|------------------|
| v1.1.x | ~10,000 | ~10ms | ~100MB |
| v1.2.0 | ~50,000 | ~1ms | ~80MB |
| v1.3.0 | ~100,000 | ~1ms | ~60MB |
| v2.0.0 | ~500,000 | <1ms | ~50MB |

---

## 기여 방법

우선순위를 변경하고 싶거나 새로운 기능을 제안하고 싶다면:

1. [GitHub Issues](https://github.com/mothership2002/tcp-client/issues)에 요청
2. 투표로 우선순위 결정
3. Pull Request 환영

---

## 버전 정책

- **메이저 버전 (x.0.0)**: Breaking changes
- **마이너 버전 (1.x.0)**: 새로운 기능, 호환성 유지
- **패치 버전 (1.1.x)**: 버그 수정, 성능 개선

---

**최종 업데이트**: 2026-01-01
**현재 버전**: v1.1.1
**다음 릴리즈**: v1.2.0 (리플렉션 최적화)
