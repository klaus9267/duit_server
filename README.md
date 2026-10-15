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


<br>

---

<br>

## 🔐 운영 설정 메모

CD 는 `deploy.sh`, `switch-traffic.sh`, `docker-compose.yml`, `monitoring/prometheus.yml`, `monitoring/loki.yml` 을 서버(`/home/vagom/duit-server`)에 복사하고 앱(blue/green)만 다시 띄웁니다. 모니터링 컨테이너와 DB 정리는 아래 절차로 직접 반영합니다.

### 모니터링 설정 반영 (최초 1회)

`/actuator` 는 `health`, `prometheus` 만 노출하며 `/actuator/prometheus` 는 `METRICS_TOKEN` Bearer 토큰이 있어야 응답합니다. 토큰이 비어 있으면 모든 스크랩이 거부됩니다. Loki 는 로그를 1년(8760h) 보관한 뒤 삭제하고, Prometheus·Grafana·Loki·node-exporter·cAdvisor 포트는 서버 내부(127.0.0.1)에만 열립니다.

```bash
cd /home/vagom/duit-server                        # docker-compose.yml 위치 (저장소의 docker/ 디렉터리)
TOKEN=$(openssl rand -hex 32)
echo "METRICS_TOKEN=$TOKEN" >> .env               # 앱 컨테이너(blue/green)에 전달
printf '%s' "$TOKEN" > monitoring/metrics-token   # Prometheus credentials_file (git 미추적)
sudo chown 65534:65534 monitoring/metrics-token && sudo chmod 400 monitoring/metrics-token
# CD 전에 반영한다면 저장소의 docker/monitoring/prometheus.yml, loki.yml 을 monitoring/ 에 먼저 복사
docker compose --profile monitoring up -d --force-recreate prometheus loki grafana node-exporter cadvisor
```

- `monitoring/metrics-token` 파일이 없는 상태로 Prometheus 를 띄우면 Docker 가 같은 경로에 디렉터리를 만들어 스크랩이 실패하므로, 반드시 파일을 먼저 만든 뒤 재기동합니다. 앱은 다음 배포부터 새 토큰을 읽습니다.
- 이후 `prometheus.yml`·`loki.yml` 이 바뀌면 배포 뒤 `docker compose --profile monitoring up -d --force-recreate prometheus loki` 로 다시 읽힙니다.
- Grafana 등은 SSH 터널로 접속합니다: `ssh -p <SSH 포트> -N -L 3000:127.0.0.1:3000 -L 9090:127.0.0.1:9090 <사용자>@<서버>` 후 `http://localhost:3000`.

### 마케팅 수신 동의 정리 (V5 배포 직후 1회)

V5 마이그레이션은 새 컨테이너가 뜰 때 실행되지만 트래픽 전환 전까지는 이전 버전(마케팅 기본값 동의, 동의 시각 미기록)이 요청을 받습니다. 그 사이 가입하거나 마케팅을 켠 회원은 동의 시각 없이 `marketing = TRUE` 로 남으므로, 배포가 끝나 이전 컨테이너가 내려간 뒤 한 번 더 정리합니다. 새 버전은 동의 시각 없는 동의를 만들지 않으므로 나중에 다시 실행해도 안전합니다.

```bash
docker exec -it duit-mysql-prod mysql -u root -p <DB_NAME>
```

```sql
UPDATE users SET marketing = FALSE WHERE marketing = TRUE AND marketing_agreed_at IS NULL;
```
