# syntax=docker/dockerfile:1
# ─────────────────────────────────────────────────────────────────────────────
# MenteeConnect Backend — Standard JVM Build (RECOMMENDED for Render free tier)
# ─────────────────────────────────────────────────────────────────────────────
# This builds a regular Spring Boot JAR using Java 21 JVM (NOT GraalVM native).
# Benefits over Dockerfile.native:
#   - Builds in ~1 min vs 4+ min for native
#   - Uses ~512MB RAM vs 6GB for native compilation
#   - No AOT property resolution issues (reads env vars/application.properties normally)
#   - Works perfectly on Render free tier

# Stage 1: Download dependencies (cached separately for faster rebuilds)
FROM eclipse-temurin:21-jdk-alpine AS deps

WORKDIR /build

COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -DskipTests

# Stage 2: Build the JAR
FROM deps AS package

WORKDIR /build

COPY ./src src/

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

# Stage 3: Extract layers for better Docker cache utilization
FROM package AS extract

WORKDIR /build

RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted

# Stage 4: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine AS final

# Non-privileged user for security
ARG UID=10001
RUN adduser \
    -D \
    -g "" \
    -h "/nonexistent" \
    -s "/sbin/nologin" \
    -H \
    -u "${UID}" \
    appuser
USER appuser

# Copy Spring Boot layered JAR content
COPY --from=extract build/target/extracted/dependencies/ ./
COPY --from=extract build/target/extracted/spring-boot-loader/ ./
COPY --from=extract build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract build/target/extracted/application/ ./

EXPOSE 8080

# Render injects $PORT dynamically. Shell form expands it at container start.
# -XX:+UseContainerSupport ensures JVM respects container memory limits.
# SPRING_PROFILES_ACTIVE is set on Render dashboard (typically "prod") to pick up
# application-prod.properties with stricter production settings.
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8080} ${SPRING_PROFILES_ACTIVE:+-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}} org.springframework.boot.loader.launch.JarLauncher"]
