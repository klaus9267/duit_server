# AGENTS.md

> Guidance for AI coding agents operating in this repository.

## Project

**DU-IT** — Spring Boot 3.5.4 + Kotlin 1.9.25 backend for a nursing event management platform.
MySQL 8 (prod/local), H2 (test), QueryDSL 5, JWT auth, Firebase social login, FCM push notifications.

## Build & Test Commands

```bash
./gradlew build                  # Full build (compile + test)
./gradlew test                   # Run all tests
./gradlew bootRun --args="--spring.profiles.active=local"  # Local dev

# Single test class
./gradlew test --tests "EventControllerV2IntegrationTest"

# Single test method (nested classes joined by dots)
./gradlew test --tests "EventControllerV2IntegrationTest.GetEventsTests.SuccessTests.SortingTests.sortByIdTest"

# Pattern matching
./gradlew test --tests "*ViewServiceTest*"
```

No separate lint command — rely on the Kotlin compiler and IDE inspections.

## Package Structure

```
src/main/kotlin/duit/server/
├── domain/                     # Business logic (organized by bounded context)
│   ├── {feature}/
│   │   ├── entity/             # JPA entities + enums
│   │   ├── dto/                # Request/Response DTOs, pagination params, filters
│   │   ├── repository/         # Spring Data JPA interfaces + custom impls
│   │   ├── service/            # Business logic
│   │   └── controller/         # REST endpoints
│   └── common/                 # Shared: pagination DTOs, ErrorResponse, extensions
├── application/                # Cross-cutting concerns
│   ├── config/                 # Spring configs (Security, QueryDsl, Swagger, etc.)
│   ├── security/               # JWT filter/provider, SecurityUtil
│   ├── exception/              # GlobalExceptionHandler
│   ├── scheduler/              # Cron jobs (EventStatus, EventAlarm)
│   └── common/                 # ErrorCode enum, RequireAuth annotation
├── infrastructure/             # External integrations
│   └── external/               # Discord webhooks, Firebase, file storage
└── ServerApplication.kt
```

## Code Style

### Imports
- **Explicit imports only** — no wildcard `*` imports (exception: `jakarta.persistence.*` in entities is tolerated).

### Entities
- `class` (not `data class`). Constructor-injected properties with `val` (immutable) or `var` (mutable).
- `@Entity` + `@Table(name = "snake_case")` with explicit indexes.
- `@EntityListeners(AuditingEntityListener::class)` for `@CreatedDate` / `@LastModifiedDate`.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` with `val id: Long? = null`.
- `@Enumerated(EnumType.STRING)` for all enums.
- `FetchType.LAZY` by default. Use `@EntityGraph` or `JOIN FETCH` when eager loading is needed.
- Domain logic lives inside the entity (e.g., `Event.updateStatus()`).

### DTOs
- **`data class`** for all DTOs.
- **Request DTOs**: `@field:NotBlank`, `@field:Schema(...)` validation annotations. Include `toEntity()` factory when converting to an entity.
- **Response DTOs**: `companion object { fun from(entity): Response }` factory pattern. Never expose entities directly.
- **Pagination params**: `init {}` blocks for validation (`require()`). Use `@get:Parameter` / `@get:Schema` for Swagger.
- Custom cross-field validators via `@Constraint` annotations (see `@ValidDateRange`).

### Repositories
- Extend `JpaRepository<Entity, Long>`.
- Complex queries: create `EventRepositoryCustom` interface + `EventRepositoryImpl` class.
- Use `findByIdOrThrow()` extension from `domain.common.extensions.RepositoryExtensions`.
- Native queries for complex sorting/filtering; JPQL for simpler cases.

### Services
- `@Service` + `@Transactional(readOnly = true)` at class level.
- `@Transactional` on individual write methods.
- Constructor injection (no `@Autowired`).
- Throw `IllegalArgumentException` for bad input, `EntityNotFoundException` for missing resources, `IllegalStateException` for conflicts — the `GlobalExceptionHandler` maps these to HTTP responses.

### Controllers
- `@RestController` + `@RequestMapping("api/v{version}/{resource}")`.
- `@Tag(name = "...")` for Swagger grouping.
- `@Operation(summary, description)` on every endpoint.
- `@ResponseStatus(HttpStatus.OK|CREATED|NO_CONTENT)` on every endpoint.
- `@RequireAuth` custom annotation for authenticated endpoints (adds Swagger lock + 401/403 docs).
- `@Valid @ParameterObject` for query param DTOs; `@Valid @RequestBody` for request bodies.
- Return service results directly (expression-body style preferred).

### Error Handling
- Centralized in `GlobalExceptionHandler` (`@RestControllerAdvice`).
- `ErrorCode` enum maps to HTTP status + Korean message.
- Standardized `ErrorResponse(code, message, fieldErrors, timestamp, path)`.
- 5xx errors trigger Discord webhook notifications automatically.

### Naming Conventions
| Element        | Convention             | Example                            |
|---------------|------------------------|------------------------------------|
| Package       | lowercase              | `domain.event.controller`          |
| Class         | PascalCase             | `EventControllerV2`                |
| Function      | camelCase              | `getEventDetail`                   |
| Property      | camelCase              | `recruitmentStartAt`               |
| Enum value    | SCREAMING_SNAKE        | `RECRUITMENT_WAITING`              |
| DB table      | snake_case plural      | `events`, `banned_ips`             |
| DB column     | snake_case             | `start_at`, `is_approved`          |
| DB index      | `idx_` prefix          | `idx_status_id`                    |
| API path      | kebab-case (no prefix) | `api/v2/events`                    |
| Test class    | `{Feature}Test` or `{Feature}IntegrationTest` | `ViewServiceTest` |

### Kotlin Idioms Used
- `?.let { }`, `?.run { }`, `?.takeIf { }` for null-safe chains.
- `require()` / `check()` for preconditions.
- `apply { }` for entity mutation after creation.
- Expression-body functions for simple returns.
- `when {}` with exhaustive branches for enums/sealed types.

## Testing

### Stack
JUnit 5, MockK (`mockk`, `every`, `verify`), SpringMockK, Spring MockMvc, H2 in-memory DB.

### Profiles
Tests auto-activate `test` profile (`src/test/resources/application.yml` sets `spring.profiles.active: test`).
`application-test.yml`: H2 with `MODE=MySQL`, `ddl-auto: create-drop`, schedulers disabled.

### Patterns
- **Integration tests**: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@Transactional`.
- **Unit tests**: `@SpringBootTest` + `@ActiveProfiles("test")` with MockK for dependencies.
- **Test naming**: Backtick method names preferred: `` fun `행사 목록 조회 성공`() ``. camelCase also used.
- **Organization**: `@Nested` inner classes with `@DisplayName` for grouping.
- **Setup**: `@BeforeEach` with `EntityManager.persist()` + `flush()` + `clear()`.
- **HTTP assertions**: `mockMvc.perform(get(...)).andExpect(status().isOk).andExpect(jsonPath("$.content").isArray)`.
- **Mocking**: `val mock = mockk<Repo>()` → `every { mock.save(any()) } returns entity` → `verify(exactly = 1) { ... }`.

## Key Files to Reference
| Purpose                    | File                                                        |
|---------------------------|-------------------------------------------------------------|
| Error codes               | `application/common/ErrorCode.kt`                          |
| Exception handling        | `application/exception/GlobalExceptionHandler.kt`           |
| Auth annotation           | `application/common/RequireAuth.kt`                         |
| Repository extension      | `domain/common/extensions/RepositoryExtensions.kt`          |
| Pagination (offset)       | `domain/common/dto/pagination/PageResponse.kt`              |
| Pagination (cursor)       | `domain/common/dto/pagination/CursorPageResponse.kt`        |
| Entity example            | `domain/event/entity/Event.kt`                              |
| Service example           | `domain/event/service/EventService.kt`                      |
| Controller example        | `domain/event/controller/EventControllerV2.kt`              |
| Integration test example  | `domain/event/controller/EventControllerV2IntegrationTest.kt` |
| Test config               | `src/test/resources/application-test.yml`                   |

## Detailed Rules (`rules/` directory)

> `rules/` 디렉토리에 도메인별 상세 가이드가 있음. 해당 도메인 작업 시 반드시 참조할 것.
>
> **⚠️ 코드 변경 후 반드시 관련 rules/ 파일도 함께 업데이트할 것.**
> - 엔티티 추가/수정 → `entities.md`, `domain-glossary.md`
> - API 추가/수정 → `api-conventions.md`
> - 테스트 패턴 변경 → `testing.md`
> - 안티패턴 수정/발견 → `anti-patterns.md`
> - 모든 작업 완료 시 → `work-log.md` (현재 상태 스냅샷 포함)

| 파일 | 내용 | 언제 읽을 것 |
|------|------|-------------|
| `rules/entities.md` | 엔티티 작성 규칙, 예시, 안티패턴 | 엔티티 생성/수정 시 |
| `rules/api-conventions.md` | API 설계 규칙, 버전닝, 요청/응답 포맷, Swagger | 컨트롤러/DTO 작성 시 |
| `rules/testing.md` | 테스트 작성 가이드, 픽스처 패턴, 베이스 클래스 | 테스트 작성 시 |
| `rules/domain-glossary.md` | 도메인 용어, 상태 흐름, 비즈니스 규칙 | 비즈니스 로직 작업 시 |
| `rules/anti-patterns.md` | 발견된 문제점, 기술 부채 목록, 개선 필요 사항 | 리팩토링/코드리뷰 시 |
| `rules/work-log.md` | 작업 일지, 기술적 결정 기록 | 작업 시작/완료 시 기록 |
