# API Conventions

> REST API 설계 규칙, 버전닝, 요청/응답 포맷, Swagger 규칙

## URL 설계

### 버전닝
```
api/v{version}/{resource}
```
- 현재: `v1` (대부분), `v2` (이벤트 목록 커서 페이지네이션)
- 예: `api/v1/events`, `api/v2/events`

### 리소스 네이밍
- **kebab-case** 사용: `api/v1/admin/auth`, `api/v1/check-nickname`
- **복수형**: `events`, `users`, `hosts`, `bookmarks`, `alarms`
- **서브리소스**: `api/v1/events/{eventId}/approve`
- **배치 작업**: `/batch` 접미사 — `api/v1/events/batch`, `api/v1/hosts/batch`
- **특수 엔드포인트**: `api/v1/events/calendar`, `api/v2/events/count`

### 전체 엔드포인트 매핑

| Controller | Base Path | 버전 | 엔드포인트 수 | 인증 |
|-----------|-----------|------|-------------|------|
| EventControllerV2 | `api/v2/events` | v2 | 3 | 없음 (공개) |
| EventController | `api/v1/events` | v1 | 7 | 관리자 작업만 |
| HostController | `api/v1/hosts` | v1 | 5 | CUD만 |
| UserController | `api/v1/users` | v1 | 7 | 대부분 |
| AuthController | `api/v1/auth` | v1 | 3 | 없음 (공개) |
| BookmarkController | `api/v1/bookmarks` | v1 | 2 | 전부 |
| AlarmController | `api/v1/alarms` | v1 | 7 | 전부 |
| AdminAuthController | `api/v1/admin/auth` | v1 | 2 | register만 |
| ViewController | `api/v1/views` | v1 | 1 | 없음 (공개) |

---

## Controller 규칙

### 필수 어노테이션
```kotlin
@RestController
@RequestMapping("api/v1/events")
@Tag(name = "Event", description = "행사 관련 API")
class EventController(
    private val eventService: EventService  // 생성자 주입
) {
    @GetMapping
    @Operation(summary = "행사 목록 조회", description = "행사 목록을 페이지네이션으로 조회합니다")
    @ResponseStatus(HttpStatus.OK)
    fun getEvents(
        @Valid @ParameterObject param: EventPaginationParam
    ) = eventService.getEvents(param)
}
```

### 체크리스트
- [ ] `@RestController` + `@RequestMapping`
- [ ] `@Tag(name, description)` on class
- [ ] 모든 엔드포인트에 `@Operation(summary, description)`
- [ ] 모든 엔드포인트에 `@ResponseStatus`
- [ ] 인증 필요 엔드포인트에 `@RequireAuth`
- [ ] Query params에 `@Valid @ParameterObject`
- [ ] Request body에 `@Valid @RequestBody`
- [ ] Expression-body style 선호 (`= eventService.xxx()`)

### HTTP 상태 코드 규칙

| 동작 | 메서드 | 상태 코드 | 응답 본문 |
|------|-------|---------|---------|
| 목록 조회 | GET | 200 OK | PageResponse / CursorPageResponse |
| 단건 조회 | GET | 200 OK | Response DTO |
| 생성 | POST | 201 CREATED | Response DTO |
| 수정 | PUT/PATCH | 200 OK | Response DTO |
| 삭제 | DELETE | 204 NO_CONTENT | 없음 |
| 승인/상태변경 | PATCH | 204 NO_CONTENT | 없음 |

### 인증 패턴
```kotlin
// 인증 필요 엔드포인트
@RequireAuth  // Swagger 자물쇠 + 401/403 응답 자동 추가
@ResponseStatus(HttpStatus.OK)
fun protectedEndpoint() = ...

// 공개 엔드포인트
@ResponseStatus(HttpStatus.OK)
fun publicEndpoint() = ...
```

---

## Request DTO 규칙

### 기본 구조
```kotlin
@ValidDateRange  // 커스텀 교차 필드 검증 (필요 시)
data class EventCreateRequest(
    @field:NotBlank(message = "행사 제목은 필수입니다")
    @field:Schema(description = "행사 제목", example = "2025 AI 컨퍼런스")
    val title: String,

    @field:NotNull(message = "행사 시작일은 필수입니다")
    @field:Schema(description = "행사 시작 일시", example = "2025-02-15T09:00:00")
    val startAt: LocalDateTime,
) {
    fun toEntity(host: Host): Event = Event(
        title = title,
        startAt = startAt,
        // ...
    )
}
```

### 체크리스트
- [ ] `data class`
- [ ] 필수 필드에 `@field:NotBlank` / `@field:NotNull`
- [ ] 모든 필드에 `@field:Schema(description, example)`
- [ ] 엔티티 변환 시 `toEntity()` 팩토리 메서드 포함
- [ ] 교차 필드 검증이 필요하면 커스텀 `@Constraint` 어노테이션

### 검증 어노테이션 정리

| 어노테이션 | 용도 | 예시 |
|-----------|------|-----|
| `@field:NotBlank` | 필수 문자열 | title, uri |
| `@field:NotNull` | 필수 non-null | startAt, eventType |
| `@field:NotEmpty` | 필수 컬렉션 | hostIds (List) |
| `@field:Size(min, max)` | 문자열 길이 | nickname(2~20), password(8~100) |
| `@field:Min` / `@field:Max` | 숫자 범위 | year, month |
| `@ValidDateRange` | 날짜 교차 검증 | EventCreateRequest |

---

## Response DTO 규칙

### 기본 구조
```kotlin
data class EventResponseV2(
    val id: Long,
    val title: String,
    val startAt: LocalDateTime,
    val eventType: EventType,
    val host: HostResponse,
    val viewCount: Int,
    val isBookmarked: Boolean = false
) {
    companion object {
        fun from(event: Event, isBookmarked: Boolean = false) = EventResponseV2(
            id = event.id!!,
            title = event.title,
            // ...
        )
    }
}
```

### 체크리스트
- [ ] `data class`
- [ ] `companion object { fun from(entity): Response }` 팩토리 패턴
- [ ] 엔티티를 직접 반환하지 않을 것
- [ ] 중첩 엔티티도 Response DTO로 변환 (예: `HostResponse.from(event.host)`)

---

## 페이지네이션 규칙

### Offset 기반 (v1, 관리자용)
```kotlin
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val pageInfo: PageInfo   // totalElements, totalPages, page, size, hasNext
)
```

### Cursor 기반 (v2, 클라이언트용)
```kotlin
data class CursorPageResponse<T>(
    val content: List<T> = emptyList(),
    val pageInfo: CursorPageInfo  // hasNext, nextCursor (Base64), pageSize
)
```

### Pagination Param 작성
```kotlin
data class EventCursorPaginationParam(
    @get:Parameter(description = "다음 페이지 커서")
    val cursor: String? = null,

    @get:Parameter(description = "페이지 크기 (1~100)")
    @get:Schema(minimum = "1", maximum = "100", defaultValue = "10")
    val size: Int = 10,

    @get:Parameter(description = "정렬 필드")
    @get:Schema(defaultValue = "CREATED_AT")
    val field: PaginationField = PaginationField.CREATED_AT,
) {
    init {
        require(size in 1..100) { "size는 1 이상 100 이하여야 합니다" }
    }
}
```

### 정렬 옵션 (PaginationField)

| 필드 | 설명 | 기본 방향 |
|------|-----|---------|
| ID | ID 최신순 | DESC |
| CREATED_AT | 등록일 최신순 | DESC |
| START_DATE | 시작일 임박/종료순 | ASC(ACTIVE) / DESC(FINISHED) |
| RECRUITMENT_DEADLINE | 모집 마감 임박순 | ASC(ACTIVE) / DESC(FINISHED) |
| VIEW_COUNT | 조회수 많은순 | DESC |

---

## Multipart 요청 처리

```kotlin
@PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
fun createEvent(
    @Valid @RequestPart("data") eventRequest: EventCreateRequest,  // JSON 파트
    @RequestPart("eventThumbnail", required = false)
    @Parameter(description = "행사 썸네일 이미지")
    eventThumbnail: MultipartFile?,  // 파일 파트
) = eventService.createEvent(eventRequest, eventThumbnail)
```
- JSON 데이터: `@RequestPart("data")` + `@Valid`
- 파일: `@RequestPart("name", required = false)` + `@Parameter`

---

## 에러 응답 포맷

```json
{
    "code": "VALIDATION_FAILED",
    "message": "입력값 검증에 실패했습니다",
    "fieldErrors": [
        {
            "field": "title",
            "rejectedValue": "",
            "message": "행사 제목은 필수입니다"
        }
    ],
    "timestamp": "2025-02-11T14:30:00",
    "path": "/api/v1/events"
}
```

### ErrorCode 매핑

| ErrorCode | HTTP Status | 메시지 | 트리거 |
|-----------|------------|--------|-------|
| INTERNAL_SERVER_ERROR | 500 | 서버 내부 오류 | Exception (+ Discord 알림) |
| INVALID_REQUEST | 400 | 잘못된 요청 | IllegalArgumentException |
| UNAUTHORIZED | 401 | 인증 필요 | AuthenticationException |
| FORBIDDEN | 403 | 접근 권한 없음 | AccessDeniedException |
| NOT_FOUND | 404 | 리소스 없음 | EntityNotFoundException |
| CONFLICT | 409 | 리소스 충돌 | IllegalStateException |
| VALIDATION_FAILED | 400 | 검증 실패 | MethodArgumentNotValidException |
| DATA_INTEGRITY_VIOLATION | 409 | 제약 조건 위반 | DataIntegrityViolationException |

---

## Swagger (OpenAPI) 규칙

### Controller
```kotlin
@Tag(name = "Event", description = "행사 관련 API")   // 클래스에
@Operation(summary = "행사 생성", description = "...")  // 메서드에
@RequireAuth                                           // 인증 필요 시
```

### Request DTO
```kotlin
@field:Schema(description = "행사 제목", example = "2025 AI 컨퍼런스")
```

### Pagination Param
```kotlin
@get:Parameter(description = "페이지 크기 (1~100)")
@get:Schema(minimum = "1", maximum = "100", defaultValue = "10")
```

### 현재 미흡한 부분
- 대부분의 Response DTO에 `@Schema` 어노테이션 누락
- 일부 Request DTO에 `@Schema` 누락 (AdminLoginRequest, UpdateNicknameRequest 등)
- `rules/anti-patterns.md` 참조
