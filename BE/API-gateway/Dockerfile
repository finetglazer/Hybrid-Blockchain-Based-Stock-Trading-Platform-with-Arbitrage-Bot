# Dockerfile located at BE/API-gateway/Dockerfile
# --- Build context is BE/API-gateway/ ---

# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy the PARENT POM first
COPY ../pom.xml .

# Copy the API-gateway module's POM
COPY pom.xml ./API-gateway/

# Copy the API-gateway module's source code
COPY src ./API-gateway/src/

# --- Change WORKDIR to the module directory ---
WORKDIR /build/API-gateway

# --- Run Maven from the MODULE directory (/build/API-gateway) ---
# It will use ../pom.xml defined in its own pom.xml to find the parent
# Add -e -X for verbose logging
RUN mvn clean package -DskipTests -e -X

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR file. Path is relative to the module's dir now.
COPY --from=builder /build/API-gateway/target/API-gateway-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
