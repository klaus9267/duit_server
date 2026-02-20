# Anti-Patterns & Issues

> 코드베이스에서 발견된 문제점, 개선 필요 사항, 기술 부채 기록

**범례:**
- `CRITICAL` — 데이터 무결성 또는 보안 위험. 즉시 수정 권장
- `HIGH` — 장애 또는 성능 문제 가능성. 조기 수정 권장
- `MEDIUM` — 코드 품질/유지보수성 저하. 리팩토링 시 수정
- `LOW` — 문서화/일관성 이슈. 시간 될 때 수정

---

## Entity 관련

### [CRITICAL] User.providerType에 `@Enumerated` 누락
- **파일**: `domain/user/entity/User.kt:30`
- **문제**: `@Enumerated(EnumType.STRING)` 없이 enum 필드 선언. Hibernate 기본값 `ORDINAL`로 저장되어 enum 순서 변경 시 데이터 깨짐
- **수정**: `@Enumerated(EnumType.STRING)` 추가. 단, 프로덕션 DB 컬럼 타입 확인 후 마이그레이션과 함께 진행 필요
- **상태**: [ ] 미수정

---

## 보안 관련

### [HIGH] CORS `allowedHeaders`가 와일드카드
- **파일**: `application/config/SecurityConfig.kt:105`
- **문제**: `allowedHeaders = listOf("*")` — 모든 헤더 허용
- **수정**: 필요한 헤더만 화이트리스트: `["Content-Type", "Authorization", "X-Trace-Id"]`
- **상태**: [ ] 미수정

### [HIGH] JWT 토큰 갱신 메커니즘 없음
- **파일**: `application/security/JwtTokenProvider.kt`
- **문제**: Access Token만 존재. Refresh Token 없이 만료 시 재로그인 필요
- **수정**: Refresh Token 발급/갱신 엔드포인트 추가
- **상태**: [ ] 미수정

### [MEDIUM] 잘못된 JWT 토큰에 대한 로깅 없음
- **파일**: `application/security/JwtAuthenticationFilter.kt:28-31`
- **문제**: 유효하지 않은 토큰이 무시되지만 로그 기록 없음
- **수정**: 경고 레벨 로그 추가
- **상태**: [ ] 미수정

### [MEDIUM] Rate Limiting 없음
- **파일**: 전체 컨트롤러
- **문제**: 무차별 대입 공격 방어 없음
- **수정**: Spring Security rate limiting 또는 커스텀 필터 구현
- **상태**: [ ] 미수정

---

## 외부 서비스 연동

### [HIGH] 외부 API 호출에 타임아웃 미설정
- **파일**: `infrastructure/external/firebase/FirebaseUtil.kt:19`, `infrastructure/external/discord/DiscordService.kt:20`
- **문제**: RestTemplate/RestClient 기본 타임아웃(30초+) 사용. 행 요청이 스레드 차단
- **수정**: 연결 타임아웃 5초, 읽기 타임아웃 10초 설정
- **상태**: [ ] 미수정

### [HIGH] 외부 호출에 재시도 로직 없음
- **파일**: `FirebaseUtil.kt`, `DiscordService.kt`, `FCMService.kt`
- **문제**: 일시적 네트워크 장애 시 영구 실패
- **수정**: Spring Retry 또는 Resilience4j 도입
- **상태**: [ ] 미수정

### [HIGH] FCMService에 에러 핸들링/로깅 없음
- **파일**: `infrastructure/external/firebase/FCMService.kt`
- **문제**: `sendEach()` 실패 시 예외가 호출자에게 전파. 로그 없음
- **수정**: try-catch 추가, 성공/실패 로깅
- **상태**: [ ] 미수정

### [HIGH] Discord 알림 전송 실패 시 fire-and-forget
- **파일**: `infrastructure/external/discord/DiscordService.kt:36-49`
- **문제**: `CompletableFuture.runAsync()`에 에러 핸들러 없음. 실패 시 묵인
- **수정**: `.exceptionally()` 핸들러 추가
- **상태**: [ ] 미수정

### [MEDIUM] Circuit Breaker 패턴 없음
- **파일**: `FirebaseUtil.kt`, `DiscordService.kt`, `FCMService.kt`
- **문제**: 외부 서비스 다운 시 연쇄 장애 가능
- **수정**: Resilience4j `@CircuitBreaker` 도입
- **상태**: [ ] 미수정

### [LOW] Firebase API URL 하드코딩
- **파일**: `infrastructure/external/firebase/FirebaseUtil.kt:52`
- **문제**: `identitytoolkit.googleapis.com` URL이 코드에 직접 포함
- **수정**: 설정 프로퍼티로 이동
- **상태**: [ ] 미수정

---

## API / DTO 관련

### [MEDIUM] 대부분의 Response DTO에 `@Schema` 누락
- **파일**: `EventResponse.kt`, `EventResponseV2.kt`, `UserResponse.kt`, `HostResponse.kt`, `BookmarkResponse.kt`, `AlarmResponse.kt`, `AdminResponse.kt`, `BookmarkToggleResponse.kt`, `AdminLoginResponse.kt`, `PageResponse.kt`
- **문제**: Swagger 문서에 필드 설명/예시 없음
- **수정**: 모든 Response DTO 필드에 `@Schema(description, example)` 추가
- **상태**: [ ] 미수정

### [MEDIUM] 일부 Request DTO에 `@Schema` 누락
- **파일**: `AdminLoginRequest.kt`, `AdminRegisterRequest.kt`, `UpdateNicknameRequest.kt`, `UpdateUserSettingsRequest.kt`
- **문제**: API 문서에 필드 설명 없음
- **수정**: `@field:Schema` 추가
- **상태**: [ ] 미수정

### [MEDIUM] HostRequest에 validation 누락
- **파일**: `domain/host/dto/HostRequest.kt:5-7`
- **문제**: `name` 필드에 `@field:NotBlank` 없음
- **수정**: validation 어노테이션 추가
- **상태**: [ ] 미수정

### [LOW] EventController(v1)에 와일드카드 임포트
- **파일**: `domain/event/controller/EventController.kt:4`
- **문제**: `import duit.server.domain.event.dto.*`
- **수정**: 명시적 임포트로 변경
- **상태**: [ ] 미수정

### [LOW] EventService에 와일드카드 임포트
- **파일**: `domain/event/service/EventService.kt:4-6`
- **문제**: 여러 패키지에서 `*` 임포트
- **수정**: 명시적 임포트로 변경
- **상태**: [ ] 미수정

---

## 테스트 관련

### [MEDIUM] EventControllerIntegrationTest 전체 @Disabled
- **파일**: `domain/event/controller/EventControllerIntegrationTest.kt`
- **문제**: H2와 MySQL 네이티브 쿼리 비호환으로 전체 테스트 비활성화
- **수정**: 쿼리를 H2 호환으로 수정하거나 Testcontainers로 MySQL 테스트 환경 구성
- **상태**: [ ] 미수정

### [MEDIUM] ServerApplicationTests에 `@ActiveProfiles` 누락
- **파일**: `ServerApplicationTests.kt:7`
- **문제**: 기본 프로파일로 로드되어 실제 DB 연결 시도 가능
- **수정**: `@ActiveProfiles("test")` 추가
- **상태**: [ ] 미수정

### [LOW] 일부 테스트에 어설션 부족
- **파일**: `EventControllerV2IntegrationTest.kt:832` (allFiltersCombinationTest)
- **문제**: status와 배열 존재만 확인, 실제 필터링 결과 미검증
- **수정**: 필터링 조건에 맞는 데이터 어설션 추가
- **상태**: [ ] 미수정

### [LOW] 테스트 메서드 네이밍 불일치
- **파일**: 여러 테스트 파일
- **문제**: camelCase와 한글 백틱 혼용
- **수정**: 한글 백틱 네이밍으로 통일
- **상태**: [ ] 미수정

---

## 인프라/설정 관련

### [MEDIUM] 스케줄러 분산 락 없음
- **파일**: `application/scheduler/EventStatusScheduler.kt`, `EventAlarmScheduler.kt`
- **문제**: 멀티 인스턴스 배포 시 중복 실행 가능
- **수정**: Redis 기반 분산 락 도입 (ShedLock 등)
- **상태**: [ ] 미수정

### [LOW] RedisConfig 주석 처리된 채 방치
- **파일**: `application/config/RedisConfig.kt`
- **문제**: 전체 주석 처리. 사용 여부 불명확
- **수정**: 삭제하거나 사용 목적 문서화
- **상태**: [ ] 미수정

### [LOW] application-test.yml에 Discord URL 직접 작성
- **파일**: `src/test/resources/application-test.yml:40-46`
- **문제**: 테스트용 Discord URL이 하드코딩
- **수정**: Mock 서비스 사용 또는 환경변수 처리
- **상태**: [ ] 미수정

---

## 대규모 트래픽 대비 (향후 개선 사항)

### 캐싱
- [ ] 행사 목록 조회 Redis 캐싱
- [ ] 행사 상세 조회 캐싱 (TTL 기반)
- [ ] 조회수 증가 버퍼링 (Redis INCR → 주기적 DB 반영)

### 데이터베이스
- [ ] Read Replica 구성 (읽기 부하 분산)
- [ ] 커넥션 풀 최적화 (HikariCP 튜닝)
- [ ] 슬로우 쿼리 모니터링

### 인프라
- [ ] 수평 확장 시 스케줄러 분산 락 필수
- [ ] API Gateway / 로드밸런서 구성
- [ ] CDN 적용 (썸네일 이미지)

### 비동기 처리
- [ ] 알림 발송 큐 도입 (Redis Queue 또는 Kafka)
- [ ] Discord 알림 비동기 큐 처리
- [ ] 이미지 리사이징 비동기 처리
