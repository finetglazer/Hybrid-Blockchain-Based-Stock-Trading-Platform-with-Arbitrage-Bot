# Use Maven to build the project
FROM maven:3.8.8-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml and install dependencies
COPY pom.xml .
RUN mvn dependency:resolve

# Copy the source code and build the JAR
COPY . .
RUN mvn clean package -DskipTests

# Use a lightweight runtime image
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/user-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
