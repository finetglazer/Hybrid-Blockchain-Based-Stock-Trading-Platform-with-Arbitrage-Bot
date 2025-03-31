# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the working directory for the build stage
WORKDIR /build

# Copy the PARENT POM first (from one directory up relative to context)
COPY ../pom.xml .

# Copy the CHILD module's POM (from context root)
COPY pom.xml .

# Maven will run in /build and find both POMs
# Download dependencies (using current pom.xml which is the child)
RUN mvn dependency:go-offline -B

# Copy the source code (from context root's src)
COPY src ./src

# Build the application module
RUN mvn clean package -DskipTests

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/target/user-service-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
