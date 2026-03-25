# Work Log

> 프로젝트 작업 일지. 코드만 봐서는 알 수 없는 맥락을 기록한다.

---

## 현재 상태 스냅샷

> 매 작업 후 갱신. 새 세션 시작 시 이 섹션만 읽으면 전체 파악 가능.

 **마지막 작업일**: 2026-03-12
 **진행 중인 작업**: 채용공고 큐레이션 — 전체 파이프라인 완료 (수집 → 스케줄러 → 클라이언트 API → E2E 검증)
 **블로커**: 사람인 API 키 미발급 (고용24만 E2E 검증 완료)
 **미수정 CRITICAL**: 1건 (User.providerType `@Enumerated` 누락)
 **미수정 HIGH**: 7건 (CORS, 외부 API 타임아웃/재시도, FCM 에러 핸들링, JWT Refresh Token, Discord fire-and-forget)
 **브랜치**: dev (origin/dev보다 15커밋 앞)
 **신규 의존성**: `jackson-dataformat-xml` (고용24 XML 파싱)
 **신규 env**: `WORK24_KEY` (application.properties에 설정됨), `SARAMIN_ACCESS_KEY` (미발급)

---

## 2026-03-12 (채용공고 큐레이션 — 2~4단계: 스케줄러, 클라이언트 API, 비동기, 테스트, E2E)

**분류**: feature | test

### 작업 내용
- **스케줄러**: `JobSyncScheduler` — `@Scheduled(cron = "0 0 */3 * * *")` 3시간마다 수집, `@Profile("prod")`, `@EventListener(ApplicationReadyEvent)` 앱 시작 시 즉시 실행
- **비동기 수집**: `JobSyncService.syncAll()` → `fetchAllAsync()`로 `CompletableFuture.supplyAsync` 병렬 fetch → 결과 순차 upsert
- **클라이언트 API (커서 페이지네이션)**:
  - `JobPostingController` — `GET /api/v1/job-postings` (목록), `GET /api/v1/job-postings/{id}` (상세)
  - `JobBookmarkController` — `POST /api/v1/job-bookmarks/{id}/toggle` (북마크 토글)
  - 필터: workRegions, employmentTypes, salaryTypes, closeTypes, sourceTypes, searchKeyword, onlyBookmarked
  - 정렬: CREATED_AT (기본), POSTED_AT
  - QueryDSL 기반 동적 쿼리 (`JobPostingRepositoryImpl`)
- **DTO**: JobPostingResponse, JobPostingCursorPaginationParam, JobPostingCursor, JobPostingSortField, JobBookmarkToggleResponse
- **테스트 (29개 신규)**:
  - `JobSyncSchedulerTest` — 스케줄러 호출 검증 (MockK)
  - `JobSyncServiceTest` — 비동기 병렬 fetch 검증 2개 추가
  - `JobPostingControllerIntegrationTest` — 15개 (목록/필터/검색/정렬/상세/페이지네이션)
  - `JobBookmarkControllerIntegrationTest` — 6개 (토글/비활성/미인증/필터/isBookmarked)
  - `TestFixtures` — jobPosting(), jobBookmark() 팩토리 추가
- **SecurityConfig**: `/api/v1/job-postings`, `/api/v1/job-postings/{jobPostingId}` GET 공개

### E2E 검증 결과 (고용24, 2026-03-12)
- `POST /api/v1/job-sync/trigger` → 1,608건 수집 완료
- `GET /api/v1/job-postings?size=3` → 200 OK, 커서 페이지네이션 정상
- `GET /api/v1/job-postings?searchKeyword=간호&size=3` → 200 OK, 제목 검색 정상
- `GET /api/v1/job-postings?workRegions=SEOUL&size=3` → 200 OK, 서울 필터 정상
- `GET /api/v1/job-postings?employmentTypes=FULL_TIME&size=2` → 200 OK
- `GET /api/v1/job-postings?sortField=POSTED_AT&size=2` → 200 OK
- `GET /api/v1/job-postings/1608` → 200 OK, 상세 정상
- `GET /api/v1/job-postings/99999` → 404 NOT_FOUND, 전역 예외처리 정상
- 커서 페이지네이션 2페이지 이동 정상 (ID 연속성 확인)

### 기술적 결정
- **CompletableFuture.supplyAsync**: 기존 코드베이스 패턴(DiscordService의 CompletableFuture.runAsync) 따름. @Async 대신 직접 CompletableFuture 사용
- **@Profile("prod")**: 스케줄러는 prod에서만 실행. 로컬/테스트에서 3시간마다 수집 방지
- **커서 페이지네이션**: 기존 Event 목록 API와 동일한 CursorPageResponse 패턴 사용
- **QueryDSL 동적 쿼리**: 필터 조건을 BooleanBuilder로 동적 조합. 기존 EventRepositoryImpl 패턴 따름

### 영향 범위
- `domain/job/controller/` — JobPostingController, JobBookmarkController (신규)
- `domain/job/dto/` — 5개 DTO 클래스 (신규)
- `domain/job/repository/` — JobPostingRepositoryCustom, JobPostingRepositoryImpl, JobBookmarkRepository (신규)
- `domain/job/service/` — JobPostingService, JobBookmarkService (신규), JobSyncService (비동기 리팩토링)
- `application/scheduler/JobSyncScheduler.kt` (신규)
- `application/config/SecurityConfig.kt` — job-postings GET 공개 추가
- 전체 테스트 통과, 빌드 성공

### 커밋 이력 (dev 브랜치, 15개)
1. feat: 채용공고 entity 생성
2. feat: 채용공고 수집 인터페이스 및 공통 DTO 정의
3. feat: 사람인 API 채용공고 수집기 구현
4. feat: 고용24 API 채용공고 수집기 구현
5. feat: 채용공고 동기화 서비스 구현
6. chore: 채용공고 수집을 위한 설정 추가
7. docs: AGENTS.md 코딩 규칙 업데이트
8. feat: 채용공고 동기화 스케줄러 구현
9. feat: 채용공고 클라이언트 API DTO 구현
10. feat: 채용공고 조회 QueryDSL 및 북마크 Repository 구현
11. feat: 채용공고 조회 및 북마크 서비스 구현
12. feat: 채용공고 REST API 엔드포인트 구현
13. refactor: 채용공고 수집 fetcher 병렬 실행으로 변경
14. test: 채용공고 스케줄러 및 서비스 단위 테스트 추가
15. test: 채용공고 조회 및 북마크 통합 테스트 추가

---

## 2026-03-10 (채용공고 큐레이션 — 1단계: 수집 기능 구현)

**분류**: feature

### 작업 내용
- **Entity**: `JobPosting`, `JobBookmark` 엔티티 + 6개 enum (SourceType, CloseType, EmploymentType, SalaryType, WorkRegion, EducationLevel)
- **Repository**: `JobPostingRepository` (findBySourceTypeAndExternalId, findByIsActiveTrueAndExpiresAtBefore)
- **인터페이스**: `JobFetcher` (sourceType, fetchAll)
- **수집 서비스**: `JobSyncService` (upsert 오케스트레이션 + 만료 비활성화)
- **사람인 Fetcher**: `SaraminApiResponse` + `SaraminCodeMapper` + `SaraminJobFetcher` (RestClient, 페이지네이션, salary 코드→범위 매핑)
- **고용24 Fetcher**: `Work24ApiResponse` (XML) + `Work24CodeMapper` + `Work24JobFetcher` (XmlMapper, 페이지네이션)
- **build.gradle**: `jackson-dataformat-xml` 의존성 추가
- **application.yml**: `WORK24_KEY` 환경변수명 수정 (`WORK24_AUTH_KEY` → `WORK24_KEY`)
- **테스트**: SaraminCodeMapperTest (77개), Work24CodeMapperTest (74개), JobSyncServiceTest (4개)

### E2E 검증 결과 (고용24)
- 1,452건 실제 수집 성공
- workRegion/workDistrict 정상 파싱 (INCHEON→남동구, GYEONGNAM→김해시 등)
- closeType: "채용시까지 26-03-29" → ON_HIRE 정상 처리
- salaryType/salaryMin/salaryMax 정상 매핑

### 기술적 결정
- **고용24 날짜 형식**: API 문서와 달리 `yy-MM-dd` 형식으로 반환됨. `yyyyMMdd`, `yyyy-MM-dd`, `yy-MM-dd` 3종 포맷 순차 파싱으로 대응
- **고용24 closeDt 복합 형태**: "채용시까지  26-03-29" → `contains("채용시까지")` 체크로 ON_HIRE 처리
- **extractDistrict**: 고용24 region이 "시도 구" 2단어 구조 → `dropLast(1)` 제거하고 `drop(1).joinToString` 사용

### 영향 범위
- `domain/job/` — 엔티티, 리포지토리, 서비스 (신규)
- `infrastructure/external/job/` — Fetcher 인터페이스, DTO, 사람인/고용24 구현체 (신규)
- `build.gradle` — XML 의존성 추가
- `application.yml` — 환경변수명 수정
- 전체 테스트 통과, 빌드 성공

### 다음 단계
- 사람인 API 키 발급 후 사람인 E2E 검증
- 2단계: JobSyncScheduler (cron 3시간마다)
- 3단계: 클라이언트 API (목록/필터/검색/북마크/정렬)

---

## 2026-02-27 (Phase 3 Caffeine 케시 교체)

**분류**: performance | refactor

### 작업 내용
- `build.gradle`: `com.github.ben-manes.caffeine:caffeine` 의존성 추가
- `CacheConfig.kt`: `RedisCacheManager` 전면 제거 → `CaffeineCacheManager` 교체 (events-5m 5분, events-3m 3분, maxSize 2000)
- `IntegrationTestSupport.kt`: `@BeforeEach clearCaches()` 추가 — Caffeine이 실제로 동작하면서 테스트 간 케시 오염 발생(에 대한 한 건 FAIL 수정)
- `docs/blog-traffic-optimization-phase2.md`: "Redis CPU 경쟁" 표현 제거 → Grafana 실측 기반 수정 (Redis CPU 2%, Spring Boot CPU 218%)

### 테스트 결과
**퀴 테스트** (quick-test.js: 500→1000→2000→3000 VU):
- 3,000 VU 전체 PASS, P95: 1,500ms, Error: 0.00%
- 이전 Redis @Cacheable: 1,000 VU부터 다운그레이드

**풀 테스트** (local-cache-breakpoint-test.js: 다양한 정렬+50% 인증):
- 500 VU PASS (P95 1,110ms), 1,000 VU부터 FAIL (P95 4,477ms)
- 풀 테스트 병목: VIEW_COUNT 정렬(캐싱 제외) + 인증 요청 50%(로그인 사용자 북마크 조회)

### 기술적 결정
- **Caffeine 선택 이유**: 단일 서버에서 Redis 라운드트립 + JSON 직렬화/역직렬화 오버헤드 완전 제거. `@Cacheable` / `@CacheEvict` 코드 변경 없이 CacheConfig.kt만 교체
- **Redis 의존성 유지**: ViewCount 등 일부 기능이 Redis를 여전히 사용할 수 있슴. 나중 스케일 아웃 시 Redis 글로벌 캐시로 복귀 가능
- **TestCacheConfig 유지**: 테스트에서는 여전히 `SimpleCacheManager` 사용. `clearCaches()`로 테스트 간 오염 방지
- **Grafana 실측 발견**: Redis CPU 2%(실제 빑르지 않음), Spring Boot CPU 218% 현횩 → 블로그 "Redis CPU 경쟁" 설명 수정

### 영향 범위
- `build.gradle` — caffeine 의존성 추가
- `application/config/CacheConfig.kt` — RedisCacheManager → CaffeineCacheManager
- `test/.../IntegrationTestSupport.kt` — clearCaches() 추가
- `docs/blog-traffic-optimization-phase2.md` — Redis CPU 경쟁 설명 수정
- 전체 테스트 337개 통과, 빌드 성공

### 다음 단계 후보
- Caffeine 코드 변경 코밀 (main 브랜치는 아직 미커밋)
- 프로덕션에서 Caffeine 적용 후 세시 Phase 3 성능 측정
---

## 2026-02-26 (Redis 캐싱 리팩토링 — 수동 Cache-Aside → Spring @Cacheable)

**분류**: refactor | performance

### 작업 내용
- 수동 Cache-Aside 패턴(EventCacheService 153줄) 전체 삭제 → Spring `@Cacheable` 기반으로 전환
- `CacheConfig.kt` 재작성: `RedisCacheManager` + `@EnableCaching` + `CacheErrorHandler`(Redis 장애 시 투명 처리)
- `EventQueryService.kt` 신규: `@Cacheable(cacheResolver = "eventCacheResolver")` 메서드로 캐시 조회 위임
- `EventCacheResolver.kt` 신규: 정렬 필드별 캐시 선택 (CREATED_AT/ID → events-5m, START_DATE/RECRUITMENT_DEADLINE → events-3m)
- `EventCacheEvictService.kt` 신규: 행사 CRUD/스케줄러 시 전체 evict (`@CacheEvict(allEntries=true)`)
- `EventCursorPaginationParam.kt`: `cacheKey()` 메서드 추가 (정렬필드+상태필터+타입+사이즈+커서 조합)
- `EventService.kt`: 캐시 로직 전부 제거, `EventQueryService`/`EventCacheEvictService`로 위임
- `EventStatusScheduler.kt`: `incrementVersion()` → `evictAll()` 교체
- `TestCacheConfig.kt` 신규: `@Profile("test")` + `SimpleCacheManager`(ConcurrentMapCache)
- `EventCacheServiceTest.kt` 삭제 (720줄), `EventServiceCacheTest.kt` 재작성 (mock 기반)
- `EventServiceUnitTest.kt`: EventCacheService mock 제거, EventQueryService/EventCacheEvictService mock 추가
- `EventStatusSchedulerUnitTest.kt`: EventCacheEvictService mock으로 교체

### 기술적 결정
- **수동 Cache-Aside 제거 이유**: 매 요청마다 `isCacheable()` 6개 조건 → `buildCacheKey()` (Redis GET for version) → L1(ConcurrentHashMap) 체크 → L2(Redis) 체크 → 분기 처리. 저부하(~1000 VU)에서 이 오버헤드가 DB 직접 쿼리보다 느림 (k6 프로덕션 측정으로 확인)
- **Spring @Cacheable 선택**: AOP 기반으로 서비스 코드와 캐시 로직 완전 분리. 수동 L1+L2+버전 관리 153줄 → @Cacheable 어노테이션 1줄
- **EventQueryService 별도 서비스로 분리**: Spring `@Cacheable`은 AOP 프록시 기반이라 같은 클래스 내 self-invocation 시 캐시 적용 안 됨. EventService.getEvents() → EventQueryService.findEvents() 호출 구조
- **CacheResolver 사용**: 정렬 필드별 TTL이 다름 (안정 정렬 5분, 변동 정렬 3분). `@Cacheable`의 단일 `cacheNames`로는 표현 불가 → `CacheResolver`로 동적 캐시 선택
- **CacheErrorHandler**: Redis 장애 시 예외를 삼키고 DB fallback. 서비스 가용성 우선
- **VIEW_COUNT 캐싱 제외**: 추후 ZSET으로 별도 처리 예정. 현재는 ID/CREATED_AT/START_DATE/RECRUITMENT_DEADLINE만 캐싱
- **TestCacheConfig**: 테스트에서 Redis 연결 불필요. SimpleCacheManager + ConcurrentMapCache로 인메모리 처리

### 프로덕션 성능 측정 (리팩토링 전 수동 Cache-Aside 기준)
k6 `prod-gradual-test.js` (50→5000 VU breakpoint, `--out json`) + `analyze-stages.py` 구간별 분석:

| VU | Phase1 RPS | Redis RPS | Phase1 P95 | Redis P95 | Phase1 | Redis |
|---|---|---|---|---|---|---|
| 500 | 393 | 345 | 280ms | 493ms | PASS | PASS |
| 1,000 | 778 | 686 | 499ms | 524ms | PASS | PASS |
| 1,500 | 917 | 960 | 1,300ms | 801ms | PASS | PASS |
| 2,000 | 959 | 1,114 | 1,820ms | 1,425ms | PASS | PASS |
| 2,500 | 963 | 1,179 | 2,630ms | 1,814ms | FAIL | **PASS** |
| 3,000 | - | 1,306 | - | 2,122ms | - | FAIL |

**핵심 발견**: Breakpoint 2,500→3,000 VU 상승, 피크 RPS +43%. 그러나 저부하(~1000 VU)에서 Redis ON이 오히려 느림 — 수동 캐시 로직 오버헤드가 DB 직접 쿼리보다 비쌈 ("캐싱 세금"). 이 결과가 @Cacheable 리팩토링의 직접적 동기.

### 영향 범위
- `build.gradle` — `spring-boot-starter-cache` 의존성 추가
- `application/config/CacheConfig.kt` — 전면 재작성 (object → @Configuration)
- `application/config/EventCacheResolver.kt` — 신규
- `application/scheduler/EventStatusScheduler.kt` — EventCacheEvictService 교체
- `domain/event/dto/EventCursorPaginationParam.kt` — cacheKey() 추가
- `domain/event/service/EventCacheService.kt` — 삭제
- `domain/event/service/EventCacheEvictService.kt` — 신규
- `domain/event/service/EventQueryService.kt` — 신규
- `domain/event/service/EventService.kt` — 캐시 로직 제거, 위임 구조로 변경
- `test/.../TestCacheConfig.kt` — 신규
- `test/.../EventCacheServiceTest.kt` — 삭제 (720줄)
- `test/.../EventServiceCacheTest.kt` — 재작성
- `test/.../EventServiceUnitTest.kt` — mock 교체
- `test/.../EventStatusSchedulerUnitTest.kt` — mock 교체
- 전체 테스트 통과, 빌드 성공 (net -913줄)

### 실패/보류한 접근
- Subagent(ultrabrain)에 전체 리팩토링 위임 → 타임아웃 발생 + 컴파일 에러 4건 (encode import 누락, cacheKey val→fun 시그니처, 닫는 괄호 누락, CachingConfigurer.cacheManager override 불일치). 수동 수정으로 해결
- `CacheConfig`에 `@Profile("!test")` 적용 → 테스트에서 `@EnableCaching` 자체가 안 붙어서 @Cacheable 테스트 불가. 제거하고 TestCacheConfig에서 SimpleCacheManager로 override

---

## 2026-02-25 (Phase 2 Redis 캐싱 — CacheConfig ObjectMapper 빈 충돌 수정)

**분류**: bugfix | performance

### 작업 내용
- `CacheConfig.kt`: `cacheObjectMapper()` `@Bean` 제거 → `companion object`의 `createCacheObjectMapper()` 팩토리 메서드로 변경
- `EventCacheService.kt`: 생성자 주입(`@Qualifier("cacheObjectMapper")`) 제거 → `CacheConfig.createCacheObjectMapper()`로 직접 생성
- 이전 세션에서 구현된 Redis 캐싱 코드(Phase A~D)로 인해 27개 테스트가 실패했던 문제 해결

### 기술적 결정
- `@Bean`으로 `ObjectMapper`를 노출하면 Spring Boot의 `@ConditionalOnMissingBean(ObjectMapper.class)`에 의해 기본 ObjectMapper 생성이 건너뛰어짐
- `activateDefaultTyping(NON_FINAL)` 옵션이 모든 JSON 응답에 `@class` 타입 정보를 추가하여 테스트 assertion 실패
- 해결: `@Bean` 제거 + `companion object` 팩토리로 분리 → Spring MVC의 기본 ObjectMapper에 영향 없음
- `@Qualifier` 방식도 검토했으나, Spring Boot auto-config는 타입 기반(`@ConditionalOnMissingBean`)이라 이름 지정만으로는 해결 불가

### 영향 범위
- `application/config/CacheConfig.kt` — `@Bean` 제거, `companion object { createCacheObjectMapper() }` 추가
- `domain/event/service/EventCacheService.kt` — 생성자 파라미터 축소, 프로퍼티에서 직접 ObjectMapper 생성
- 전체 테스트: 324 tests, 0 failures (이전: 27 failures)

### 실패/보류한 접근
- `@Bean("cacheObjectMapper")` + `@Qualifier` 조합: Spring Boot의 `@ConditionalOnMissingBean`이 타입(ObjectMapper.class) 기반이라 빈 이름과 무관하게 기본 ObjectMapper 생성 건너뜀 → 실패




## 2026-02-24 (Phase 1 블로그 포스팅 — 요청 #20: blockquote 교체 + 오타 수정 + 스크립트 검증)

**분류**: docs

### 작업 내용
 **#20**: 블로그 6편 마무리 작업
 - `<details><summary>` HTML 토글 → 마크다운 `>` blockquote로 교체 (사용자 요청)
 - 출력 예시를 실제 analyze-stages.py 실행 결과(k6-prod-phase1.json)로 교체
 - analyze-stages.py 타임스탬프 파싱 버그 수정: `rstrip("Z") + "+00:00"` → `endswith("Z")` 분기 (실제 k6 JSON이 `+09:00` KST 포맷)
 - 오타 수정: 94번째 줄 `요청 끜까지` → `요청 끝까지`
 - 오타 검사 수행: 1건 발견 및 수정
 - k6 JSON 파일 2개(2.3GB) .gitignore에 추가

### 기술적 결정
 Tistory가 `<details>` 태그를 제한적으로 지원하므로 마크다운 blockquote(`>`)로 변경
 analyze-stages.py의 타임스탬프: Python 3.14 이하에서는 `fromisoformat`이 timezone offset(`+09:00`)을 안정적으로 처리 못하는 경우가 있어 `endswith("Z")` 분기 추가
 k6 JSON 파일(796MB + 1.5GB): 프로젝트 루트에 임시 배치되어 있으므로 .gitignore에 명시적 추가

### 영향 범위
 - `docs/blog-traffic-optimization-phase1.md`: ~508줄 → ~451줄 (HTML 토글 제거 + blockquote 축약, 오타 수정)
 - `scripts/k6/analyze-stages.py`: 타임스탬프 파싱 1줄 수정
 - `.gitignore`: k6 JSON 2개 항목 추가

---
## 2026-02-24 (Phase 1 블로그 포스팅 — 요청 #19: jq→Python 분석 스크립트 교체)

**분류**: docs

### 작업 내용
 **#19**: 블로그 6편의 --out json 분석 섹션을 jq 한 줄짜리 → Python 스크립트(analyze-stages.py)로 교체
 - `scripts/k6/analyze-stages.py` 신규 생성 (211줄): NDJSON 파싱, 스테이지별 hold 구간 자동 계산, P95/P50/Max/RPS/에러율 테이블 출력
 - 판정 기준(P95 < 2s, 에러율 < 1%) 자동 적용, 진행률 표시(50만줄마다)
 - 블로그에 스크립트 핵심 로직(축약본) + 실행 명령어 + 출력 예시 삽입
 - "835MB를 jq로 한 구간씩 수동으로 파싱하는 건 비현실적이라, 스크립트를 짜서 전체 스테이지를 한 번에 분석했습니다" 서술 추가
 - GitHub 링크 안내 문구 포함

### 기술적 결정
 실제 분석에 사용한 도구가 jq가 아니라 Python이었으므로, 블로그의 신뢰성을 위해 실제 방식으로 교체
 스크립트 전문(211줄)은 블로그에 넣기 과하므로, 핵심 로직만 축약 + GitHub 링크로 안내
 ramp 구간 자동 계산 로직: k6 스크립트의 stages 배열을 파싱하여 각 ramp→hold 경계 timestamp를 산출하는 방식

### 영향 범위
 - `docs/blog-traffic-optimization-phase1.md`: ~465줄 → ~508줄 (jq 섹션 교체 + Python 코드/출력 예시)
 - `scripts/k6/analyze-stages.py`: 신규 파일

---

## 2026-02-24 (Phase 1 블로그 포스팅 — 요청 #18: --out json 구간별 분석 방법 추가)

**분류**: docs

### 작업 내용
 **#18**: k6 `--out json`으로 구간별 데이터를 뽑는 방법을 블로그 3장(측정)에 추가
 - end-of-test summary의 한계 설명 (breakpoint 테스트에서 전체 P95만 나오는 문제)
 - `--out json` 실행 명령어, NDJSON 형식 설명
 - jq로 hold 구간 필터링 → P95/RPS/성공률 구하는 실전 커맨드
 - 835MB JSON 파일 경험담, 스크립트 자동화 권장
 **7편 Redis 캐싱 포스팅**: 작업 목록에서 제거 (사용자 요청)

### 기술적 결정
 삽입 위치: 3장 스크립트 설명(ramp+hold) 직후 → 결과 테이블 직전. "어떻게 이 수치를 뽑았는가"의 자연스러운 흐름
 jq 예시는 실제 테스트에서 사용한 패턴 기반. 시간대 필터링 + sort + awk P95 계산

---

## 2026-02-24 (Phase 1 블로그 포스팅 — 요청 #17: HikariCP pool size 근거 수정)

**분류**: docs | performance

### 작업 내용
 **#17**: HikariCP pool size 근거 전면 수정 — pool=10 vs pool=20 비교 테스트 후 "기본값 10이 적정" 반영
 - 프로덕션 breakpoint test 실행 (pool=10, OSIV off): Phase 1 JSON에서 2000 VU까지 P95 1.85s, 0% 에러 → pool=20(1.82s)과 동일
 - 블로그 5장: "커넥션 풀 증설 (10→20)" → "커넥션 풀 사이징 검증"
 - YAML 설정: `maximum-pool-size: 20` → `10` (기본값 유지)
 - 근거 서술: "약간 여유를 두고 20으로" → "처음에 20 올려봤으나 성능 차이 없음. 공식 (4×2)+1=9 ≈ 10. 풀 크기가 아니라 커넥션 회전율이 핵심, OSIV off가 해결"
 - `scripts/k6/prod-breakpoint-test.js` 프로젝트에 추가

### 기술적 결정
 pool=10 vs pool=20: 동일 스크립트(sleep(1), ramp+hold)로 프로덕션 테스트. Phase 1 JSON 파싱 결과 2000 VU까지 거의 동일 성능. HikariCP 공식 `(4×2)+1=9` ≈ 기본값 10 확인
 OSIV off가 진짜 핵심: 커넥션 회전율이 올라가면 10개로도 충분. 풀을 20으로 늘리는 것은 실질적 효과 없음
 기존 데이터 테이블 유지: pool=20으로 측정한 데이터지만 pool=10과 성능 동일하므로 수치 변경 불필요

### 성능 수치
 **pool=10 (Phase 1, clean run, 동일 breakpoint 스크립트)**:
 | VU | RPS | P95 | 에러율 | 판정 |
 | 500 | 426 | 307ms | 0% | PASS |
 | 1,000 | 802 | 475ms | 0% | PASS |
 | 1,500 | 911 | 1.32s | 0% | PASS |
 | 2,000 | 971 | 1.85s | 0% | PASS |
 **pool=20 (기존 측정)**: 500→280ms, 1000→499ms, 1500→1.30s, 2000→1.82s → 거의 동일
 **pool=10 Full run overall**: P95 3.92s, 에러율 1.85%, RPS 753 (5000 VU까지)

### 영향 범위
 `docs/blog-traffic-optimization-phase1.md` — HikariCP 설정 섹션 전면 수정 (5장)
 `scripts/k6/prod-breakpoint-test.js` — 신규 추가 (실제 엔드포인트, 마스킹 안 됨)
 API/코드 변경 없음

---

## 2026-02-24 (Phase 1 블로그 포스팅 — 요청 #15~#16: OSIV 재작성 + 개인정보 마스킹)

**분류**: docs

### 작업 내용
 **#15**: OSIV 섹션 전면 재작성 — 교과서식 세 개념(영속성 컨텍스트, 트랜잭션, 커넥션) 나열 삭제 → 핵심("트랜잭션 끝나도 커넥션 안 풀린다") + 흐름도 + OSIV on/off 비교 테이블 + "더 자세히 알고 싶다면 이 글 추천" 형태로 변경. Reference에 ykh6242 OSIV 상세 글 추가(총 6개).
 **#16**: 개인정보 마스킹 — 전체 파일(블로그 본문, k6 스크립트 2개, k6 결과 JSON)에서 프로덕션 도메인(`api.dutyit.net` → `api.your-domain.com`)과 API 경로(`/api/v2/events` → `/api/v2/resources`) 일괄 마스킹

### 기술적 결정
 OSIV 서술 방식: 개념 나열은 독자가 이미 아는 내용을 반복하게 됨 → "트랜잭션이 끝나도 커넥션이 안 풀린다"라는 핵심 한 줄 + 영속성 컨텍스트-커넥션 1:1 관계를 흐름도로 보여주고 + 추가 학습은 외부 글 링크로 유도하는 게 경험기 톤에 적합
 개인정보 마스킹 범위: 도메인뿐 아니라 API 경로까지 일반화. 사용자 요청에 따라 `/api/v2/events`를 `/api/v2/resources`로 변경 (서비스 구조를 유추할 수 없도록)
 블로그 본문 코드 블록 내 주석도 "행사 목록 API" → "목록 조회 API"로 일반화

### 영향 범위
 `docs/blog-traffic-optimization-phase1.md` — ~428줄 (OSIV 재작성 + API 경로 마스킹)
 `scripts/k6/local-gradual-test.js` — API 경로 마스킹 (2곳)
 `scripts/k6/prod-gradual-test.js` — 도메인 4곳 + API 경로 2곳 마스킹
 `scripts/k6/results/prod-gradual-summary.json` — 도메인 1곳 마스킹
 API/코드 변경 없음

## 2026-02-24 (Phase 1 블로그 포스팅 — 요청 #10~#14: 최종 정리 및 k6 스크립트 정리)

**분류**: docs

### 작업 내용
 **#10**: 트러블슈팅 절(6장) 전체 삭제, 장 번호 재정렬(7→6), Reference를 한글 자료 5개로 교체
  - HikariCP 풀 사이징, OSIV 성능 최적화, Prometheus+Grafana 모니터링, k6 테스트 유형, k6 스크립트 작성·결과 분석
 **#11**: 도입부 전면 재작성 — "피크타임 100명 동시 접속" 동기 → "서버 한계 궁금 + 한 번도 측정 안 해봄 + 성능 최적화 경험 + 성장 대비"
 **#12**: 쿼리 최적화 이미 완료 상태 반영 — 4장 병목 예측, 5장 프로덕션 포화 분석, 6장 다음 병목, 마치며 총 4곳 수정
  - 코드베이스 확인: EventRepositoryImpl(QueryDSL+JPAQueryFactory, 커서 페이지네이션, fetchJoin), Event 엔티티(10개+ 복합 인덱스), View 인덱스, Bookmark/Alarm @EntityGraph
  - "DB 쿼리 자체" → "DB 접근 횟수"로 병목 재정의 (쿼리 최적화 완료 전제)
 **#13**: 3,500→4,000 VU 이상 현상 재서술 — JIT 추측 제거 → GC pause 타이밍 + 측정 구간 노이즈로 솔직하게 재작성 + "JVM 튜닝과 GC는 별도 포스팅에서 다룰 예정"
 **#14**: k6 스크립트 정리 — 미사용 5개 삭제(baseline-test, breakpoint-test, max-capacity-test, prod-breakpoint-test, prod-extended-test), 미사용 results 3개 삭제

### 기술적 결정
 트러블슈팅 절 삭제 → 마치며에 에피소드 언급은 유지하되, 별도 절로 다루기엔 포스팅 초점(병목 분석+설정 튜닝)과 맞지 않음
 Reference 한글 자료 전환 → 한국어 블로그 독자 대상이므로 한글 자료가 접근성 높음. librarian 에이전트로 URL 유효성 확인 완료
 도입부 동기 변경 → 인사/기술 담당자가 공감할 "성장하는 엔지니어" 포지셔닝 ("피크타임 100명"은 현실과 안 맞음)
 3,500→4,000 VU 현상: JIT warmup은 이미 끝난 시점이라 부적절 → GC pause + 측정 노이즈가 유력하나 GC 로그 미확인이므로 솔직하게 "모른다" + 후속 포스팅 예고
 k6 스크립트 정리: 실제 블로그에 사용한 것만 남김. 나머지는 실험용이었고 재현 가치 없음

### 영향 범위
 `docs/blog-traffic-optimization-phase1.md` — 438줄 → 422줄 (트러블슈팅 절 삭제로 축소)
 `scripts/k6/` — 8개 파일 삭제, 3개 유지 (local-gradual-test.js, prod-gradual-test.js, results/prod-gradual-summary.json)
 API/코드 변경 없음

## 2026-02-24 (Phase 1 블로그 포스팅 — 요청 #9 반영: k6 테스트 종류·트러블슈팅 재구성)

**분류**: docs

### 작업 내용
 3장 "Breakpoint Test" 소제목 → **"부하 테스트의 종류"**로 확장 (Smoke/Load/Stress/Spike/Breakpoint/Soak 테이블 추가)
 실제 사용 스크립트(`local-gradual-test.js`) 한글 주석 달아서 코드 블록 첨부
 k6 한글 튜토리얼 링크 2개 추가 (bitkunst.tistory.com, mangkyu.tistory.com)
 6장 트러블슈팅 재구성: 6.1(직렬화 추측), 6.3(Tomcat 400), 6.4(k6 JSON), 6.5(show-sql) **삭제**
 6.2(HikariCP max-lifetime) 유지 + 새 섹션 2개 추가:
   - "로컬과 프로덕션의 결과 괴리" — 로컬 3,000 VU PASS vs 프로덕션 100 VU FAIL, OSIV + 네트워크 지연 원인 분석
   - "k6 결과 데이터의 구간별 분석" — 639MB JSON 파싱, ramp+hold 경계 계산 트러블슈팅
 4장 소제목: "병목 이동 게임" → **"하나를 풀면 다음이 드러난다"**
 마치며 섹션: 삭제된 트러블슈팅(직렬화, Tomcat 400) 참조 제거, 새 트러블슈팅(로컬/프로덕션 괴리, max-lifetime) 반영

### 기술적 결정
 트러블슈팅은 "실제 테스트 경험에서 나온 것"만 유지 — 추측 기반(직렬화, show-sql) 및 4장에서 이미 설명한 것(Tomcat 불균형) 제거
 k6 한글 튜토리얼: bitkunst(테스트 유형 전반), 망나니개발자(스크립트 작성·결과 분석) — URL 유효성 확인 완료

### 영향 범위
 `docs/blog-traffic-optimization-phase1.md` — 397줄 → 438줄
 API/코드 변경 없음

## 2026-02-23 (Phase 1 블로그 포스팅 — 실서비스 문제 해결기로 톤 전환)

**분류**: docs

### 작업 내용
 `docs/blog-traffic-optimization-phase1.md` 전체 재구성 (409줄 → 397줄)
 제목: "대규모 트래픽 경험기" → **"서비스 개선기 - 6. 대규모 트래픽 (1) - 병목 분석과 설정 튜닝"**
 도입부: 학습 동기("몇 명까지 버틸 수 있지?") → **프로덕션 문제 발견("100명에서 P95 2초 넘었다")**으로 전환
 Phase 0/1 용어 → **"최적화 전"/"설정 튜닝 후"**로 대체 (읽는 사람이 맥락 없이 이해 가능)
 초반 직렬화 내용 제거 (추측 금지 섹션 + JSON 직렬화 섹션) → 트러블슈팅 6.1에만 유지
 잠재적 병목 정리 표 → 발견 서사 브릿지 문단으로 교체
 4장 소제목: "실측 데이터로 병목 식별" → **"Grafana에서 병목이 보였다"**
 5장 소제목: "어떤 방법들이 있는가" → **"왜 이 방법을 먼저 선택했는가"**
 7장 제목: "병목은 어디로 이동했는가" → **"다음 병목은 어디인가"**
 마치며: 학습 회고 톤 → **결과 중심 서술("100명에서 터지던 서버를 2,000명까지 올렸습니다")**
 프로덕션 비교 테이블 헤더: Phase 0/1 → 튜닝 전/후

### 기술적 결정
 시리즈 구조 채택: 기존 "서비스 개선기 1~5" 시리즈에 6편으로 편입
  - 6편: 병목 분석과 설정 튜닝 (현재)
  - 7편: Redis 캐싱 (예정)
 인사/기술 담당자 타겟 톤: "학습했습니다" → "문제를 발견하고 해결한 엔지니어" 포지셔닝
 데이터 테이블 수치는 변경 없음 (실측값 보존)

### 영향 범위
 `docs/blog-traffic-optimization-phase1.md` — 전체 재작성
 API/코드 변경 없음

---

## 2026-02-22 (Phase 1 블로그 포스팅 재구성 — 트래픽 이해 중심)

**분류**: docs

### 작업 내용
- `docs/blog-traffic-optimization-phase1.md` 전체 재구성 (기존 덮어쓰기, 413줄)
- 관통 주제를 "DB 풀 최적화" → **"트래픽의 본질을 이해하는 것"**으로 변경
- 판정 기준을 P95 < 5초 → **P95 < 2초**로 상향
- 2장(요청 한 건의 여정) + 4장(병목 분석)이 포스팅의 핵심 축으로 재배치

### 기술적 결정
- P95 < 2초 기준 → 모바일 사용자 이탈 임계점 근거. 더 엄격하지만 현실적인 기준
- 해결책 비교표 축소 → 포스팅 초점이 "이해"에 있으므로, 설정 변경 자체보다 "왜 이 방향인지"에 무게
- 프로덕션 Phase 0: 100 VU FAIL → Phase 1: 2,000 VU PASS 스토리를 극적으로 배치
- OSIV 설명을 2장(여정)에서 먼저 소개 → 5장(해결)에서 자연스럽게 연결되는 구조

### 구조 변경
| # | 섹션 | 역할 |
|---|------|------|
| 1 | 트래픽이란 무엇인가 | 정의, 핵심 지표, P95<2초 기준, 측정 환경, "추측 금지" 원칙 |
| 2 | 요청 한 건의 여정 ⭐ | Nginx→Tomcat→HikariCP→OSIV→MySQL→직렬화 전체 경로 상세 |
| 3 | 측정 — 현재 서버의 한계 | Phase 0 테이블 (P95<2s: 3,000 VU max), 포화 현상 |
| 4 | 병목은 어디에서, 왜 | Actuator 데이터, HikariCP Pending 178, 은행 창구 비유, 병목 이동 게임 |
| 5 | 해결 — 데이터가 가리키는 방향으로 | OSIV off+풀 증설, Phase 1 결과, 프로덕션 극적 개선 |
| 6 | 트러블슈팅 | 5가지 (직렬화 추측, max-lifetime, Tomcat 불균형, k6 분석, show-sql) |
| 7 | 병목 이동 + 마치며 | Redis 예고, 핵심 배움 3가지 |

### P95 < 2초 재계산 결과 요약
- **Phase 0 로컬**: Max 3,000 VU (4,000 VU에서 P95 2.32s → FAIL)
- **Phase 1 로컬**: Max 3,000 VU (동일하지만 RPS +11%, 고부하 RPS +41%)
- **Phase 0 프로덕션**: 100 VU에서 이미 FAIL (P95 2.05s)
- **Phase 1 프로덕션**: Max 2,000 VU (P95 1.82s) — 극적 개선

### 영향 범위
- `docs/blog-traffic-optimization-phase1.md` — 전체 덮어쓰기
- API/코드 변경 없음

---

## 2026-02-15 (Phase 1: DB 최적화 — OSIV off, batch_fetch_size, HikariCP 튜닝)

**분류**: performance

### 작업 내용
- `application.yml`: `spring.jpa.open-in-view: false` 추가 (OSIV 비활성화)
- `application-local.yml`: `hibernate.default_batch_fetch_size: 100` 추가
- `application-prod.yml`: `hibernate.default_batch_fetch_size: 100` 추가
- `application-local.yml`: HikariCP 설정 추가 (maximum-pool-size: 20, minimum-idle: 10, idle-timeout: 30000, max-lifetime: 300000, connection-timeout: 3000)

### 기술적 결정
- OSIV off → 컨트롤러 레이어에서 DB 커넥션 점유 방지. 서비스 레이어 내에서만 커넥션 사용
- batch_fetch_size 100 → N+1 쿼리 방지 (Lazy Loading 시 IN절으로 일괄 로드)
- HikariCP maximum-pool-size 20 → 공식: `(코어수 × 2) + 1 = (8 × 2) + 1 = 17`, 여유 포함 20
- minimum-idle 10 → 유휴 시에도 최소 커넥션 유지하여 콜드 스타트 방지
- max-lifetime 300000 (5분) → MySQL `wait_timeout`(기본 28800초)보다 충분히 짧게
- connection-timeout 3000 → 커넥션 풀 대기 최대 3초 (기본 30초는 너무 김)

### 성능 수치

| 지표 | Phase 0 (baseline) | Phase 1 | 변화 |
|------|-------------------|---------|------|
| P95 | 6.91s | 4.80s | -30.5% |
| 에러율 | 2.55% | 0.43% | -83% |
| RPS | 1,198 | 1,240 | +3.5% |
| 총 요청 | - | 161,256 | - |
| 평균 응답 | - | 1.91s | - |

측정 조건: k6 breakpoint 테스트, 1000→9000 VU 10 stages, 로컬 Mac, Docker MySQL 8

### Git 상태
- stash@{0}: `cachinh` (hotfix-isApproved 브랜치)
- stash@{1}: Phase 2 Redis 캐시 + @Transactional 메서드 전환
- stash@{2}: Phase 3 Tomcat 400 + G1GC (성능 저하 확인됨)

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

---

## 2026-03-24 (배포 실패 로그 보존 및 Discord 첨부 추가)

**분류**: infra

### 작업 내용
- `scripts/deploy.sh`에 실패 공통 처리 함수를 추가해 배포 실패 시 컨테이너 로그, inspect 결과, compose 상태를 파일로 저장하도록 변경
- 저장된 컨테이너 로그의 최근 200줄을 표준 출력으로 내보내 GitHub Actions 로그에서 빠르게 원인을 확인할 수 있도록 변경
- 저장한 로그 파일을 Discord 웹훅으로 첨부 전송한 뒤 타겟 환경 컨테이너를 내리도록 롤백 순서를 조정
- CD 워크플로에 서버 배포 자산 동기화 단계를 추가해 `deploy.sh`, `switch-traffic.sh`, `docker-compose.yml`을 `/home/vagom/duit-server` 루트로 배포하도록 변경
- CD 워크플로에서 원격 배포 스크립트로 Discord 웹훅과 tail 줄 수를 환경변수로 전달하도록 수정

### 기술적 결정
- 전체 로그는 서버 파일로 보존하고 Actions에는 `tail -n 200`만 출력하도록 분리 → Actions 로그 가독성과 로그 유실 방지를 동시에 만족
- Discord 업로드는 배포 스크립트 안에서 수행 → 실패한 컨테이너가 내려가기 전에 로그 파일을 직접 첨부할 수 있음
- `switch-traffic.sh`는 상대 경로 문자열 실행 대신 `bash "$SCRIPT_DIR/..."` 형태로 호출 → 실행 권한과 작업 디렉터리 의존성을 줄임

### 영향 범위
- 배포 스크립트: `scripts/deploy.sh`, `scripts/switch-traffic.sh`
- CI/CD: `.github/workflows/cd.yml`
- 작업 기록: `rules/work-log.md`

### 발견한 제약 조건
- 서버에서 실행되는 배포 스크립트는 GitHub Actions 러너가 아니라 원격 호스트 파일시스템의 스크립트를 사용하므로, 저장소 수정 후 서버 측 스크립트도 동일하게 반영되어야 실제 배포에 적용됨
- 실제 서버는 `/home/vagom/duit-server` 루트의 `deploy.sh`, `switch-traffic.sh`, `docker-compose.yml`을 사용하고 애플리케이션 저장소는 하위 `duit_server/` 디렉터리에 별도로 존재함

---

## 2026-03-25 (야간 시간대 알람 시각 보정)

**분류**: feature

### 작업 내용
- `EventAlarmScheduler`의 알람 시각 계산 규칙을 변경해 행사/모집 시각이 `20:00~07:00`이면 전날 20시에 발송하도록 보정
- 주간 시간대(`07:01~19:59`)는 기존처럼 1일 전 동일 시각 규칙을 유지
- `EventAlarmSchedulerUnitTest`에 야간 보정 경계값과 모집 종료 알람 케이스를 추가
- `domain-glossary.md`의 알림 시점 설명을 실제 스케줄 규칙에 맞게 갱신

### 기술적 결정
- 조회 윈도우(`now+1day` ~ `now+2day`)는 유지하고 알람 시각 계산만 바꿈 → 현재 04시 일일 스케줄 구조를 유지하면서 야간 시간대만 정책적으로 조정 가능
- 보정 기준은 "기존 알람 시각"이 아니라 실제 이벤트/모집 시각의 시분으로 판단 → `25일 04시~07시`, `25일 20시 이후` 이벤트 모두 `24일 20시` 발송 요구를 직접적으로 만족

### 영향 범위
- 스케줄러: `src/main/kotlin/duit/server/application/scheduler/EventAlarmScheduler.kt`
- 테스트: `src/test/kotlin/duit/server/application/scheduler/EventAlarmSchedulerUnitTest.kt`
- 문서: `rules/domain-glossary.md`, `rules/work-log.md`
