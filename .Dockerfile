# ---------- Stage 1: Build the app ----------
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

# Copy Gradle project
COPY . .

# Build a runnable distribution (uses the same stuff as `./gradlew run`)
RUN gradle installDist --no-daemon

# ---------- Stage 2: Run the app ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built distribution from the builder image
# NOTE: replace 'moodmatch-backend' with your rootProject.name from settings.gradle.kts if different
COPY --from=builder /app/build/install/moodmatch-backend /app

# Ktor listens on PORT (Render will set this env var)
EXPOSE 8080
ENV PORT=8080

# Start the Ktor server
CMD ["./bin/moodmatch-backend"]
