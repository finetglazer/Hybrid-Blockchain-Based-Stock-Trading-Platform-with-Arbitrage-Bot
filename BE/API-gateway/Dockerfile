# 1) Use Maven image to build the JAR
FROM maven:3.8.6-openjdk-17 AS builder
WORKDIR /app

# 2) Copy the source code and build
COPY . .
RUN mvn clean package -DskipTests

# 3) Use a lightweight JDK image to run the application
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# 4) Copy the built JAR from the builder stage
COPY --from=builder /app/target/API-gateway-0.0.1-SNAPSHOT.jar app.jar

# 5) Expose the port
EXPOSE 8080

# 6) Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
