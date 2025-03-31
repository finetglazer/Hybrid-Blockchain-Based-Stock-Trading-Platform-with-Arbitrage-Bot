# Dockerfile located at BE/API-gateway/Dockerfile
# --- Build context is NOW BE/ --- # IMPORTANT CHANGE FOR EXECUTION

# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the top-level working directory for the build stage
WORKDIR /build

# Copy the PARENT POM (from context root BE/)
# Destination is /build/pom.xml inside the container
COPY pom.xml .

# Copy the API-gateway module's POM (from context BE/API-gateway/)
COPY API-gateway/pom.xml ./API-gateway/

# Copy the API-gateway module's source code (from context BE/API-gateway/src)
COPY API-gateway/src ./API-gateway/src/

# --- Run Maven from the PARENT directory (/build) ---
# Explicitly tell Maven to use /build/pom.xml as the main file (-f)
# Target the specific module (-pl) and its dependencies (-am)
# Add -e -X for verbose logging just in case
RUN mvn -f pom.xml clean package -pl API-gateway -am -DskipTests -e -X
# ^^ Ensure 'API-gateway' matches the <artifactId> in BE/API-gateway/pom.xml

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR file from the correct path in the builder stage
COPY --from=builder /build/API-gateway/target/API-gateway-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
