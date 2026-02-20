# Entity Rules

> JPA 엔티티 작성 규칙, 예시, 안티패턴

## 기본 규칙

### 클래스 선언
- **`class` 사용** (절대 `data class` 사용 금지)
- `data class`는 `equals()`, `hashCode()`, `copy()`를 자동 생성하여 Lazy Loading 시 전체 관계가 로드되거나, `copy()`로 의도치 않은 엔티티 복제가 발생할 수 있음
- 예외: `@Embeddable` 값 객체는 `data class` 허용 (예: `AlarmSettings`)

### ID 전략
```kotlin
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
val id: Long? = null
```
- `val` (불변) — ID는 DB가 생성하므로 코드에서 변경 불가
- `Long?` (nullable) — 영속화 전에는 `null`

### 감사 필드 (Audit Fields)
```kotlin
@EntityListeners(AuditingEntityListener::class)
class Event(
    // ...
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
```
- `@EntityListeners(AuditingEntityListener::class)` **필수**
- `createdAt`: `val` + `@CreatedDate` + `updatable = false`
- `updatedAt`: `var` + `@LastModifiedDate`
- **혼용 금지**: `@CreatedDate`와 `@LastModifiedDate`를 잘못된 필드에 적용하지 않을 것
- **혼합 금지**: Spring Data(`@LastModifiedDate`)와 Hibernate(`@UpdateTimestamp`) 동시 사용 금지

### 테이블 네이밍
```kotlin
@Entity
@Table(name = "snake_case_plural")  // events, banned_ips, users
```
- snake_case + 복수형

### Enum 필드
```kotlin
@Enumerated(EnumType.STRING)
@Column(nullable = false)
var status: EventStatus = EventStatus.PENDING
```
- **모든 Enum 필드에 `@Enumerated(EnumType.STRING)` 필수**
- 미지정 시 Hibernate 기본값 `ORDINAL`이 사용되어 enum 순서 변경 시 데이터 깨짐

### 관계 매핑
```kotlin
// 소유 측 (FK를 가진 쪽)
@ManyToOne(fetch = FetchType.LAZY)
val host: Host

// 반대 측
@OneToMany(mappedBy = "host", cascade = [CascadeType.ALL], orphanRemoval = true)
val events: List<Event> = emptyList()
```
- **기본 FetchType.LAZY** — EAGER 절대 금지
- N+1 방지: `@EntityGraph` 또는 `JOIN FETCH` 사용
- 소유 측: `@ManyToOne(fetch = FetchType.LAZY)` — FK를 가진 엔티티
- 반대 측: `mappedBy` + `cascade` + `orphanRemoval` 설정

### 불변/가변 프로퍼티
- `val`: ID, 생성일, FK 관계(변경되지 않는 것)
- `var`: 업데이트 가능한 필드 (title, status, updatedAt 등)
- 컬렉션 관계: `val` + `emptyList()` 또는 `mutableListOf()`

### 도메인 로직
엔티티 내부에 비즈니스 로직 배치:
```kotlin
class Event(...) {
    fun updateStatus(time: LocalDateTime) {
        status = when {
            isFinished(time) -> EventStatus.FINISHED
            isActive(time) -> EventStatus.ACTIVE
            // ...
        }
    }
    
    private fun isFinished(time: LocalDateTime): Boolean =
        time > (endAt ?: startAt)
}
```

### 인덱스
```kotlin
@Table(
    name = "events",
    indexes = [
        Index(name = "idx_status_id", columnList = "status, id DESC"),
    ]
)
```
- 인덱스 이름: `idx_` 접두사
- 자주 필터링/정렬하는 컬럼 조합에 복합 인덱스 추가

---

## 전체 엔티티 목록

| 엔티티 | 테이블 | 감사 필드 | 도메인 로직 | 관계 |
|--------|-------|----------|------------|------|
| Event | events | O (created, updated) | updateStatus(), update() | ManyToOne(Host), OneToOne(View), OneToMany(Bookmark, Alarm) |
| User | users | O (created, updated) | updateNickname(), updateSettings() | OneToMany(Bookmark, Alarm), OneToOne(Admin) |
| Host | hosts | **X (미구현)** | 없음 | OneToMany(Event) |
| View | views | **X (미구현)** | 없음 | OneToOne(Event) |
| Bookmark | bookmarks | O (created, updated) | 없음 | ManyToOne(User, Event) |
| Alarm | alarms | O (created만) | 없음 | ManyToOne(User, Event) |
| Admin | admins | O (created, updated) | 없음 | OneToOne(User) |
| BannedIp | banned_ips | O (created만) | ban(), unban(), recordFailure() | 없음 |

## Embeddable

| 값 객체 | 사용처 | 비고 |
|---------|-------|------|
| AlarmSettings | User.alarmSettings | `data class` 허용, push/bookmark/calendar/marketing 설정 |

## Enum 목록

| Enum | 값 | 사용처 |
|------|---|-------|
| EventStatus | PENDING, RECRUITMENT_WAITING, RECRUITING, EVENT_WAITING, ACTIVE, FINISHED | Event.status |
| EventStatusGroup | PENDING, ACTIVE, FINISHED | Event.statusGroup |
| EventType | CONFERENCE, SEMINAR, WEBINAR, WORKSHOP, CONTEST, CONTINUING_EDUCATION, EDUCATION, VOLUNTEER, TRAINING, ETC | Event.eventType |
| AlarmType | EVENT_START, RECRUITMENT_START, RECRUITMENT_END | Alarm.type |
| ProviderType | KAKAO, GOOGLE, APPLE | User.providerType |
| EventDate | START_AT, RECRUITMENT_START_AT, RECRUITMENT_END_AT | 스케줄러 내부 사용 |

---

## 모범 예시: Event 엔티티

```kotlin
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(
    name = "events",
    indexes = [
        Index(name = "idx_status_id", columnList = "status, id DESC"),
    ]
)
class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    var title: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EventStatus = EventStatus.PENDING,
    
    @ManyToOne(fetch = FetchType.LAZY)
    var host: Host,
    
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookmarks: List<Bookmark> = emptyList(),
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun updateStatus(time: LocalDateTime) { /* 도메인 로직 */ }
}
```

---

## 안티패턴 (절대 하지 말 것)

### 1. Entity에 data class 사용
```kotlin
// BAD
data class Admin(@Id val id: Long? = null, ...)

// GOOD
class Admin(@Id val id: Long? = null, ...)
```

### 2. Enum 필드에 @Enumerated 누락
```kotlin
// BAD — ORDINAL로 저장됨
val providerType: ProviderType? = null

// GOOD
@Enumerated(EnumType.STRING)
val providerType: ProviderType? = null
```

### 3. FetchType.EAGER 사용
```kotlin
// BAD
@ManyToOne(fetch = FetchType.EAGER)
val host: Host

// GOOD
@ManyToOne(fetch = FetchType.LAZY)
val host: Host
```

### 4. 감사 어노테이션 혼용
```kotlin
// BAD — createdAt에 LastModifiedDate
@LastModifiedDate
var createdAt: LocalDateTime = LocalDateTime.now()
@UpdateTimestamp  // Spring Data와 Hibernate 혼용
var updatedAt: LocalDateTime = LocalDateTime.now()

// GOOD
@CreatedDate
@Column(nullable = false, updatable = false)
val createdAt: LocalDateTime = LocalDateTime.now()
@LastModifiedDate
var updatedAt: LocalDateTime = LocalDateTime.now()
```

### 5. 엔티티를 직접 API 응답으로 반환
```kotlin
// BAD
@GetMapping("{id}")
fun getEvent(@PathVariable id: Long): Event = eventService.getEvent(id)

// GOOD
@GetMapping("{id}")
fun getEvent(@PathVariable id: Long): EventResponseV2 = eventService.getEventDetail(id)
```
