# Dockerfile located at BE/api-gateawy/Dockerfile
# --- IMPORTANT: Run 'docker build -t /api-gateawy:latest .' from the BE/ directory ---

# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the working directory for the build stage
WORKDIR /build

# Copy the PARENT POM first (from BE/pom.xml relative to build context)
# Destination is /build/pom.xml inside the container
COPY pom.xml .

COPY user-service/pom.xml ./user-service/

# --- Change working directory to the module we are building ---
WORKDIR /build/user-service
RUN mvn dependency:go-offline -B

COPY user-service/src ./src

RUN mvn clean package -DskipTests

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/user-service/target/user-service-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
