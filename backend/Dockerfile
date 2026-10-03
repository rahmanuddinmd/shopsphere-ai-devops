# syntax=docker/dockerfile:1
# =====================================================================
# ShopSphere backend image - multi-stage build
#   Stage 1 (build):   JDK 21 + Maven wrapper -> builds the JAR
#   Stage 2 (runtime): JRE 21 only, non-root user, health check
# Build:  docker build -t shopsphere-backend:local ./backend
# Run:    docker run --rm -p 8080:8080 --env-file .env shopsphere-backend:local
# Tests run in the CI pipeline (Jenkins), so the image build skips them.
# =====================================================================

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
# 1) Copy only build descriptors first so dependency downloads are cached as a layer
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
# 2) Copy sources and build
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
LABEL org.opencontainers.image.title="shopsphere-backend" \
      org.opencontainers.image.source="https://github.com/rahmanuddinmd/shopsphere-ai-devops" \
      org.opencontainers.image.licenses="MIT"
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/shopsphere-backend.jar app.jar
USER 10001:10001
EXPOSE 8080
# Container-aware JVM: heap sized from the container memory limit
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom" \
    LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD ["wget", "-qO-", "http://127.0.0.1:8080/actuator/health/liveness"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
