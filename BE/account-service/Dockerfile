# ---------------- Stage 1: Build the application ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the working directory for the build stage
WORKDIR /build

COPY ../pom.xml .

RUN mkdir account-service

COPY pom.xml ./account-service/

WORKDIR /build/account-service

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

# ---------------- Stage 2: Create the final runtime image ----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/account-service/target/account-service-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
