FROM gradle:8.14.3-jdk17 AS builder

# Build arguments for JOOQ code generation
ARG DB_URL
ARG DB_USERNAME
ARG DB_PASSWORD
ARG DB_NAME

WORKDIR /app
COPY . .

# gradle.properties 생성 (JOOQ 코드 생성용)
RUN echo "DB_URL=${DB_URL}" > gradle.properties && \
    echo "DB_USERNAME=${DB_USERNAME}" >> gradle.properties && \
    echo "DB_PASSWORD=${DB_PASSWORD}" >> gradle.properties && \
    echo "DB_NAME=${DB_NAME}" >> gradle.properties

# JOOQ 코드 생성 (DB 연결)
RUN chmod +x gradlew && ./gradlew jooqCodegen

# 애플리케이션 빌드
RUN ./gradlew bootJar

FROM openjdk:17.0.2-jdk

ENV APP_HOME=/apps
WORKDIR $APP_HOME

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

