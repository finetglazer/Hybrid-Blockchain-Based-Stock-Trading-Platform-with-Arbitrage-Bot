# Dockerfile located at BE/API-gateway/Dockerfile
# --- Build context MUST BE BE/ ---

# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy parent POM and module structure
COPY pom.xml .
COPY API-gateway/pom.xml ./API-gateway/
COPY API-gateway/src ./API-gateway/src/
# Add other modules ONLY if API-gateway actually depends on their source/classes directly during build
# (Usually not needed if dependencies are handled via Maven repo)

# Run the build focusing only on API-gateway and its reactor dependencies
# Ensure 'API-gateway' matches the <artifactId>
RUN mvn -f pom.xml clean package -pl API-gateway -am -DskipTests

# --- Verification Step 1: Check if JAR exists in builder ---
RUN echo "Listing builder target directory:" && \
    ls -l /build/API-gateway/target/ || \
    (echo "ERROR: JAR file not found in builder stage!" && exit 1)

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# --- Explicitly copy the specific JAR ---
# Use the known artifactId and version from your POM.
COPY --from=builder /build/API-gateway/target/API-gateway-0.0.1-SNAPSHOT.jar /app/app.jar

# --- Verification Step 2: Check if JAR exists in final image ---
RUN echo "Listing final app directory:" && \
    ls -l /app/ && \
    echo "Checking specifically for /app/app.jar:" && \
    ls -l /app/app.jar || \
    (echo "ERROR: app.jar not found in final image after copy!" && exit 1)

EXPOSE 8080

# --- Final ENTRYPOINT ---
# Using absolute path for clarity
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
