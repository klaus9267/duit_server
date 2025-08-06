# 멀티 스테이지 빌드로 최적화된 이미지 생성
FROM gradle:8.14.3-jdk17 AS builder

WORKDIR /app

# Gradle wrapper와 의존성 파일들을 먼저 복사 (캐시 최적화)
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle settings.gradle ./

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

# 런타임 이미지
FROM openjdk:17.0.1-jdk-slim

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
