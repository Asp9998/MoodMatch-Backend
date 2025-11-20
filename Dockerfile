# ---------- Stage 1: Build the app ----------
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

# Copy Gradle project
COPY . .

# Make sure the Gradle wrapper is executable
RUN chmod +x gradlew

# Build a runnable distribution (skip tests for now)
RUN ./gradlew installDist -x test --no-daemon

# ---------- Stage 2: Run the app ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# rootProject.name is "moodmatch-backend", so this path is correct
COPY --from=builder /app/build/install/moodmatch-backend /app

# Ktor listens on PORT (Render will override this env var)
EXPOSE 8080
ENV PORT=8080

# Start the Ktor server
CMD ["./bin/moodmatch-backend"]
