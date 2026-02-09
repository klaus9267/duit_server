# DUIT!

> 인터넷에 흩어져있는 수 많은 간호 대외활동과 행사들 편하게 모아보세요! 

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)](https://www.mysql.com)
[![Gradle](https://img.shields.io/badge/Gradle-8.14-02303A?logo=gradle)](https://gradle.org)

### 🚀 현재 운영 중 | MAU 900+ | DAU 100+

<br>

## 🎯 프로젝트 소개

**DUIT**은 간호 관련 컨퍼런스, 세미나, 워크숍, 공모전 등 다양한 대외활동과 행사 정보를 한 곳에서 확인하고 관리할 수 있는 플랫폼입니다.

<br>

### 💡 왜 만들었나요?

간호학 행사 정보는 여러 채널에 흩어져 있어 놓치기 쉽고, 모집 시작/마감 시각을 놓쳐 참여 기회를 잃는 경우가 많습니다. DUIT은 이러한 문제를 해결하기 위해 탄생했습니다.

**핵심 가치**

- 🗂️ **통합 조회** — 다양한 간호학 행사 정보를 한눈에
- 🔔 **맞춤 알림** — 북마크한 행사의 중요 일정 알림
- 📊 **트렌드 파악** — 조회수 기반 인기 행사 확인
- 📱 **멀티 플랫폼** — iOS, Android, Web 지원

<br>

---

<br>

## ✨ 주요 기능

### 🎪 행사 관리

**검색 및 필터링**
- 행사 제목, 주최자 검색
- 행사 타입별 필터링 (컨퍼런스, 세미나, 워크숍, 공모전, 스터디, 해커톤, 기타)
- 상태별 필터링 (모집 대기, 모집 중, 행사 대기, 진행 중, 종료)
- 북마크한 행사만 보기

**다양한 정렬 옵션**
- 최신 등록순 / 조회수 많은순
- 시작일 임박순/종료순 (상태에 따라 동적 변경)
- 모집 마감일 임박순/종료순 (상태에 따라 동적 변경)

<br>

### 👤 사용자 기능

**소셜 로그인** — Google, Apple 지원

**북마크 관리** — 관심 행사 북마크

**알림 설정** — 푸시 알림, 북마크 알림, 캘린더 알림, 마케팅 알림 ON/OFF

<br>

### 🛡️ 관리자 기능

**행사 승인 시스템**
- 신규 행사 등록 시 관리자 승인 필요
- Discord 웹훅으로 실시간 알림

**IP 차단 시스템**
- 로그인 5회 실패 시 자동 차단
- 관리자 수동 차단/해제 기능

<br>

### 🔔 알림 시스템

**Firebase Cloud Messaging (FCM)** 기반 푸시 알림

**스케줄러 기반 자동 알림**
- 행사 시작 1일 전 오전 9시 알림
- 모집 시작 1일 전 알림
- 모집 종료 1일 전 알림

**Discord 이중 웹훅**
- 신규 행사 등록 알림 (관리자용)
- 서버 에러 실시간 모니터링 (500-level 에러)

<br>

---

<br>

## 🛠 기술 스택

**Backend** &nbsp; 
- `Kotlin 1.9.25` 
- `Spring Boot 3.5.4` 
- `Java 17` 
- `Gradle 8.14.3`

**Database & ORM** &nbsp; 
- `MySQL 8.0` 
- `H2` 
- `Spring Data JPA` 
- `QueryDSL` 
- `Native Query`

**Security** &nbsp; 
- `Spring Security` 
- `JWT` 
- `Firebase Authentication`

**External Services** &nbsp; 
- `Firebase Cloud Messaging (Admin SDK 9.2.0)` 
- `Discord Webhook`

**Infrastructure** &nbsp; 
- `Docker` 
- `Docker Compose` 
- `Prometheus` 
- `Grafana` 
- `Loki` 
- `Promtail` 
- `Node Exporter` 
- `cAdvisor`

**API & Testing** &nbsp; 
- `Springdoc OpenAPI` 
- `JUnit 5` 
- `MockK`

<br>

---

<br>

## 🏗️ 아키텍처

<img width="3585" height="2030" alt="arch" src="https://github.com/user-attachments/assets/732e54a9-a28d-4295-a7db-8a09fbc6b64a" />

<br>

---

<br>

## 📊 ERD

<img width="2362" height="4030" alt="erd" src="https://github.com/user-attachments/assets/7a687cab-290f-4466-bad2-be41cdebc26c" />

