# Dockerfile located at BE/trading-service/Dockerfile
# --- Build context MUST BE the parent directory: BE/ ---

# ---------------- Stage 1: Build the application ----------------
# Use a single builder stage
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the working directory for the build
WORKDIR /build

# Copy the entire project structure (parent POM and all modules)
# The source '.' refers to the build context (BE/)
COPY . .

# Run Maven to build ONLY the trading-service module and its dependencies (-am includes the parent)
# Maven runs from /build (where the parent pom.xml is)
# -pl specifies the target module relative to the parent pom
RUN mvn -pl trading-service -am clean package -DskipTests

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the specific JAR from the TARGET module's target directory
# Path is relative to the WORKDIR in the builder stage (/build)
COPY --from=builder /build/trading-service/target/trading-service-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8084

# --- Final ENTRYPOINT ---
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
