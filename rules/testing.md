# Testing Rules

> 테스트 작성 가이드, 픽스처 패턴, 베이스 클래스 사용법

## 테스트 구조

```
src/test/kotlin/duit/server/
├── support/
│   ├── IntegrationTestSupport.kt    # 통합 테스트 베이스 클래스
│   ├── fixture/
│   │   └── TestFixtures.kt          # 테스트 데이터 팩토리
│   └── util/
│       └── MockMvcResponseUtils.kt  # MockMvc 응답 헬퍼
├── domain/
│   └── {feature}/
│       ├── controller/
│       │   └── {Feature}ControllerIntegrationTest.kt
│       ├── service/
│       │   └── {Feature}ServiceUnitTest.kt
│       └── entity/
│           └── {Feature}UnitTest.kt
├── ServerApplicationTests.kt
└── resources/
    ├── application.yml               # test 프로파일 자동 활성화
    └── application-test.yml           # H2, 스케줄러 비활성화
```

---

## 통합 테스트 (Integration Test)

### 베이스 클래스 상속 (필수)
```kotlin
@DisplayName("Event API v2 통합 테스트")
class EventControllerV2IntegrationTest : IntegrationTestSupport() {
    // IntegrationTestSupport가 제공하는 것:
    // - @SpringBootTest + @AutoConfigureMockMvc + @ActiveProfiles("test") + @Transactional
    // - mockMvc, entityManager, objectMapper, jwtTokenProvider
    // - authHeader(userId): String 헬퍼
}
```

### IntegrationTestSupport 구조
```kotlin
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestSupport {
    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var entityManager: EntityManager
    @Autowired protected lateinit var objectMapper: ObjectMapper
    @Autowired protected lateinit var jwtTokenProvider: JwtTokenProvider

    protected fun authHeader(userId: Long): String =
        "Bearer ${jwtTokenProvider.createAccessToken(userId)}"
}
```

### 테스트 데이터 셋업
```kotlin
private lateinit var host1: Host
private lateinit var user1: User

@BeforeEach
fun setUp() {
    // 1. TestFixtures로 엔티티 생성
    host1 = TestFixtures.host(name = "테크 컨퍼런스")
    user1 = TestFixtures.user(nickname = "테스트유저1")
    
    // 2. EntityManager로 영속화
    entityManager.persist(host1)
    entityManager.persist(user1)
    
    val event = TestFixtures.event(host = host1, status = EventStatus.ACTIVE)
    entityManager.persist(event)
    
    val view = TestFixtures.view(event = event, count = 100)
    entityManager.persist(view)
    
    // 3. flush + clear (영속성 컨텍스트 초기화)
    entityManager.flush()
    entityManager.clear()
}
```

### MockMvc 어설션 패턴
```kotlin
// 목록 조회
mockMvc.perform(
    get("/api/v2/events")
        .param("field", "CREATED_AT")
        .param("size", "5")
)
    .andDo(print())
    .andExpect(status().isOk)
    .andExpect(jsonPath("$.content").isArray)
    .andExpect(jsonPath("$.content.length()").value(5))
    .andExpect(jsonPath("$.pageInfo.hasNext").value(true))
    .andReturn()

// 인증이 필요한 엔드포인트
mockMvc.perform(
    post("/api/v1/bookmarks/{eventId}", eventId)
        .header("Authorization", authHeader(user1.id!!))
)
    .andExpect(status().isOk)

// JSON 본문 전송
mockMvc.perform(
    patch("/api/v1/users/nickname")
        .header("Authorization", authHeader(user1.id!!))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
)
    .andExpect(status().isOk)
```

---

## 단위 테스트 (Unit Test)

### 서비스 단위 테스트 (MockK)
```kotlin
@DisplayName("EventService 단위 테스트")
class EventServiceUnitTest {

    private lateinit var eventRepository: EventRepository
    private lateinit var viewService: ViewService
    private lateinit var eventService: EventService

    @BeforeEach
    fun setUp() {
        eventRepository = mockk()
        viewService = mockk()
        // ... 모든 의존성 mock
        eventService = EventService(eventRepository, viewService, ...)
    }

    @Nested
    @DisplayName("createEvent")
    inner class CreateEventTests {
        @Test
        @DisplayName("hostId 사용 - 기존 Host로 Event 생성")
        fun `hostId 사용시 기존 Host로 Event를 생성한다`() {
            // given
            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(any()) } returns savedEvent
            every { viewService.createView(any()) } returns mockk(relaxed = true)
            
            // when
            val result = eventService.createEvent(request, null, null, true)
            
            // then
            verify(exactly = 1) { hostService.getHost(1L) }
            assertEquals(1L, result.id)
        }
    }
}
```

### 엔티티 단위 테스트 (순수 Kotlin, Spring 없음)
```kotlin
@DisplayName("Event 엔티티 단위 테스트")
class EventUnitTest {

    private lateinit var host: Host
    private val baseTime = LocalDateTime.of(2026, 6, 15, 12, 0)

    @BeforeEach
    fun setUp() {
        host = Host(id = 1L, name = "테스트 주최")
    }

    @Nested
    @DisplayName("updateStatus - 상태 전이 로직")
    inner class UpdateStatusTests {
        @Test
        @DisplayName("행사 종료 후 → FINISHED")
        fun `종료일 이후에는 FINISHED 상태가 된다`() {
            val event = createEvent(
                startAt = baseTime.minusDays(2),
                endAt = baseTime.minusDays(1)
            )
            event.updateStatus(baseTime)
            
            assertEquals(EventStatus.FINISHED, event.status)
            assertEquals(EventStatusGroup.FINISHED, event.statusGroup)
        }
    }
}
```

---

## TestFixtures 사용법

### 팩토리 메서드
```kotlin
object TestFixtures {
    fun host(name: String = "테스트 주최자", thumbnail: String? = null): Host
    fun user(email: String? = "test@example.com", nickname: String = "테스트유저", ...): User
    fun event(title: String = "테스트 행사", host: Host, status: EventStatus = EventStatus.PENDING, ...): Event
    fun bookmark(user: User, event: Event): Bookmark
    fun view(event: Event, count: Int = 0): View
    fun alarm(user: User, event: Event, type: AlarmType = AlarmType.EVENT_START): Alarm
    fun admin(user: User, adminId: String = "testadmin"): Admin
    fun bannedIp(ipAddress: String = "192.168.1.100"): BannedIp
}
```

### 사용 원칙
- 기본값은 가장 일반적인 케이스로 설정
- 테스트에서 중요한 값만 명시적으로 오버라이드
- 새 엔티티 추가 시 TestFixtures에도 팩토리 메서드 추가

---

## MockMvcResponseUtils 사용법

```kotlin
// 응답에서 특정 필드 값 추출
val ids = extractValuesFromResponse(result, "id")
val hostIds = extractValuesFromResponse(result, "host.id")  // 중첩 경로

// 커서 페이지네이션 관련
val cursor = extractCursorFromResponse(result)
val hasNext = extractHasNextFromResponse(result)

// 정렬 순서 검증
assertDescendingOrder(ids, "ID")
assertDateDescendingOrder(dates, "createdAt")
assertDateAscendingOrder(dates, "startAt")
```

---

## 테스트 조직화 규칙

### @Nested 계층 구조
```kotlin
@DisplayName("Event API v2 통합 테스트")
class EventControllerV2IntegrationTest : IntegrationTestSupport() {

    @Nested
    @DisplayName("GET /api/v2/events - 행사 목록 조회")
    inner class GetEventsTests {

        @Nested
        @DisplayName("성공 케이스")
        inner class SuccessTests {

            @Nested
            @DisplayName("정렬 기능")
            inner class SortingTests {
                @Test
                @DisplayName("ID 정렬 테스트")
                fun sortByIdTest() { ... }
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        inner class FailureTests {
            @Test
            @DisplayName("size 범위 초과 시 400 에러")
            fun sizeExceedsMaximumTest() { ... }
        }
    }
}
```

### 네이밍 규칙
- **테스트 클래스**: `{Feature}IntegrationTest`, `{Feature}UnitTest`
- **테스트 메서드**: 한글 백틱 선호 `` fun `행사 목록 조회 성공`() `` (camelCase도 허용)
- **@DisplayName**: 한국어 설명
- **@Nested 클래스**: `{Scenario}Tests` (예: `SuccessTests`, `FailureTests`, `SortingTests`)

---

## 테스트 설정 (application-test.yml)

| 설정 | 값 | 설명 |
|-----|---|------|
| DB | H2 in-memory (`MODE=MySQL`) | 테스트마다 스키마 재생성 |
| DDL | `create-drop` | 테스트 시작 시 생성, 종료 시 삭제 |
| SQL 로그 | `false` | 테스트 로그 깔끔하게 |
| 스케줄러 | `disabled` | 테스트에서 cron 실행 안 함 |
| JWT | 테스트용 시크릿 (1시간 유효) | |
| Discord | 목 URL | 실제 웹훅 전송 안 함 |
| 파일 저장 | `${java.io.tmpdir}` | 임시 디렉토리 |

---

## 지켜야 할 것 / 하지 말 것

### DO
- IntegrationTestSupport 상속 (통합 테스트)
- TestFixtures 사용 (테스트 데이터)
- `entityManager.flush()` + `clear()` 호출 (데이터 셋업 후)
- `@Nested` + `@DisplayName`으로 계층 구조화
- 실패 케이스 테스트 포함 (400, 404, 409)
- 커서 페이지네이션: 중복 없음 검증, hasNext 일관성 검증

### DON'T
- `@SpringBootTest`를 개별 클래스에 직접 선언 (IntegrationTestSupport 사용)
- `@ActiveProfiles("test")` 누락 (베이스 클래스에서 이미 설정됨)
- 수동 JPQL DELETE로 정리 (`@Transactional` 롤백 사용)
- 어설션 없는 테스트 (status 확인만으로 부족 — 실제 데이터 검증 필요)
- VIEW_COUNT 정렬 테스트를 H2에서 실행 (MySQL 네이티브 쿼리 비호환)
