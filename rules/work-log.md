# Work Log

> 프로젝트 작업 일지. 코드만 봐서는 알 수 없는 맥락을 기록한다.

---

## 현재 상태 스냅샷

> 매 작업 후 갱신. 새 세션 시작 시 이 섹션만 읽으면 전체 파악 가능.

- **마지막 작업일**: 2025-02-12
- **진행 중인 작업**: 없음
- **블로커**: `User.providerType` `@Enumerated` 수정 — 프로덕션 DB 컬럼 타입 확인 필요
- **미수정 CRITICAL**: 1건 (User.providerType `@Enumerated` 누락)
- **미수정 HIGH**: 7건 (CORS, 외부 API 타임아웃/재시도, FCM 에러 핸들링, JWT Refresh Token, Discord fire-and-forget)

---

## 2025-02-12 (CRITICAL 안티패턴 수정)

**분류**: refactor, security

### 작업 내용
- `Admin.kt`, `BannedIp.kt`: `data class` → `class` 변환
- `AuthController.kt` 리팩토링: 테스트 엔드포인트(`/token`, `/id-token`)를 `AuthTestController.kt`로 분리
- `AuthTestController`에 `@Profile("!prod")` 적용하여 프로덕션에서 테스트 토큰 발급 차단
- `AuthController`는 소셜 로그인만 유지, 불필요 의존성 제거

### 기술적 결정
- `@Profile("!prod")`를 메서드가 아닌 클래스 레벨에 적용 → Spring `@Profile`은 빈 레벨 어노테이션이라 메서드 단위 불가
- 기존 컨트롤러에 `@Profile` 붙이지 않고 별도 컨트롤러로 분리 → 프로덕션 소셜 로그인 엔드포인트와 테스트 엔드포인트의 책임 분리

### 영향 범위
- `domain/admin/entity/Admin.kt` — data class → class (DB 영향 없음)
- `domain/admin/entity/BannedIp.kt` — data class → class (DB 영향 없음)
- `domain/auth/controller/AuthController.kt` — 테스트 엔드포인트 제거, 의존성 축소
- `domain/auth/controller/AuthTestController.kt` — 신규 생성
- API 경로 변경 없음 (`/api/v1/auth/token`, `/api/v1/auth/id-token` 동일)

### 실패/보류한 접근
- `User.providerType`에 `@Enumerated(EnumType.STRING)` 추가 보류 → 현재 `@Enumerated` 없으면 Hibernate 기본값 ORDINAL로 DB에 정수(0,1,2) 저장됨. STRING으로 변경 시 기존 데이터 읽기 실패. DB 컬럼 타입/데이터 확인 후 마이그레이션과 함께 진행 필요

---

## 2025-02-11 (rules/ 디렉토리 생성)

**분류**: docs

### 작업 내용
- `rules/` 디렉토리 생성 및 프로젝트 문서화 체계 수립
- 코드베이스 전체 분석 (엔티티 8개, 컨트롤러 10개, DTO 30+, 테스트 25개, 인프라/설정)
- 6개 문서 작성: `entities.md`, `api-conventions.md`, `testing.md`, `domain-glossary.md`, `anti-patterns.md`, `work-log.md`
- `AGENTS.md`에 rules/ 참조 포인터 추가

### 기술적 결정
- rules/ 파일은 AGENTS.md에서 포인터로 참조 (자동 로드 아님) → AGENTS.md가 너무 커지면 에이전트 컨텍스트 낭비. 필요한 도메인 작업 시에만 해당 파일을 읽는 방식
- anti-patterns.md에 체크박스 추적 방식 채택 → 이슈 트래커 없이도 수정 상태 추적 가능

### 영향 범위
- `rules/` 디렉토리 6개 파일 신규 생성
- `AGENTS.md` 하단에 rules/ 참조 테이블 추가

### 발견한 제약 조건
- AGENTS.md는 에이전트 세션 시작 시 자동 로드됨 → 여기에 모든 규칙을 넣으면 매 세션 토큰 낭비
- 30+ 안티패턴 발견 (CRITICAL 3, HIGH 7, MEDIUM 13, LOW 7+) — `anti-patterns.md` 참조

---

## 이전 작업 이력 (커밋 기반)

### 168b48e — 공통 테스트 support 클래스 생성 및 픽스처 패턴 작성을 통한 테스트 최적화
- `IntegrationTestSupport` 베이스 클래스 생성 (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@Transactional` 통합)
- `TestFixtures` object 생성 (전 엔티티 팩토리 메서드)
- `MockMvcResponseUtils` 유틸리티 생성

### 45d2a74 — 단위 테스트 작성
- 서비스/엔티티 단위 테스트 작성 (MockK 기반)
- `@Nested` + `@DisplayName` 계층 구조 도입

### 95219ee — admin test 케이스 작성
- AdminAuthController 통합 테스트, IP 차단 시나리오 테스트

### d5badf4 — 통합 테스트 코드 작성
- 전체 도메인 통합 테스트 (EventControllerV2: 정렬, 필터링, 페이지네이션 종합 976줄)

### 5f379fa — 테스트를 통한 엣지 케이스 대응
- 테스트에서 발견된 엣지 케이스 비즈니스 로직 수정

### 9c69c1a — jacoco를 통한 테스트 커버리지 표현 추가
- JaCoCo 플러그인 설정, HTML/XML 커버리지 리포트, 엔티티/DTO/설정/인프라/스케줄러 제외

### 7ce0a21 — update 쿼리를 사용한 동시성 이슈 해결
- View 조회수 증가: `count++` → `UPDATE views SET count = count + 1`
- 50 스레드 동시성 테스트 (ViewServiceConcurrencyTest)

### 04b8812 — 알람 중복 생성 방지 추가
- Alarm 엔티티에 (user_id, event_id, type) 유니크 제약 추가
- DataIntegrityViolationException 핸들링

### aa1e982 — ddl-auto 수정 (update → validate)
- 프로덕션 스키마 변경 방지

---

## 작업 일지 작성 가이드

### 템플릿

```markdown
## YYYY-MM-DD (한줄 요약)

**분류**: feature | bugfix | refactor | performance | infra | security | test | docs

### 작업 내용
- 구체적 변경 사항

### 기술적 결정
- 결정 사항 → 사유 (대안이 있었다면 왜 이걸 선택했는지)

### 영향 범위
- 변경된 파일/모듈, API 변경 여부, 스키마 변경 여부

### 실패/보류한 접근 (해당 시)
- 시도한 것 → 왜 안 됐는지 / 보류 사유

### 발견한 제약 조건 (해당 시)
- 코드만 봐서 알 수 없는 외부 제약 (DB 상태, 환경, 사업 요구사항 등)

### 성능 수치 (성능 관련 작업일 경우)
- before / after 측정값, 측정 조건
```

### 작성 원칙
1. **코드에서 추론 불가능한 것만 기록** — git log로 알 수 있는 건 안 씀
2. **"왜"가 가장 중요** — 기술적 결정의 사유, 대안, 트레이드오프
3. **실패한 접근은 반드시 기록** — 같은 삽질 반복 방지
4. **현재 상태 스냅샷 매번 갱신** — 새 세션 시작점
5. **분류 태그 필수** — 작업 유형별 빠른 검색
