# Dockerfile located at BE/account-service/Dockerfile
# --- IMPORTANT: Run 'docker build -t account-service:latest .' from the BE/ directory ---

# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the working directory for the build stage
WORKDIR /build

# Copy the PARENT POM first (from BE/pom.xml relative to build context)
# Destination is /build/pom.xml inside the container
COPY pom.xml .

# Copy the CHILD module's POM (from BE/account-service/pom.xml relative to build context)
# Destination is /build/account-service/pom.xml inside the container
COPY account-service/pom.xml ./account-service/

# --- Change working directory to the module we are building ---
WORKDIR /build/account-service

# Download dependencies. Maven will run in /build/account-service
# and use ../pom.xml to find the parent at /build/pom.xml
# Adding echo/ls commands for debugging if needed:
# RUN echo "Listing files after POM copy:" && ls -R /build && \
#     echo "Running dependency:go-offline in $(pwd)" && \
#     mvn dependency:go-offline -B
RUN mvn dependency:go-offline -B

# Copy the source code for the account-service module
# Source is BE/account-service/src relative to build context
# Destination is ./src relative to current WORKDIR (/build/account-service)
COPY account-service/src ./src

# Build the application module inside its directory
# RUN echo "Running package in $(pwd)" && \
#     mvn clean package -DskipTests
RUN mvn clean package -DskipTests

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR file from the correct path in the builder stage
# Path is relative to the root of the builder stage filesystem
COPY --from=builder /build/account-service/target/account-service-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
