# Dockerfile located at BE/API-gateway/Dockerfile
# --- Build context is BE/ ---

# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml .
COPY API-gateway/pom.xml ./API-gateway/
COPY API-gateway/src ./API-gateway/src/

# Run the build (keep -e -X for debugging if needed)
RUN mvn -f pom.xml clean package -pl API-gateway -am -DskipTests -e -X

# --------> ADD THIS LINE <--------
# Verify the contents of the target directory after build
RUN echo "Listing target directory contents:" && ls -l /build/API-gateway/target/
# --------> END OF ADDED LINE <--------

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR file (using wildcard is usually fine if JAR exists)
COPY --from=builder /build/API-gateway/target/API-gateway-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
