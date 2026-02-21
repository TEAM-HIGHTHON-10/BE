# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and config first (layer caching for dependencies)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Download dependencies (cached unless build.gradle changes)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source and build
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: Runtime ───────────────────────────────────────
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy only the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Timezone support
ENV TZ=Asia/Seoul

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
