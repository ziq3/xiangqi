# --- Stage 1: Build SvelteKit Frontend ---
FROM node:20-slim AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi
COPY frontend/ ./
ARG PUBLIC_SUPABASE_URL
ARG PUBLIC_SUPABASE_PUBLISHABLE_KEY
ENV PUBLIC_SUPABASE_URL=$PUBLIC_SUPABASE_URL
ENV PUBLIC_SUPABASE_PUBLISHABLE_KEY=$PUBLIC_SUPABASE_PUBLISHABLE_KEY
RUN npm run build


# --- Stage 2: Build Spring Boot Backend ---
FROM eclipse-temurin:24-jdk AS backend-builder
WORKDIR /app
# Copy maven wrapper and pom.xml first
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
# Copy the source code
COPY src/ ./src/
# Copy the built frontend static files to Spring Boot's static resources folder
COPY --from=frontend-builder /app/frontend/build/ ./src/main/resources/static/
# Grant execution rights to the Maven wrapper and build the project jar
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# --- Stage 3: Run Application ---
FROM eclipse-temurin:24-jre AS runner
WORKDIR /app
# Install curl for health checks (optional)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
# Copy the jar file from builder stage
COPY --from=backend-builder /app/target/*.jar app.jar
# Copy the engine folder (which contains the executables and pikafish.nnue)
COPY engine/ ./engine/
# Ensure the Linux engine binary is executable
RUN chmod +x ./engine/pikafish || true

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
