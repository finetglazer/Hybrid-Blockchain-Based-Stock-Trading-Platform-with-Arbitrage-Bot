# Dockerfile located at BE/API-gateway/Dockerfile
# --- Build context should be BE/ ---

# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy the parent POM (BE/pom.xml)
COPY pom.xml .

# Copy the specific module's POM and source code (BE/API-gateway/*)
COPY API-gateway/pom.xml ./API-gateway/
COPY API-gateway/src ./API-gateway/src/

# Run Maven targeting the specific module's POM file
# Maven will find parent at /build/pom.xml via relativePath
RUN mvn -f API-gateway/pom.xml clean package -DskipTests

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the JAR from the module's target directory
COPY --from=builder /build/API-gateway/target/API-gateway-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
