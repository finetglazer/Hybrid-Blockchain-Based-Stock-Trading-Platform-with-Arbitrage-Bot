# 1) Use a Maven image to build the JAR
FROM maven:3.8.8-eclipse-temurin-17 AS builder
WORKDIR /app

# 2) Copy the source code and build the project
COPY . .
RUN mvn clean package -DskipTests

# 3) Use a minimal Java runtime image
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# 4) Copy the built JAR from the builder stage
COPY --from=builder /app/target/user-service-0.0.1-SNAPSHOT.jar app.jar

# 5) Expose the application port (optional)
EXPOSE 8081

# 6) Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
