# Domain Glossary

> 간호 행사 관리 플랫폼 DU-IT의 도메인 용어, 비즈니스 규칙, 상태 흐름

## 핵심 도메인 개념

### Event (행사)
간호 관련 컨퍼런스, 세미나, 워크숍 등의 대외활동/행사 정보.

**주요 속성:**
- `title` — 행사 제목
- `startAt` / `endAt` — 행사 시작/종료 일시
- `recruitmentStartAt` / `recruitmentEndAt` — 모집 시작/종료 일시
- `uri` — 행사 상세 URL
- `thumbnail` — 행사 썸네일 이미지
- `eventType` — 행사 유형 (10가지)
- `status` — 세부 상태 (6가지)
- `statusGroup` — 그룹 상태 (3가지)

### Host (주최기관)
행사를 주최하는 기관 (대학교, 병원, 협회 등).
- 고유 이름 제약 (`name` UNIQUE)
- Event와 1:N 관계

### User (사용자)
OAuth 소셜 로그인으로 가입한 사용자.
- Google, Apple, Kakao 로그인 지원
- 고유 닉네임 (중복 시 카운터 추가: `user1`, `user2`)
- 알림 설정 (push, bookmark, calendar, marketing)
- 디바이스 토큰 (FCM 푸시용)

### Bookmark (북마크)
사용자가 관심 행사를 저장한 것.
- User-Event 조합 유니크 제약
- 캘린더 자동 추가 옵션 (`isAddedToCalendar`)

### Alarm (알림)
행사 일정 관련 푸시 알림.
- User-Event-Type 조합 유니크 제약 (중복 방지)
- 읽음/안읽음 상태 (`isRead`)

### View (조회수)
행사별 조회 횟수 추적.
- Event와 1:1 관계
- 동시성 안전: UPDATE 쿼리 직접 사용

### Admin (관리자)
별도 관리자 계정.
- User와 1:1 관계
- 고유 adminId + 비밀번호 (BCrypt)

### BannedIp (차단 IP)
로그인 실패 추적 및 자동 차단.
- 24시간 내 5회 실패 시 자동 차단
- 관리자 수동 차단/해제

---

## 행사 상태 흐름 (Event Status)

### 6단계 상태
```
PENDING ──(관리자 승인)──> RECRUITMENT_WAITING
                              │
                    (recruitmentStartAt)
                              │
                              v
                          RECRUITING
                              │
                    (recruitmentEndAt)
                              │
                              v
                        EVENT_WAITING
                              │
                          (startAt)
                              │
                              v
                           ACTIVE
                              │
                      (endAt 또는 startAt)
                              │
                              v
                          FINISHED
```

### 상태 설명

| 상태 | 한국어 | 설명 | 전이 조건 |
|------|-------|------|---------|
| PENDING | 승인 대기 | 사용자 등록, 관리자 승인 필요 | 관리자 수동 승인 |
| RECRUITMENT_WAITING | 모집 대기 | 승인됨, 모집 기간 전 | recruitmentStartAt 도달 |
| RECRUITING | 모집 중 | 참가 신청 가능 | recruitmentEndAt 도달 |
| EVENT_WAITING | 행사 대기 | 모집 종료, 행사 시작 전 | startAt 도달 |
| ACTIVE | 진행 중 | 행사 진행 중 | endAt (또는 startAt) 도달 |
| FINISHED | 종료 | 행사 완료 | 최종 상태 |

### 상태 그룹 (EventStatusGroup)

| 그룹 | 포함 상태 | 용도 |
|------|---------|------|
| PENDING | PENDING | 미승인 필터링 |
| ACTIVE | RECRUITMENT_WAITING, RECRUITING, EVENT_WAITING, ACTIVE | 활성 행사 조회 (기본값) |
| FINISHED | FINISHED | 종료 행사 조회 |

### 모집 기간 유연성
모집 기간은 3가지 패턴 허용:
1. **시작 + 종료 모두 있음**: `recruitmentStartAt ~ recruitmentEndAt`
2. **종료만 있음**: 오늘부터 `recruitmentEndAt`까지 모집
3. **시작만 있음**: `recruitmentStartAt`부터 행사 시작(`startAt`)까지 모집
4. **둘 다 없음**: 모집 없이 바로 행사 대기

---

## 행사 유형 (EventType)

| 값 | 한국어 | 설명 |
|---|-------|------|
| CONFERENCE | 컨퍼런스/학술대회 | 대규모 학술 행사 |
| SEMINAR | 세미나 | 소규모 발표/토론 |
| WEBINAR | 웨비나 | 온라인 세미나 |
| WORKSHOP | 워크숍 | 실습 중심 교육 |
| CONTEST | 공모전 | 간호 관련 공모전 |
| CONTINUING_EDUCATION | 보수교육 | 간호사 보수교육 |
| EDUCATION | 교육 | 일반 교육 프로그램 |
| VOLUNTEER | 봉사 | 봉사활동 |
| TRAINING | 연수 | 집중 연수 프로그램 |
| ETC | 기타 | 기타 행사 |

---

## 알림 유형 (AlarmType)

| 유형 | 한국어 | 알림 시점 | FCM 메시지 |
|------|-------|---------|-----------|
| EVENT_START | 행사 시작 | 행사 시작 1일 전 09:00 | "{행사명}이(가) 내일 시작됩니다!" |
| RECRUITMENT_START | 모집 시작 | 모집 시작 1일 전 | "{행사명}의 모집이 곧 시작됩니다!" |
| RECRUITMENT_END | 모집 종료 | 모집 종료 1일 전 | "{행사명}의 모집이 곧 마감됩니다!" |

### 알림 생성 조건
알림이 생성되려면:
1. 해당 행사를 **북마크**한 사용자
2. 사용자의 **알림 설정**이 활성화 (push=true, bookmark=true)
3. 사용자에게 **디바이스 토큰** 존재
4. 같은 (user, event, type) 조합의 알림이 아직 없음 (유니크 제약)

---

## 날짜 검증 규칙 (DateRangeValidator)

Event 생성/수정 시 적용되는 8가지 날짜 규칙:

| # | 규칙 | 설명 |
|---|------|------|
| 1 | `endAt >= startAt` | 종료일은 시작일 이후 |
| 2 | `recruitmentEndAt >= recruitmentStartAt` | 모집 종료는 모집 시작 이후 |
| 3 | `recruitmentEndAt <= startAt` | 모집은 행사 시작 전에 종료 |
| 4 | `recruitmentStartAt < startAt` | 모집 시작은 행사 시작 전 |
| 5 | `recruitmentStartAt <= endAt` | 모집 시작은 행사 종료 전 |
| 6 | `recruitmentEndAt <= endAt` | 모집 종료는 행사 종료 전 |
| 7 | 모집 기간 없어도 됨 | 둘 다 null 허용 |
| 8 | 부분 모집 기간 허용 | 시작만, 종료만 가능 |

---

## 승인 워크플로우

```
사용자 행사 등록 ──> PENDING (승인 대기)
                      │
                      ├── Discord 알림 → 관리자에게 신규 행사 알림
                      │
                      └── 관리자 승인 ──> RECRUITMENT_WAITING → 스케줄러가 상태 전이

관리자 행사 등록 ──> RECRUITMENT_WAITING (자동 승인, autoApprove=true)
```

---

## 스케줄러 (매일 04:00 KST)

### EventStatusScheduler
1. **놓친 상태 업데이트 일괄 처리** (catch-up)
   - 서버 재시작/장애로 놓친 전이를 일괄 수정
2. **오늘 전환 예정 이벤트 스케줄링**
   - 정확한 시각에 `TaskScheduler`로 전이 예약

### EventAlarmScheduler
- 내일 날짜에 해당하는 이벤트 조회
- 행사 시작, 모집 시작, 모집 종료 각각 알림 생성
- 정확한 시각에 `TaskScheduler`로 알림 예약

---

## 인증 (ProviderType)

| 프로바이더 | 설명 |
|-----------|------|
| GOOGLE | Google OAuth2 |
| APPLE | Apple Sign In |
| KAKAO | 카카오 로그인 |

### 소셜 로그인 플로우
1. 클라이언트가 Firebase ID Token 전송
2. 서버가 Firebase Admin SDK로 토큰 검증
3. 프로바이더 정보 추출 (firebase.sign_in_provider)
4. 사용자 조회 또는 신규 생성
5. JWT Access Token 발급 + 사용자 정보 반환

---

## ErrorCode 전체 목록

| 코드 | HTTP | 메시지 |
|------|------|--------|
| INTERNAL_SERVER_ERROR | 500 | 서버 내부 오류가 발생했습니다 |
| INVALID_REQUEST | 400 | 잘못된 요청입니다 |
| UNAUTHORIZED | 401 | 인증이 필요합니다 |
| FORBIDDEN | 403 | 접근 권한이 없습니다 |
| NOT_FOUND | 404 | 요청한 리소스를 찾을 수 없습니다 |
| METHOD_NOT_ALLOWED | 405 | 지원하지 않는 HTTP 메서드입니다 |
| CONFLICT | 409 | 리소스 충돌이 발생했습니다 |
| VALIDATION_FAILED | 400 | 입력값 검증에 실패했습니다 |
| INVALID_FIREBASE_TOKEN | 400 | 유효하지 않은 Firebase 토큰입니다 |
| FIREBASE_VERIFICATION_FAILED | 500 | Firebase 토큰 검증에 실패했습니다 |
| FIREBASE_USER_NOT_FOUND | 404 | Firebase 사용자를 찾을 수 없습니다 |
| DATA_ACCESS_ERROR | 500 | 데이터베이스 오류가 발생했습니다 |
| DATA_INTEGRITY_VIOLATION | 409 | 데이터 제약 조건 위반입니다 |

---

## 한국어 도메인 용어 사전

| 한국어 | 영어 | 코드 | 설명 |
|-------|------|------|------|
| 행사 | Event | Event | 간호 관련 대외활동/행사 |
| 모집 | Recruitment | recruitment* | 행사 참가 신청 기간 |
| 북마크 | Bookmark | Bookmark | 관심 행사 저장 |
| 알림 | Alarm/Notification | Alarm | 푸시 알림 |
| 주최기관 | Host | Host | 행사 주최자 |
| 조회수 | View Count | View | 행사 인기도 지표 |
| 승인 | Approval | PENDING→ACTIVE | 관리자 행사 승인 |
| 대기 | Waiting | *_WAITING | 다음 단계 전 대기 상태 |
| 진행 중 | Active/In Progress | ACTIVE | 현재 진행 중인 행사 |
| 종료 | Finished | FINISHED | 완료된 행사 |
| 보수교육 | Continuing Education | CONTINUING_EDUCATION | 간호사 법정 보수교육 |
| 간호 | Nursing | - | 플랫폼 타겟 도메인 |
