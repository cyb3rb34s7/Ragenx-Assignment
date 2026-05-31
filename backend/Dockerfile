# syntax=docker/dockerfile:1

# ---- Stage 1: build the bootable jar (JDK) ----
FROM eclipse-temurin:17.0.13_11-jdk-jammy AS build
WORKDIR /app

# Resolve dependencies first so this layer is cached and only re-runs when the
# build files change — not on every source edit.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

# Build only the bootable jar (tests run via ops/run.sh test / CI, not in the image build).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# ---- Stage 2: minimal runtime (JRE only -> smaller image) ----
FROM eclipse-temurin:17.0.13_11-jre-jammy AS runtime

# curl is used only by the container HEALTHCHECK below.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Run as a non-root system user.
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
USER app

EXPOSE 8412
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD curl -fsS http://localhost:8412/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
