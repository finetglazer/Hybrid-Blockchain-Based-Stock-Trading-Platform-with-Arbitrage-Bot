# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the working directory for the build stage
WORKDIR /build

COPY ../pom.xml .

RUN mkdir API-gateway

COPY pom.xml ./API-gateway/

WORKDIR /build/API-gateway

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/API-gateway/target/API-gateway-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
