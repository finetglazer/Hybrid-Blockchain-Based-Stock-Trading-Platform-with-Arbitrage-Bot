# 1) Use an official Maven image with OpenJDK 17
FROM maven:3.8.8-eclipse-temurin-17 AS builder
WORKDIR /app

# 2) Copy source code and build the application
COPY . .
RUN mvn clean package -DskipTests

# 3) Use a smaller, official OpenJDK image for runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 4) Copy the built JAR from the builder stage
COPY --from=builder /app/target/API-gateway-0.0.1-SNAPSHOT.jar app.jar

# 5) Expose the application port
EXPOSE 8080

# 6) Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]




## Below is optimized dockerfile for LOCAL & DOCKER environments
#
#FROM eclipse-temurin:17-jdk-alpine
#
## Set working directory
#WORKDIR /app
#
## Copy the pre-built JAR file from your local machine to the container
#COPY target/API-gateway-0.0.1-SNAPSHOT.jar app.jar
#
## Expose the port the app will run on
#EXPOSE 8080
#
## Run the application
#ENTRYPOINT ["java", "-jar", "app.jar"]