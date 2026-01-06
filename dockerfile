FROM gradle:8.14.3-jdk17 AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY . .
RUN ./gradlew bootJar --no-daemon

FROM openjdk:17.0.2-jdk

ENV APP_HOME=/apps
WORKDIR $APP_HOME

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

