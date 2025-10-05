FROM gradle:8.14.3-jdk17 AS builder

WORKDIR /app
COPY . .

# 애플리케이션 빌드
RUN ./gradlew bootJar

FROM openjdk:17.0.2-jdk

ENV APP_HOME=/apps
WORKDIR $APP_HOME

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

