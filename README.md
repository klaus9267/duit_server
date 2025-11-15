# Du It Server

> 다양한 IT 행사를 통합 관리하고 사용자에게 맞춤 알림을 제공하는 백엔드 서버

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)](https://www.mysql.com)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-02303A?logo=gradle)](https://gradle.org)

---

## 🎯 프로젝트 소개

**Du It**은 컨퍼런스, 세미나, 워크숍, 공모전 등 다양한 IT 행사 정보를 한 곳에서 확인하고 관리할 수 있는 플랫폼입니다.

### 핵심 가치

- 🔍 **통합 조회**: 흩어진 IT 행사 정보를 한 곳에서
- 🔔 **맞춤 알림**: 관심 행사의 모집 시작/종료, 행사 시작 알림
- 📱 **멀티 플랫폼**: iOS, Android, Web 지원

---

## ✨ 주요 기능

### 1. 검색 및 필터링
- 행사 제목, 주최자 검색
- 행사 타입별 필터링
- 종료된 행사 포함/제외
- 북마크한 행사만 보기
- 다양한 정렬 옵션 (조회수, 최신순, 날짜 임박순, 모집 마감순)

### 2. 사용자 기능
- 소셜 로그인 (Kakao, Google, Apple)
- 북마크 관리
- 캘린더 연동
- 알림 설정 (푸시, 북마크, 캘린더, 마케팅)

### 3. 알림 시스템
- Firebase Cloud Messaging (FCM) 기반 푸시 알림
- 스케줄러를 통한 자동 알람 생성
  - 행사 시작 1일 전 알림
  - 모집 시작 1일 전 알림
  - 모집 종료 1일 전 알림

---

## 🛠 기술 스택

### Backend
- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.4
- **Build Tool**: Gradle 8.x
- **JVM**: Java 17

### Database
- **Production**: MySQL 8.0
- **Development/Test**: H2 Database
- **ORM**: Spring Data JPA, QueryDSL

### Authentication & Security
- **Social Login**: Firebase Authentication
- **Token**: JWT (JSON Web Token)
- **Security**: Spring Security

### External Services
- **Push Notification**: Firebase Cloud Messaging (FCM)
- **Webhook**: Discord

### Monitoring & Documentation
- **API Docs**: Swagger/OpenAPI 3.0
- **Monitoring**: Prometheus, Grafana, Loki

## 📁 프로젝트 구조

```
src/main/kotlin/duit/server/
├── application/              # 애플리케이션 레이어
│   ├── config/              # 설정 (Security, Firebase, Swagger)
│   ├── security/            # JWT 인증/인가
│   ├── scheduler/           # 스케줄러
│   ├── exception/           # 전역 예외 처리
│   ├── filter/              # 필터
│   └── common/              # 공통 코드
├── domain/                  # 도메인 레이어
│   ├── auth/               # 인증
│   ├── user/               # 사용자
│   ├── event/              # 행사
│   ├── host/               # 주최자
│   ├── bookmark/           # 북마크
│   ├── alarm/              # 알람
│   └── view/               # 조회수
└── infrastructure/          # 인프라 레이어
    └── external/           # 외부 서비스 연동
        ├── firebase/       # FCM
        ├── discord/        # Discord
        └── file/           # 파일
```

### 📄ERD
<img width="1442" height="724" alt="duit drawio" src="https://github.com/user-attachments/assets/3eaf4d6e-65e5-4462-b45e-8464798fa1f3" />
