# Dockerfile located at BE/API-gateway/Dockerfile
# --- Build context is BE/API-gateway/ ---

# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the top-level working directory for the build stage
WORKDIR /build

# Copy the PARENT POM first (from one directory up relative to context)
# Destination is /build/pom.xml inside the container
COPY ../pom.xml .

# Copy the API-gateway module's POM (from context root) into its subdirectory
COPY pom.xml ./API-gateway/

# Copy the API-gateway module's source code (from context root's src)
COPY src ./API-gateway/src/

# --- Run Maven from the PARENT directory (/build) ---
# Tell Maven to build only the API-gateway project (-pl)
# and also build any reactor dependencies it might have (-am)
# Maven reads /build/pom.xml, finds the module in /build/API-gateway/
RUN mvn clean package -pl API-gateway -am -DskipTests

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR file from the correct path in the builder stage
# Maven built it within the module's target directory relative to /build
COPY --from=builder /build/API-gateway/target/API-gateway-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
